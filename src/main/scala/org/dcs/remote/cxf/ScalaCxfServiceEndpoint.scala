package org.dcs.remote.cxf

import org.osgi.service.remoteserviceadmin.EndpointDescription

class ScalaCxfServiceEndpoint(endpointDescription: EndpointDescription , serviceProxy: Any ) {

	val serviceProxyImplName: Option[String] = ScalaCxfEndpointUtils.serviceProxyImplName(endpointDescription);
	
	def serviceProxy_ = serviceProxy
}