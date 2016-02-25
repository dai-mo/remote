package org.dcs.remote.osgi;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by cmathew on 16/01/16.
 */
public class HostActivator implements BundleActivator
{
  private BundleContext bundleContext = null;

  private Map<String, ServiceTracker> classNameTrackerMap;

  public void start(BundleContext context) throws Exception
  {
    bundleContext = context;
    classNameTrackerMap = new HashMap<>();    
  }

  public void stop(BundleContext context)
  {
    bundleContext = null;
    for(ServiceTracker tracker : classNameTrackerMap.values()) {
      tracker.close();
    }
  }

  public BundleContext getContext() {
    return bundleContext;
  }

  public Object getService(String className) {
    ServiceTracker<?,?> tracker = classNameTrackerMap.get(className);
    if(tracker == null) {
      tracker = new ServiceTracker<>(bundleContext, className, null);
      tracker.open(true);
      try {
      	// TODO: The first time the tracker is launched, 
      	//       it takes some time to create the remote stub,
      	//       so we wait for a bit.
      	//       This is clearly not ideal and should be replaced with
      	//       some kind of overall callback mechanism like
      	//       https://jersey.java.net/documentation/latest/async.html
				tracker.waitForService(5000);
			} catch (InterruptedException e) {
				// TODO: Throw a rest exception
			}
    }
    Object service = tracker.getService();
    if(service != null) {
      classNameTrackerMap.put(className, tracker);
    }
    return service;
  }
}
