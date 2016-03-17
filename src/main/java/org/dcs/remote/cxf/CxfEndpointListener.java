package org.dcs.remote.cxf;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CxfEndpointListener implements EndpointListener {
	private static final Logger logger = LoggerFactory.getLogger(CxfEndpointListener.class);

	private final ConcurrentHashMap<String, List<CxfServiceEndpoint>> serviceEndpointMap;

	public CxfEndpointListener(ConcurrentHashMap<String, List<CxfServiceEndpoint>> serviceEndpointMap) {
		this.serviceEndpointMap = serviceEndpointMap;
	}

	@Override
	public void endpointAdded(EndpointDescription endpoint, String matchedFilter) {
		try {
			String[] serviceNames = CxfEndpointUtils.getServiceInterfaceNames(endpoint);
			for(String serviceName: serviceNames) {
				List<CxfServiceEndpoint> serviceEndpoints = serviceEndpointMap.get(serviceName);
				if(serviceEndpoints == null) {
					serviceEndpoints = new ArrayList<>();
					serviceEndpointMap.put(serviceName, serviceEndpoints);
				}
				Object serviceProxy = CxfWSDLUtils.createProxy(Class.forName(serviceName), endpoint);
				CxfServiceEndpoint serviceEndpoint = new CxfServiceEndpoint(endpoint, serviceProxy);
				serviceEndpoints.add(serviceEndpoint);
				logger.warn("Added service proxy / impl : " + serviceName + "/ " + serviceEndpoint.getServiceProxyImplName());
			}
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void endpointRemoved(EndpointDescription endpoint, String matchedFilter) {
		String[] serviceNames = CxfEndpointUtils.getServiceInterfaceNames(endpoint);
		for(String serviceName: serviceNames) {
			List<CxfServiceEndpoint> serviceEndpoints = serviceEndpointMap.get(serviceName);
			if(serviceEndpoints != null) {
				CxfServiceEndpoint serviceEndpointToDelete = null;
				for(CxfServiceEndpoint serviceEndpoint: serviceEndpoints) {
					String serviceProxyImplName = CxfEndpointUtils.generateServiceProxyImplName(endpoint);
					if(serviceProxyImplName.equals(serviceEndpoint.getServiceProxyImplName())) {
						serviceEndpointToDelete = serviceEndpoint;
						break;
					}
				}
				if(serviceEndpointToDelete != null) {
					serviceEndpoints.remove(serviceEndpointToDelete);
					logger.warn("Removing service proxy / impl : " + serviceName + "/ " + serviceEndpointToDelete.getServiceProxyImplName());
				}
			}
		}		
	}

}
