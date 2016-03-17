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
import org.dcs.api.service.TestService;
import org.dcs.api.service.TestServiceImpl;
import org.dcs.remote.cxf.CxfServiceEndpoint;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.Assert;

public class ZookeeperWSDLTest {

	private static final Logger logger = LoggerFactory.getLogger(ZookeeperWSDLTest.class);
	private static TestingServer zkTestServer;

	private static final String A_TEST_SERVICE_PATH = 
			"/osgi/service_registry/org/dcs/api/service/TestService/localhost#9000##org#dcs#api#service#impl#ATestServiceImpl";

	private static final String B_TEST_SERVICE_PATH = 
			"/osgi/service_registry/org/dcs/api/service/TestService/localhost#9000##org#dcs#api#service#impl#BTestServiceImpl";

	private static final String TEST_SERVICE_NAME = "org.dcs.api.service.TestService";
	private static final String A_TEST_SERVICE_IMPL_NAME = "org.dcs.api.service.impl.ATestServiceImpl";
	private static final String B_TEST_SERVICE_IMPL_NAME = "org.dcs.api.service.impl.BTestServiceImpl";

	private static final String ZOOKEEPER_SERVER = "localhost:2282";
	@BeforeClass
	public static void startZookeeper() throws Exception {
		zkTestServer = new TestingServer(2282);
	}


	@Test
	public void testZookeeperServiceNode() throws InterruptedException {

		ZooKeeperServiceTracker tracker = null;
		RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
		try(CuratorFramework client = CuratorFrameworkFactory.newClient(ZOOKEEPER_SERVER, retryPolicy)) {

			client.start();

			Path path = Paths.get(this.getClass().getResource("ATestServiceData.xml").toURI());
			byte[] abytes = Files.readAllBytes(path);								

			// Add A Test Service Impl node
			client.create().creatingParentsIfNeeded().forPath(A_TEST_SERVICE_PATH, abytes);			
			String expected = new String(abytes);
			String nodeData = new String(client.getData().forPath(A_TEST_SERVICE_PATH));	
			Assert.assertEquals(expected, nodeData);

			// Check A Test Service Impl is retrived
			tracker = new ZooKeeperServiceTracker();
			CxfServiceEndpoint se = tracker.getServiceEndpoint(TestService.class);		
			Assert.assertNotNull(se.getServiceProxy());
			Assert.assertTrue(se.getServiceProxy() instanceof TestService);
			Assert.assertEquals(A_TEST_SERVICE_IMPL_NAME, se.getServiceProxyImplName());

			path = Paths.get(this.getClass().getResource("BTestServiceData.xml").toURI());
			byte[] bbytes = Files.readAllBytes(path);	

			// Add B Test Service Impl node
			client.create().creatingParentsIfNeeded().forPath(B_TEST_SERVICE_PATH, bbytes);			
			expected = new String(bbytes);
			nodeData = new String(client.getData().forPath(B_TEST_SERVICE_PATH));	
			Assert.assertEquals(expected, nodeData);

			Thread.sleep(2000);

			// Check A Test Service Impl is retrived
			se = tracker.getServiceEndpoint(TestService.class,B_TEST_SERVICE_IMPL_NAME);		
			Assert.assertNotNull(se.getServiceProxy());
			Assert.assertTrue(se.getServiceProxy() instanceof TestService);
			Assert.assertEquals(B_TEST_SERVICE_IMPL_NAME, se.getServiceProxyImplName());

			// Delete A and B Test Service Impl nodes
			String zooKeeperServicePath = Utils.getZooKeeperPath(TEST_SERVICE_NAME);
			List<String> serviceNodes = client.getChildren().forPath(zooKeeperServicePath);	

			for(String serviceNode: serviceNodes) {
				client.delete().forPath(zooKeeperServicePath + "/" + serviceNode);
			}

			logger.warn("Waiting for service node delete to be triggered");
			Thread.sleep(2000);
			Assert.assertNull(tracker.getService(TestService.class));


			client.create().creatingParentsIfNeeded().forPath(A_TEST_SERVICE_PATH, abytes);
			logger.warn("Waiting for service node add to be triggered");
			Thread.sleep(2000);
			se = tracker.getServiceEndpoint(TestService.class,A_TEST_SERVICE_IMPL_NAME);		
			Assert.assertNotNull(se.getServiceProxy());
			Assert.assertTrue(se.getServiceProxy() instanceof TestService);
			Assert.assertEquals(A_TEST_SERVICE_IMPL_NAME, se.getServiceProxyImplName());

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
