package org.dcs.remote;

import org.osgi.service.remoteserviceadmin.EndpointDescription;

public class CxfEndpointUtils {
	
	public static String getAddress(EndpointDescription endpoint) {
		return (String)endpoint.getProperties().get("org.apache.cxf.ws.address");
	}

}
