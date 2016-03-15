package org.dcs.remote;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CxfEndpointListener implements EndpointListener {
	private static final Logger logger = LoggerFactory.getLogger(CxfEndpointListener.class);

	private final ConcurrentHashMap<String, List<Object>> serviceObjectMap;

	public CxfEndpointListener(ConcurrentHashMap<String, List<Object>> serviceObjectMap) {
		this.serviceObjectMap = serviceObjectMap;
	}

	@Override
	public void endpointAdded(EndpointDescription endpoint, String matchedFilter) {
		String address = CxfEndpointUtils.getAddress(endpoint);
		Object serviceObject = WSDLToJava.toServiceObject(address);
		String[] serviceNames = (String[])endpoint.getProperties().get("objectClass");
		for(String serviceName: serviceNames) {
			List<Object> serviceObjects = serviceObjectMap.get(serviceName);
			if(serviceObjects == null) {
				serviceObjects = new ArrayList<>();
				serviceObjectMap.put(serviceName, serviceObjects);
			}
			serviceObjects.add(serviceObject);
		}
		logger.warn("Added endpoint with cxf address : " + address);

	}

	@Override
	public void endpointRemoved(EndpointDescription endpoint, String matchedFilter) {
		String address = (String)endpoint.getProperties().get("org.apache.cxf.ws.address");
		String[] services = (String[])endpoint.getProperties().get("objectClass");
		for(String service: services) {
			serviceObjectMap.put(service, new ArrayList<>());
		}
		logger.warn("Removed endpoint with cxf address : " + address);
	}

}
