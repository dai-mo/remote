/*
 * Copyright (c) 2017-2018 brewlabs SAS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

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
