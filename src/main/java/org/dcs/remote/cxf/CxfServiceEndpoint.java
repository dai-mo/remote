package org.dcs.remote.cxf;

import org.osgi.service.remoteserviceadmin.EndpointDescription;

public class CxfServiceEndpoint {
	
	private final EndpointDescription endpointDescription;
	private final Object serviceProxy;
	private final String serviceProxyImplName;

	
	public CxfServiceEndpoint(EndpointDescription endpointDescription, Object serviceProxy) {
		this.endpointDescription = endpointDescription;
		this.serviceProxy = serviceProxy;
		this.serviceProxyImplName = CxfEndpointUtils.generateServiceProxyImplName(endpointDescription);
	}

	public EndpointDescription getEndpointDescription() {
		return endpointDescription;
	}

	public Object getServiceProxy() {
		return serviceProxy;
	}
	
	public String getServiceProxyImplName() {
		return serviceProxyImplName;
	}
	
}
