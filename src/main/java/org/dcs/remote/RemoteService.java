package org.dcs.remote;

/**
 * Created by laurent on 16/02/16.
 * 
 */
public class RemoteService {


	private static ZooKeeperServiceTracker zst;

	public static Object getService(Class<?> serviceClass)  {
		if(zst == null) {
			throw new IllegalStateException("Zookeeper service tracker has not been initialised");
		}
		
		try {
			return zst.getService(serviceClass);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public static void initialize()  {
		initZooKeeperServiceTracker();
	}

	public static void initialize(String serverList)  {
		initZooKeeperServiceTracker(serverList);
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

	private static void initZooKeeperServiceTracker()  {
		if(zst != null) {
			return;
		}
		try {
			zst = new ZooKeeperServiceTracker();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private static void initZooKeeperServiceTracker(String serverList)  {
		if(zst != null) {
			return;
		}
		try {
			zst = new ZooKeeperServiceTracker(serverList);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
