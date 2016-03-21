package org.dcs.remote;

import java.nio.channels.IllegalSelectorException;

import org.dcs.remote.osgi.FrameworkService;

/**
 * Created by laurent on 16/02/16.
 * 
 */
public class RemoteService {

	
	private static ZooKeeperServiceTracker zst;

	public static Object getService(Class<?> serviceClass)  {
		try {
			return getZooKeeperServiceTracker().getService(serviceClass);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
	
	public static void initialize()  {
		getZooKeeperServiceTracker();
	}
	
	public static void close() {
		if(zst != null) {
			try {
				zst.close();
			} catch (InterruptedException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	private static ZooKeeperServiceTracker getZooKeeperServiceTracker()  {
		if(zst == null) {
			try {
				zst = new ZooKeeperServiceTracker();
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}
		return zst;
	}

}
