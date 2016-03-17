package org.dcs.remote;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.cxf.dosgi.discovery.zookeeper.subscribe.InterfaceMonitor;
import org.apache.cxf.dosgi.discovery.zookeeper.util.Utils;
import org.apache.zookeeper.ZooKeeper;
import org.dcs.remote.config.ConfigurationFacade;
import org.dcs.remote.cxf.CxfEndpointListener;
import org.dcs.remote.cxf.CxfServiceEndpoint;
import org.dcs.remote.cxf.CxfWSDLUtils;
import org.osgi.service.remoteserviceadmin.EndpointDescription;

public class ZooKeeperServiceTracker {

	private ZooKeeper zooKeeperClient;
	private CuratorFramework curatorClient;

	private ConcurrentHashMap<String, List<CxfServiceEndpoint>> serviceEndpointMap;
	private ConcurrentHashMap<String, InterfaceMonitor> serviceMonitorMap;

	private static final String SERVICE_SCOPE = "singleton";

	public ZooKeeperServiceTracker() throws Exception {
		RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
		
		String serverList = ConfigurationFacade.getCurrentConfiguration().getZookeeperServers();
		curatorClient = CuratorFrameworkFactory.newClient(serverList, retryPolicy);
		curatorClient.start();
		zooKeeperClient = curatorClient.getZookeeperClient().getZooKeeper();
		serviceEndpointMap = new ConcurrentHashMap<>();
		serviceMonitorMap = new ConcurrentHashMap<>();

	}
	
	public Object getService(Class<?> serviceClass) throws Exception {
		return getService(serviceClass, null);
	}

	public Object getService(Class<?> serviceClass, String serviceImplName) throws Exception {
		CxfServiceEndpoint serviceEndpoint = getServiceEndpoint(serviceClass, serviceImplName);
		if(serviceEndpoint != null) {
			return serviceEndpoint.getServiceProxy();
		}
		return null;
	}
	
	public CxfServiceEndpoint getServiceEndpoint(Class<?> serviceClass) throws Exception {
		return getServiceEndpoint(serviceClass, null);
	}

	public synchronized CxfServiceEndpoint getServiceEndpoint(Class<?> serviceClass, String serviceImplName) throws Exception {
		String serviceName = serviceClass.getName();

		// First check if the service is currently available ... 
		List<CxfServiceEndpoint> serviceEndpoints = serviceEndpointMap.get(serviceName);

		getServiceEndpointForImpl(serviceEndpoints, serviceImplName);

		// .. if not, check if there exists a monitor tracking it
		InterfaceMonitor interfaceMonitor = serviceMonitorMap.get(serviceName);
		if(interfaceMonitor == null) {
			// .. if not, start and register a monitor for the service name 
			interfaceMonitor = startMonitor(serviceName);			
		}

		if(interfaceMonitor != null) {
			// .. retrieve all available service objects for the given service name
			String zooKeeperServicePath = Utils.getZooKeeperPath(serviceName);
			List<String> serviceNodes = curatorClient.getChildren().forPath(zooKeeperServicePath);	
			serviceEndpoints = new ArrayList<>();
			for(String serviceNode : serviceNodes) {				
				byte[] data = curatorClient.getData().forPath(zooKeeperServicePath + "/" + serviceNode);				
				EndpointDescription firstEnpointDescription = interfaceMonitor.getFirstEnpointDescription(data);
				Object serviceProxy =  CxfWSDLUtils.createProxy(serviceClass, firstEnpointDescription);	
				serviceEndpoints.add(new CxfServiceEndpoint(firstEnpointDescription, serviceProxy));								
			}
			serviceEndpointMap.put(serviceName, serviceEndpoints);
			return getServiceEndpointForImpl(serviceEndpoints, serviceImplName);
		}		
		return null;
	}

	private CxfServiceEndpoint getServiceEndpointForImpl(List<CxfServiceEndpoint> serviceEndpoints, String serviceImplName) {		
		if(serviceEndpoints != null && !serviceEndpoints.isEmpty()) {
			if(serviceImplName != null && !serviceImplName.isEmpty()) {
				for(CxfServiceEndpoint cse: serviceEndpoints) {
					if(serviceImplName.equals(cse.getServiceProxyImplName())) {
						return cse;
					}
				}
			} else {
				return serviceEndpoints.get(0);
			}
		}
		return null;
	}

	private InterfaceMonitor startMonitor(String serviceName) {
		CxfEndpointListener cxfEndpointListener = new CxfEndpointListener(serviceEndpointMap);
		InterfaceMonitor monitor = new InterfaceMonitor(zooKeeperClient, serviceName, cxfEndpointListener, SERVICE_SCOPE);
		serviceMonitorMap.put(serviceName, monitor);
		monitor.start();
		return monitor;
	}

	public void close() throws InterruptedException {
		for(InterfaceMonitor monitor: serviceMonitorMap.values()) {
			monitor.close();
		}
		curatorClient.close();		
	}

}
