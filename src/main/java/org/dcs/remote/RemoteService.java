package org.dcs.remote;

import javax.servlet.ServletContext;

import org.dcs.remote.osgi.FrameworkService;

/**
 * Created by laurent on 16/02/16.
 * THIS CLASS IS JUST A STUB FOR COMPILATION. It actually does nothing.
 */
public class RemoteService {


	private static FrameworkService frameworkService;

	public static Object getService(String className) {
		return frameworkService.getService(className);
	}

	public static void initialize(ServletContext servletContext) {
		frameworkService = new FrameworkService(servletContext);
	}

	public static FrameworkService getFrameworkService() {
		return frameworkService;
	}
}
