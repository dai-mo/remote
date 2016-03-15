package org.dcs.remote;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.apache.cxf.dosgi.discovery.zookeeper.util.Utils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.Assert;

public class ZookeeperWSDLTest {

	private static final Logger logger = LoggerFactory.getLogger(ZookeeperWSDLTest.class);
	private static TestingServer zkTestServer;

	private static final String TEST_SERVICE_PATH = 
			"/osgi/service_registry/org/dcs/api/service/TestService/localhost#9000##org#dcs#api#service#TestService";
	
	private static final String TEST_SERVICE_NAME = "org.dcs.api.service.TestService";

	@BeforeClass
	public static void startZookeeper() throws Exception {
		zkTestServer = new TestingServer(2282);
	}


	@Test
	public void testZookeeperServiceNode() throws InterruptedException {

		ZooKeeperServiceTracker tracker = null;
		RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
		try(CuratorFramework client = CuratorFrameworkFactory.newClient("localhost:2282", retryPolicy)) {

			client.start();

			Path path = Paths.get(this.getClass().getResource("TestServiceData.xml").toURI());
			byte[] bytes = Files.readAllBytes(path);								

			client.create().creatingParentsIfNeeded().forPath(TEST_SERVICE_PATH, bytes);			
			String expected = new String(bytes);
			String nodeData = new String(client.getData().forPath(TEST_SERVICE_PATH));	
			Assert.assertEquals(expected, nodeData);

			tracker = new ZooKeeperServiceTracker();
			Object obj = tracker.getService(TEST_SERVICE_NAME);		

			String zooKeeperServicePath = Utils.getZooKeeperPath(TEST_SERVICE_NAME);
			List<String> serviceNodes = client.getChildren().forPath(zooKeeperServicePath);	

			for(String serviceNode: serviceNodes) {
				client.delete().forPath(zooKeeperServicePath + "/" + serviceNode);
			}

			logger.warn("Waiting for service node delete to be triggered");
			Thread.sleep(5000);
			Assert.assertNull(tracker.getService(TEST_SERVICE_NAME));
			
			client.create().creatingParentsIfNeeded().forPath(TEST_SERVICE_PATH, bytes);
			logger.warn("Waiting for service node add to be triggered");
			Thread.sleep(5000);
			Assert.assertNotNull(tracker.getService(TEST_SERVICE_NAME));

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("No error should be thrown on client initialisation / add");
		} finally {
			if(tracker != null) {
				tracker.close();
			}
		}


	}

	@AfterClass
	public static void stopZookeeper() throws IOException {
		zkTestServer.stop();
	}

}
