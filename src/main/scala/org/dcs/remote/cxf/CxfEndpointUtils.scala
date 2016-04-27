package org.dcs.remote.cxf

import org.osgi.service.remoteserviceadmin.EndpointDescription

object CxfEndpointUtils {
  private val CxfBasePath: String = "cxf/"
	private val AddressKey: String = "org.apache.cxf.ws.address"
	
	def address(endpointDescription: EndpointDescription): String  = 
	  endpointDescription.getProperties().get("org.apache.cxf.ws.address").asInstanceOf[String]
	
	def endpointId(endpointDescription: EndpointDescription): String = 
		endpointDescription.getProperties().get("endpoint.id").asInstanceOf[String]
	
	
	def serviceInterfaceNames(endpointDescription: EndpointDescription): Array[String] =
		endpointDescription.getProperties().get("objectClass").asInstanceOf[Array[String]]
	
	
	def serviceProxyImplName(endpointDescription: EndpointDescription): Option[String] = {
		val address = endpointId(endpointDescription)
		val indexOfCxfBasePath = address.indexOf(CxfBasePath);
		var name: Option[String] = None 
		if(indexOfCxfBasePath != -1) {
			val startIndex = address.indexOf(CxfBasePath) + CxfBasePath.length();
			name = Some(address.substring(startIndex).replaceAll("/", "."))
		}
		name
	}	
}