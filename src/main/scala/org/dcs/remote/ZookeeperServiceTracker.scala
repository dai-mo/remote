package org.dcs.remote

import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.{List => JavaList}

import org.apache.curator.framework.imps.CuratorFrameworkState
import org.apache.curator.framework.recipes.cache.PathChildrenCache.StartMode
import org.apache.curator.framework.recipes.cache.{ChildData, PathChildrenCache}
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.cxf.dosgi.discovery.zookeeper.subscribe.InterfaceMonitor
import org.apache.cxf.dosgi.discovery.zookeeper.util.Utils
import org.apache.cxf.dosgi.endpointdesc.{EndpointDescriptionParser, PropertiesMapper}
import org.apache.ws.commons.schema.XmlSchemaException
import org.apache.zookeeper.ZooKeeper
import org.dcs.api.service.{ProcessorServiceDefinition, RemoteProcessorService, StatefulRemoteProcessorService}
import org.dcs.commons.config.{GlobalConfiguration, GlobalConfigurator}
import org.dcs.commons.serde.YamlSerializerImplicits._
import org.dcs.remote.ZookeeperServiceTracker._
import org.dcs.remote.cxf.{CxfEndpointListener, CxfEndpointUtils, CxfServiceEndpoint, CxfWSDLUtils}
import org.osgi.service.remoteserviceadmin.EndpointDescription
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions.{asScalaBuffer, collectionAsScalaIterable, mapAsScalaConcurrentMap}
import scala.collection.JavaConverters._
import scala.reflect.ClassTag

object ZookeeperServiceTracker {

  val logger = LoggerFactory.getLogger(classOf[ZookeeperServiceTracker])

  val ServiceScope = "singleton"

  val config = GlobalConfigurator.config.toObject[GlobalConfiguration]

  val retryPolicy = new ExponentialBackoffRetry(1000, 3)

  var curatorClient: CuratorFramework = _
  var zooKeeperClient: ZooKeeper = _

  var serviceEndpointMap = new ConcurrentHashMap[String, List[CxfServiceEndpoint]]()
  val serviceMonitorMap = new ConcurrentHashMap[String, InterfaceMonitor]()

  val CxfPathPrefix = "/osgi/service_registry"
  val StatelessRemoteServicePath: String = CxfPathPrefix + "/org/dcs/api/service/RemoteProcessorService"
  val StatefulRemoteServicePath: String = CxfPathPrefix + "/org/dcs/api/service/StatefulRemoteProcessorService"

  var statelessProcessorServicesCache: PathChildrenCache = _
  var statefulProcessorServicesCache: PathChildrenCache = _


}

trait ZookeeperServiceTracker extends ServiceTracker {

  val edParser = new EndpointDescriptionParser

  def start: Unit = {
    if (curatorClient == null || curatorClient.getState == CuratorFrameworkState.STOPPED) {
      curatorClient = CuratorFrameworkFactory.newClient(config.zookeeperServers, retryPolicy)
      curatorClient.start()
      zooKeeperClient = curatorClient.getZookeeperClient.getZooKeeper

    }
  }

  def init: Unit = {
    // FIXME(AL-195): Following is a hack to start sync monitors
    // on the zookeeper service tracker, o/w trying to access
    // a remote service fails on the first try

    try {
      service[RemoteProcessorService]
      service[StatefulRemoteProcessorService]
    } catch {
      // FIXME(AL-196): No idea why the exception,
      // org.apache.ws.commons.schema.XmlSchemaException: Schema name conflict in collection
      //   at org.apache.ws.commons.schema.XmlSchema.<init>(XmlSchema.java:126)
      //   at org.apache.ws.commons.schema.XmlSchema.<init>(XmlSchema.java:140)
      //   ...
      // is thrown
      case xse: XmlSchemaException => // do nothing
    }
  }

  def loadServiceCaches(): Unit = {
    if (curatorClient != null) {
      if(statelessProcessorServicesCache == null) {
        statelessProcessorServicesCache = new PathChildrenCache(curatorClient, StatelessRemoteServicePath, true)
        statelessProcessorServicesCache.start(StartMode.BUILD_INITIAL_CACHE)
      }
      if(statefulProcessorServicesCache == null) {
        statefulProcessorServicesCache = new PathChildrenCache(curatorClient, StatefulRemoteServicePath, true)
        statefulProcessorServicesCache.start(StartMode.BUILD_INITIAL_CACHE)
      }
    }
  }

  def close {
    for (monitor: InterfaceMonitor <- serviceMonitorMap.values) {
      monitor.close()
    }

    if(statelessProcessorServicesCache != null) statelessProcessorServicesCache.close()
    if(statefulProcessorServicesCache != null) statefulProcessorServicesCache.close()
    if(curatorClient != null) curatorClient.close()
  }

  def serviceProperties(serviceNodes: List[ChildData]): List[Map[String, AnyRef]] = {
    val pm = new PropertiesMapper
    serviceNodes
      .map(sn => pm.toProps(edParser.getEndpointDescriptions(new ByteArrayInputStream(sn.getData)).head.getProperty).asScala.toMap)
  }

  def filterServiceByProperty(serviceProperties: List[Map[String, AnyRef]], property: String, regex: String): List[ProcessorServiceDefinition] = {
    serviceProperties
      .filter(props => CxfEndpointUtils.matchesPropertyValue(props, property, regex))
      .map(props => CxfEndpointUtils.toProcessorServiceDefinition(props))
  }

  override def filterServiceByProperty(property: String, regex: String): List[ProcessorServiceDefinition] = {
    filterServiceByProperty(serviceProperties(statelessProcessorServicesCache.getCurrentData.asScala.toList), property, regex) ++
      filterServiceByProperty(serviceProperties(statefulProcessorServicesCache.getCurrentData.asScala.toList), property, regex)
  }

  override def services(): List[ProcessorServiceDefinition] = {
    serviceProperties(statelessProcessorServicesCache.getCurrentData.asScala.toList).map(props => CxfEndpointUtils.toProcessorServiceDefinition(props)) ++
      serviceProperties(statefulProcessorServicesCache.getCurrentData.asScala.toList).map(props => CxfEndpointUtils.toProcessorServiceDefinition(props))
  }

  def addEndpoint(serviceName: String, endpointDescription: EndpointDescription) {
    var endpoints: List[CxfServiceEndpoint] = serviceEndpointMap.getOrElse(serviceName, Nil)
    if (endpoints == Nil) serviceEndpointMap.put(serviceName, endpoints)

    val serviceProxy = CxfWSDLUtils.proxy(Class.forName(serviceName), endpointDescription)
    val endpoint = new CxfServiceEndpoint(endpointDescription, serviceProxy)
    endpoints = endpoint :: endpoints

    logger.warn("Added service proxy / impl : " + serviceName + "/ " + endpoint.serviceProxyImplName)
  }

  def removeEndpoint(serviceName: String, endpointDescription: EndpointDescription) {
    var endpoints: List[CxfServiceEndpoint] = serviceEndpointMap.getOrElse(serviceName, Nil)
    var endpointToDelete: Option[CxfServiceEndpoint] = None
    if (endpoints != Nil) {
      for (endpoint <- endpoints) {
        val serviceProxyImplName = CxfEndpointUtils.serviceProxyImplName(endpointDescription)
        if (serviceProxyImplName != None && serviceProxyImplName.equals(endpoint.serviceProxyImplName)) {
          endpointToDelete = Some(endpoint);

        }
      }
      if (endpointToDelete != None) {
        endpoints = endpoints.filter(_ != endpointToDelete.get)
        serviceEndpointMap.put(serviceName, endpoints)
        logger.warn("Removing service proxy / impl : " + serviceName + "/ " + endpointToDelete.get.serviceProxyImplName)
      } else {
        serviceEndpointMap.put(serviceName, Nil)
        logger.warn("Removing service proxy / impl : " + serviceName + "/ None ")
      }
    }
  }


  override def service[T](implicit tag: ClassTag[T]): Option[T] = {
    service[T](None)
  }

  override def service[T](serviceImplName: String)(implicit tag: ClassTag[T]): Option[T] = {
    service[T](Some(serviceImplName))
  }

  private def service[T](serviceImplName: Option[String])(implicit tag: ClassTag[T]): Option[T] = {
    val endpoint = serviceEndpoint[T](serviceImplName)
    endpoint match {
      case endpoint if (endpoint == None) => None
      case _                              => Some(endpoint.get.proxy.asInstanceOf[T])
    }
  }

  def serviceEndpoint[T]()(implicit tag: ClassTag[T]): Option[CxfServiceEndpoint] = {
    serviceEndpoint[T](None)
  }

  def serviceEndpoint[T](serviceImplName: String)(implicit tag: ClassTag[T]): Option[CxfServiceEndpoint] = {
    serviceEndpoint[T](Some(serviceImplName))
  }

  private def serviceEndpoint[T](serviceImplName: Option[String])(implicit tag: ClassTag[T]): Option[CxfServiceEndpoint] = {
    val serviceName = tag.runtimeClass.getName

    // First check if the service is currently available ...
    var endpoints = serviceEndpointMap.getOrElse(serviceName, Nil)

    var endpoint = serviceEndpoint(endpoints, serviceImplName)

    if (endpoint == None) {

      // .. if not, check if there exists a monitor tracking it
      var interfaceMonitor = serviceMonitorMap.get(serviceName)
      if (interfaceMonitor == null) {
        // .. if not, start and register a monitor for the service name
        interfaceMonitor = startMonitor(serviceName)
      }

      if (interfaceMonitor != null) {
        // .. retrieve all available service objects for the given service name
        val zooKeeperServicePath = Utils.getZooKeeperPath(serviceName)
        val serviceNodes = curatorClient.getChildren().forPath(zooKeeperServicePath)
        endpoints = Nil

        for (serviceNode <- serviceNodes) {
          val data: Array[Byte] = curatorClient.getData().forPath(zooKeeperServicePath + "/" + serviceNode)
          val firstEndpointDescription = interfaceMonitor.getFirstEnpointDescription(data)
          val serviceProxy = CxfWSDLUtils.proxy(tag.runtimeClass, firstEndpointDescription)

          if (serviceProxy != None) endpoints = new CxfServiceEndpoint(firstEndpointDescription, serviceProxy.get.asInstanceOf[T]) :: endpoints
        }

        serviceEndpointMap.put(serviceName, endpoints)
        endpoint = serviceEndpoint(endpoints, serviceImplName)

      }
    }
    endpoint
  }

  private def serviceEndpoint(endpoints: List[CxfServiceEndpoint], serviceImplName: Option[String]): Option[CxfServiceEndpoint] = serviceImplName match {
    case serviceImplName if (serviceImplName != None && !serviceImplName.isEmpty) => endpoints.find(serviceImplName == _.serviceProxyImplName)
    case _ if (endpoints == Nil) => None
    case _ => Some(endpoints(0))
  }

  private def startMonitor(serviceName: String): InterfaceMonitor = {
    val cxfEndpointListener = new CxfEndpointListener(this)
    val monitor = new InterfaceMonitor(zooKeeperClient, serviceName, cxfEndpointListener, ServiceScope)
    serviceMonitorMap.put(serviceName, monitor)
    monitor.start()
    monitor
  }
}
