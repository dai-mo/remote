package org.dcs.remote;

import java.io.IOException;
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
import org.osgi.service.remoteserviceadmin.EndpointDescription;

public class ZooKeeperServiceTracker {

	private ZooKeeper zooKeeperClient;
	private CuratorFramework curatorClient;

	private ConcurrentHashMap<String, List<Object>> serviceObjectMap;
	private ConcurrentHashMap<String, InterfaceMonitor> serviceMonitorMap;

	private static final String SERVICE_SCOPE = "singleton";

	public ZooKeeperServiceTracker() throws Exception {
		RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
		curatorClient = CuratorFrameworkFactory.newClient("localhost:2282", retryPolicy);
		curatorClient.start();
		zooKeeperClient = curatorClient.getZookeeperClient().getZooKeeper();
		serviceObjectMap = new ConcurrentHashMap<>();
		serviceMonitorMap = new ConcurrentHashMap<>();

	}

	public synchronized Object getService(Class<?> serviceClass) throws Exception {
		String serviceName = serviceClass.getName();

		// First check if the service is currently available ... 
		List<Object> serviceObjects = serviceObjectMap.get(serviceName);

		if(serviceObjects != null && !serviceObjects.isEmpty()) {
			return serviceObjects.get(0);
		}

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
			serviceObjects = new ArrayList<>();
			for(String serviceNode : serviceNodes) {				
				byte[] data = curatorClient.getData().forPath(zooKeeperServicePath + "/" + serviceNode);				
				EndpointDescription firstEnpointDescription = interfaceMonitor.getFirstEnpointDescription(data);
				Object serviceObject =  WSDLToJava.createProxy(serviceClass, firstEnpointDescription);	
				serviceObjects.add(serviceObject);				
				
			}
			if(serviceObjects.isEmpty()) {
				serviceObjectMap.put(serviceName, new ArrayList<>());				
			} else {
				serviceObjectMap.put(serviceName, serviceObjects);
				return serviceObjects.get(0);
			}
		}		
		return null;
	}

	private InterfaceMonitor startMonitor(String serviceName) {
		CxfEndpointListener cxfEndpointListener = new CxfEndpointListener(serviceObjectMap);
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
