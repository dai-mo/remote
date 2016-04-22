package org.dcs.remote.cxf

import org.dcs.remote.RemoteBaseUnitSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.apache.curator.test.TestingServer
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.RetryPolicy
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.curator.framework.CuratorFramework
import org.dcs.remote.cxf.ScalaZookeeperWSDLTest._
import org.dcs.commons.YamlSerializerImplicits._
import org.dcs.commons.config.ConfigurationFacade
//import org.dcs.remote.ZooKeeperServiceTracker
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files

object ScalaZookeeperWSDLTest {
  val logger  = LoggerFactory.getLogger(classOf[ScalaZookeeperWSDLTest])
  
	var zkTestServer:TestingServer = _
	var client: CuratorFramework = _
	
	val ATestServicePath = 
			"/osgi/service_registry/org/dcs/api/service/MultiImplTestService/localhost#9000##org#dcs#api#service#impl#ATestServiceImpl";

	val BTestServicePath = 
			"/osgi/service_registry/org/dcs/api/service/MultiImplTestService/localhost#9000##org#dcs#api#service#impl#BTestServiceImpl";

	val MultiImplTestServiceName = "org.dcs.api.service.MultiImplTestService";
	val ATestServiceImplName = "org.dcs.api.service.impl.ATestServiceImpl";
	val BTestServiceImplName = "org.dcs.api.service.impl.BTestServiceImpl";

	val SingleImplTestServicePath = 
			"/osgi/service_registry/org/dcs/api/service/SingleImplTestService/localhost#9000##org#dcs#api#service#SingleImplTestService";
	
	val SingleImplTestServiceName = "org.dcs.api.service.SingleImplTestService";
	
	val ZookeeperServer = "localhost:2282";
}

class ScalaZookeeperWSDLTest extends RemoteBaseUnitSpec {
  
  before {    
    zkTestServer = new TestingServer(2282);
    
    val retryPolicy: RetryPolicy  = new ExponentialBackoffRetry(1000, 3)
    client = CuratorFrameworkFactory.newClient(ZookeeperServer, retryPolicy)
    client.start()
  }
  
  after {
    client.close()
    zkTestServer.stop()
  }
  
//  "The Zookeeper Service Tracker" should "work in the case of services with multiple implementations" in {
//    var path:Path  = Paths.get(this.getClass().getResource("ATestServiceData.xml").toURI())
//		var abytes: Array[Byte]  = Files.readAllBytes(path)							
//
//			// Add A Test Service Impl node
//			client.create().creatingParentsIfNeeded().forPath(ATestServicePath, abytes)
//			var expected = new String(abytes)
//			var nodeData = new String(client.getData().forPath(ATestServicePath));	
//			assert(expected == nodeData);
//
//			// Check A Test Service Impl is retrived
//			val tracker = new ZooKeeperServiceTracker();
//			var CxfServiceEndpoint = tracker.getServiceEndpoint(MultiImplTestService.class);		
//			Assert.assertNotNull(se.getServiceProxy());
//			Assert.assertTrue(se.getServiceProxy() instanceof MultiImplTestService);
//			Assert.assertEquals(A_TEST_SERVICE_IMPL_NAME, se.getServiceProxyImplName());
//
//			path = Paths.get(this.getClass().getResource("BTestServiceData.xml").toURI());
//			byte[] bbytes = Files.readAllBytes(path);	
//
//			// Add B Test Service Impl node
//			client.create().creatingParentsIfNeeded().forPath(B_TEST_SERVICE_PATH, bbytes);			
//			expected = new String(bbytes);
//			nodeData = new String(client.getData().forPath(B_TEST_SERVICE_PATH));	
//			Assert.assertEquals(expected, nodeData);
//
//			Thread.sleep(2000);
//
//			// Check A Test Service Impl is retrived
//			se = tracker.getServiceEndpoint(MultiImplTestService.class,B_TEST_SERVICE_IMPL_NAME);		
//			Assert.assertNotNull(se.getServiceProxy());
//			Assert.assertTrue(se.getServiceProxy() instanceof MultiImplTestService);
//			Assert.assertEquals(B_TEST_SERVICE_IMPL_NAME, se.getServiceProxyImplName());
//
//			// Delete A and B Test Service Impl nodes
//			String zooKeeperServicePath = Utils.getZooKeeperPath(MULTI_IMPL_TEST_SERVICE_NAME);
//			List<String> serviceNodes = client.getChildren().forPath(zooKeeperServicePath);	
//
//			for(String serviceNode: serviceNodes) {
//				client.delete().forPath(zooKeeperServicePath + "/" + serviceNode);
//			}
//
//			logger.warn("Waiting for service node delete to be triggered");
//			Thread.sleep(2000);
//			Assert.assertNull(tracker.getService(MultiImplTestService.class));
//
//
//			client.create().creatingParentsIfNeeded().forPath(A_TEST_SERVICE_PATH, abytes);
//			logger.warn("Waiting for service node add to be triggered");
//			Thread.sleep(2000);
//			
//			se = tracker.getServiceEndpoint(MultiImplTestService.class,A_TEST_SERVICE_IMPL_NAME);		
//			Assert.assertNotNull(se.getServiceProxy());
//			Assert.assertTrue(se.getServiceProxy() instanceof MultiImplTestService);
//			Assert.assertEquals(A_TEST_SERVICE_IMPL_NAME, se.getServiceProxyImplName());
//  }
    
}