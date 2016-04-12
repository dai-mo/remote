package org.dcs.remote.cxf

import org.dcs.remote.BaseUnitSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.apache.curator.test.TestingServer
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.RetryPolicy
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.curator.framework.CuratorFramework
import org.dcs.remote.cxf.ScalaZookeeperWSDLTest._

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

class ScalaZookeeperWSDLTest extends BaseUnitSpec {
  
  before {
    zkTestServer = new TestingServer(2282);
    
    val retryPolicy: RetryPolicy  = new ExponentialBackoffRetry(1000, 3)
    client = CuratorFrameworkFactory.newClient(ZookeeperServer, retryPolicy)
    client.start()
  }
  
  after {
    
  }
    
}