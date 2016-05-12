package org.dcs.remote.cxf

import org.osgi.service.remoteserviceadmin.EndpointDescription
import org.osgi.service.remoteserviceadmin.EndpointListener
import org.slf4j.LoggerFactory
import org.dcs.remote.ZookeeperServiceTracker


class CxfEndpointListener(zst: ZookeeperServiceTracker) extends EndpointListener {
  private val logger = LoggerFactory.getLogger(classOf[CxfEndpointListener]);
  
  def endpointAdded(endpointDescription: EndpointDescription, matchedFilter: String) {
    val serviceNames: Array[String] = CxfEndpointUtils.serviceInterfaceNames(endpointDescription);
    for (serviceName <- serviceNames) {
      zst.addEndpoint(serviceName, endpointDescription)
    }
  }

  def endpointRemoved(endpointDescription: EndpointDescription, matchedFilter: String) {
    val serviceNames: Array[String] = CxfEndpointUtils.serviceInterfaceNames(endpointDescription);
    for (serviceName <- serviceNames) {
      zst.removeEndpoint(serviceName, endpointDescription)
    }
  }
}