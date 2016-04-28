package org.dcs.remote.cxf

import org.osgi.service.remoteserviceadmin.EndpointDescription

class CxfServiceEndpoint(endpointDescription: EndpointDescription , serviceProxy: Any ) {

	val serviceProxyImplName: Option[String] = CxfEndpointUtils.serviceProxyImplName(endpointDescription);
	
	var proxy = serviceProxy
}