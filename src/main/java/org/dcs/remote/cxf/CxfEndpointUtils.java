package org.dcs.remote.cxf;

import org.osgi.service.remoteserviceadmin.EndpointDescription;

public class CxfEndpointUtils {
	
	private final static String CXF_BASE_PATH = "cxf/";
	
	public static String getAddress(EndpointDescription endpoint) {
		return (String)endpoint.getProperties().get("org.apache.cxf.ws.address");
	}
	
	public static String getEndpointId(EndpointDescription endpoint) {
		return (String)endpoint.getProperties().get("endpoint.id");
	}
	
	public static String[] getServiceInterfaceNames(EndpointDescription endpoint) {
		return (String[])endpoint.getProperties().get("objectClass");
	}
	

	public static String generateServiceProxyImplName(EndpointDescription endpoint) {
		String address = CxfEndpointUtils.getEndpointId(endpoint);
		int indexOfCxfBasePath = address.indexOf(CXF_BASE_PATH);
		if(indexOfCxfBasePath != -1) {
			int startIndex = address.indexOf(CXF_BASE_PATH) + CXF_BASE_PATH.length();
			return address.substring(startIndex).replaceAll("/", ".");
		}
		throw new IllegalStateException("Target endpoint is not a cxf endpoint");
	}	

}
