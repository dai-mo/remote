package org.dcs.remote;

import java.io.IOException;

import org.apache.curator.test.TestingServer;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.Assert;

public class ZookeeperWSDLTest {

  static final Logger logger = LoggerFactory.getLogger(ZookeeperWSDLTest.class);
	private static TestingServer zkTestServer;

	@BeforeClass
	public static void startZookeeper() throws Exception {
		zkTestServer = new TestingServer(2282);
	}

	
	@Test
	public void testZookeeperServiceNode() throws InterruptedException {
		ZooKeeper client = null;
		try {
			client = new ZooKeeper("localhost:2282", 5000, null);
			client.create("/osgi", "data".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
			String expected = new String("data".getBytes());
			String nodeData = new String(client.getData("/osgi", false, null));
			Assert.assertEquals(expected, nodeData);
		} catch (KeeperException | InterruptedException | IOException e) {
			e.printStackTrace();
			Assert.fail("No error should be thrown on client initialisation / add");
		} finally {
			if(client != null) {
				client.close();
			}
		}
		

	}

	@AfterClass
	public static void stopZookeeper() throws IOException {
		zkTestServer.stop();
	}

}
