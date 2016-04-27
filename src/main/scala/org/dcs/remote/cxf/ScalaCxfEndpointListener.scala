package org.dcs.remote.cxf

import org.osgi.service.remoteserviceadmin.EndpointDescription
import org.osgi.service.remoteserviceadmin.EndpointListener
import org.slf4j.LoggerFactory
import org.dcs.remote.ScalaZookeeperServiceTracker

class ScalaCxfEndpointListener extends EndpointListener {
  private val logger = LoggerFactory.getLogger(classOf[ScalaCxfEndpointListener]);

  def endpointAdded(endpointDescription: EndpointDescription, matchedFilter: String) {
    val serviceNames: Array[String] = ScalaCxfEndpointUtils.serviceInterfaceNames(endpointDescription);
    for (serviceName <- serviceNames) {
      ScalaZookeeperServiceTracker.addEndpoint(serviceName, endpointDescription)
    }
  }

  def endpointRemoved(endpointDescription: EndpointDescription, matchedFilter: String) {
    val serviceNames: Array[String] = ScalaCxfEndpointUtils.serviceInterfaceNames(endpointDescription);
    for (serviceName <- serviceNames) {
      ScalaZookeeperServiceTracker.removeEndpoint(serviceName, endpointDescription)
    }
  }
}