package org.dcs.remote

import java.util.concurrent.ConcurrentHashMap
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.collection.JavaConversions.mapAsScalaConcurrentMap
import scala.collection.JavaConversions.seqAsJavaList
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.framework.imps.CuratorFrameworkState
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.cxf.dosgi.discovery.zookeeper.subscribe.InterfaceMonitor
import org.apache.cxf.dosgi.discovery.zookeeper.util.Utils
import org.dcs.commons.config.ConfigurationFacade
import org.dcs.remote.cxf.ScalaCxfEndpointListener
import org.dcs.remote.cxf.ScalaCxfEndpointUtils
import org.dcs.remote.cxf.ScalaCxfServiceEndpoint
import org.dcs.remote.cxf.ScalaCxfWSDLUtils
import org.osgi.service.remoteserviceadmin.EndpointDescription
import org.slf4j.LoggerFactory
import org.apache.curator.framework.CuratorFramework
import org.apache.zookeeper.ZooKeeper

object ScalaZookeeperServiceTracker {

  val logger = LoggerFactory.getLogger(classOf[ScalaZookeeperServiceTracker])

  val ServiceScope = "singleton"

  val config = ConfigurationFacade.config

  val retryPolicy = new ExponentialBackoffRetry(1000, 3)

  var curatorClient: CuratorFramework = _
  var zooKeeperClient: ZooKeeper = _

  var serviceEndpointMap = new ConcurrentHashMap[String, List[ScalaCxfServiceEndpoint]]()
  val serviceMonitorMap = new ConcurrentHashMap[String, InterfaceMonitor]()

  start

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
    var endpoints: List[ScalaCxfServiceEndpoint] = serviceEndpointMap.getOrElse(serviceName, Nil)
    if (endpoints == Nil) serviceEndpointMap.put(serviceName, endpoints)

    val serviceProxy = ScalaCxfWSDLUtils.proxy(Class.forName(serviceName), endpointDescription)
    val endpoint = new ScalaCxfServiceEndpoint(endpointDescription, serviceProxy)
    endpoints = endpoint :: endpoints

    logger.warn("Added service proxy / impl : " + serviceName + "/ " + endpoint.serviceProxyImplName)
  }

  def removeEndpoint(serviceName: String, endpointDescription: EndpointDescription) {
    var endpoints: List[ScalaCxfServiceEndpoint] = serviceEndpointMap.getOrElse(serviceName, Nil)
    var endpointToDelete: Option[ScalaCxfServiceEndpoint] = None
    if (endpoints != Nil) {
      for (endpoint <- endpoints) {
        val serviceProxyImplName = ScalaCxfEndpointUtils.serviceProxyImplName(endpointDescription)
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

  def service(serviceClass: Class[_]): Option[Any] = {
    service(serviceClass, None);
  }

  def service(serviceClass: Class[_], serviceImplName: String): Option[Any] = {
    service(serviceClass, Some(serviceImplName))
  }

  private def service(serviceClass: Class[_], serviceImplName: Option[String]): Option[Any] = {
    val endpoint = serviceEndpoint(serviceClass, serviceImplName)
    endpoint match {
      case endpoint if (endpoint == None) => None
      case _                              => Some(endpoint.get.serviceProxy_)
    }
  }

  def serviceEndpoint(serviceClass: Class[_]): Option[ScalaCxfServiceEndpoint] = {
    serviceEndpoint(serviceClass, None)
  }

  def serviceEndpoint(serviceClass: Class[_], serviceImplName: String): Option[ScalaCxfServiceEndpoint] = {
    serviceEndpoint(serviceClass, Some(serviceImplName))
  }

  private def serviceEndpoint(serviceClass: Class[_], serviceImplName: Option[String]): Option[ScalaCxfServiceEndpoint] = {
    val serviceName = serviceClass.getName()

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
          val serviceProxy = ScalaCxfWSDLUtils.proxy(serviceClass, firstEndpointDescription)

          if (serviceProxy != None) endpoints = new ScalaCxfServiceEndpoint(firstEndpointDescription, serviceProxy.get) :: endpoints
        }

        serviceEndpointMap.put(serviceName, endpoints)
        endpoint = serviceEndpoint(endpoints, serviceImplName)

      }
    }
    endpoint
  }

  private def serviceEndpoint(endpoints: List[ScalaCxfServiceEndpoint], serviceImplName: Option[String]): Option[ScalaCxfServiceEndpoint] = serviceImplName match {
    case serviceImplName if (serviceImplName != None && !serviceImplName.isEmpty) => endpoints.find(serviceImplName == _.serviceProxyImplName)
    case _ if (endpoints == Nil) => None
    case _ => Some(endpoints(0))
  }

  private def startMonitor(serviceName: String): InterfaceMonitor = {
    val cxfEndpointListener = new ScalaCxfEndpointListener()
    val monitor = new InterfaceMonitor(zooKeeperClient, serviceName, cxfEndpointListener, ServiceScope)
    serviceMonitorMap.put(serviceName, monitor)
    monitor.start()
    monitor
  }
}

class ScalaZookeeperServiceTracker {

}