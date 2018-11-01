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

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.FelixConstants;

public final class FrameworkService
{
	private final ServletContext context;
	private Felix felix;
	private static HostActivator hostActivator;
	private static final String FELIX_CONFIG_PROPERTY_KEY="felixConfig";

	public FrameworkService(ServletContext context) {
		this.context = context;
	}

	public void start()
	{
		try {
			log("Starting OSGi Framework ...", null);
			doStart();
		} catch (Exception e) {
			log("Failed to start framework", e);
		}
	}

	public void stop()
	{
		try {
			doStop();
		} catch (Exception e) {
			log("Error stopping framework", e);
		}
	}

	private void doStart()
			throws Exception
	{
		Felix tmp = new Felix(createConfig());
		tmp.start();
		this.felix = tmp;
		log("OSGi framework started", null);
		
	}

	private void doStop()
			throws Exception
	{
		log("Stopping OSGi framework ...", null);
		if (this.felix != null) {
			this.felix.stop();
			this.felix.waitForStop(0);
		}

		log("OSGi framework stopped", null);
	}

	private Map<String, Object> createConfig()
			throws Exception
	{
		Properties props = new Properties();
		props.load(this.context.getResourceAsStream("/WEB-INF/framework.properties"));
		String felixConfigPath = System.getProperty(FELIX_CONFIG_PROPERTY_KEY);
		
		if(felixConfigPath != null) {
			File felixConfigFile =  new File(felixConfigPath);
			if(felixConfigFile.exists()) {
				props.load(new FileInputStream(felixConfigFile));
			}
		}
		HashMap<String, Object> map = new HashMap<String, Object>();
		for (Object key : props.keySet()) {
			map.put(key.toString(), props.get(key));
		}

		hostActivator = new HostActivator();
		map.put(FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP,
				Arrays.asList(new ProvisionActivator(this.context), hostActivator));
		return map;
	}

	private void log(String message, Throwable cause)
	{
		this.context.log(message, cause);
	}

	public static Object getService(String className) {
		if(hostActivator != null) {
			Object service = hostActivator.getService(className);
			return service;
		}
		return null;
	}
}
