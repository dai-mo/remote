package org.dcs.remote

import java.util.concurrent.ConcurrentHashMap

import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.framework.imps.CuratorFrameworkState
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.cxf.dosgi.discovery.zookeeper.subscribe.InterfaceMonitor
import org.apache.cxf.dosgi.discovery.zookeeper.util.Utils
import org.apache.zookeeper.ZooKeeper
import org.dcs.commons.config.{GlobalConfiguration, GlobalConfigurator}
import org.dcs.remote.ZookeeperServiceTracker._
import org.dcs.commons.YamlSerializerImplicits._
import org.dcs.remote.cxf.{CxfEndpointListener, CxfEndpointUtils, CxfServiceEndpoint, CxfWSDLUtils}
import org.osgi.service.remoteserviceadmin.EndpointDescription
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions.{asScalaBuffer, collectionAsScalaIterable, mapAsScalaConcurrentMap}
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
  
}

trait ZookeeperServiceTracker extends ServiceTracker {
  
  def start {
    if (curatorClient == null || curatorClient.getState == CuratorFrameworkState.STOPPED) {
      curatorClient = CuratorFrameworkFactory.newClient(config.zookeeperServers, retryPolicy)
      curatorClient.start()
      zooKeeperClient = curatorClient.getZookeeperClient().getZooKeeper()
    }
  }

  def close {
    for (monitor: InterfaceMonitor <- serviceMonitorMap.values) {
      monitor.close()
    }
    curatorClient.close()
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

  
  def service[T](implicit tag: ClassTag[T]): Option[T] = {
    service[T](None)
  }

  def service[T](serviceImplName: String)(implicit tag: ClassTag[T]): Option[T] = {
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
