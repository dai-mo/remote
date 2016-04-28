package org.dcs.remote.cxf

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.List
import scala.collection.JavaConversions.asScalaBuffer
import org.apache.curator.RetryPolicy
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.curator.test.TestingServer
import org.apache.cxf.dosgi.discovery.zookeeper.util.Utils
import org.dcs.api.service.MultiImplTestService
import org.dcs.api.service.SingleImplTestService
import org.dcs.remote.RemoteBaseUnitSpec
import org.dcs.remote.cxf.ZookeeperWSDLSpec._
import org.slf4j.LoggerFactory
import org.dcs.remote.ZookeeperServiceTracker

object ZookeeperWSDLSpec {

  val logger = LoggerFactory.getLogger(classOf[ZookeeperWSDLSpec])

  var zkTestServer: TestingServer = _
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

class ZookeeperWSDLSpec extends RemoteBaseUnitSpec {

  before {
    zkTestServer = new TestingServer(2282);

    val retryPolicy: RetryPolicy = new ExponentialBackoffRetry(1000, 3)
    client = CuratorFrameworkFactory.newClient(ZookeeperServer, retryPolicy)
    client.start()
  }

  after {
    client.close()
    zkTestServer.stop()
  }

  override def beforeEach() = {
    ZookeeperServiceTracker.start
  }

  override def afterEach() = {
    ZookeeperServiceTracker.close
  }

  "The Zookeeper Service Tracker" should "work in the case of services with a single implementation" in {
    val path: Path = Paths.get(this.getClass().getResource("SingleImplTestServiceData.xml").toURI())
    val abytes: Array[Byte] = Files.readAllBytes(path)

    // Add Single Impl Test Service node
    client.create().creatingParentsIfNeeded().forPath(SingleImplTestServicePath, abytes)
    var expected = new String(abytes)
    var nodeData = new String(client.getData().forPath(SingleImplTestServicePath))
    assert(expected == nodeData)

    // Check Single Impl Test Service Impl is retrieved

    var service = ZookeeperServiceTracker.service[SingleImplTestService]

    service should not be (None)
    assert(service.get.isInstanceOf[SingleImplTestService])    
    
    var endpoint = ZookeeperServiceTracker.serviceEndpoint[SingleImplTestService]
    endpoint.get.serviceProxyImplName should be (None)

    // Delete A and B Test Service Impl nodes
    val zooKeeperServicePath = Utils.getZooKeeperPath(SingleImplTestServiceName)
    val serviceNodes: List[String] = client.getChildren().forPath(zooKeeperServicePath)

    for (serviceNode <- serviceNodes) {
      client.delete().forPath(zooKeeperServicePath + "/" + serviceNode)
    }

    logger.warn("Waiting for service node delete to be triggered")
    Thread.sleep(2000)

    ZookeeperServiceTracker.service[SingleImplTestService] should be (None)

    client.create().creatingParentsIfNeeded().forPath(SingleImplTestServicePath, abytes);
    logger.warn("Waiting for service node add to be triggered");
    Thread.sleep(2000);

    service = ZookeeperServiceTracker.service[SingleImplTestService]
    service should not be (None)
    assert(service.get.isInstanceOf[SingleImplTestService])   
    
    endpoint = ZookeeperServiceTracker.serviceEndpoint[SingleImplTestService]
    endpoint.get.serviceProxyImplName should be (None)
  }

  "The Zookeeper Service Tracker" should "work in the case of services with multiple implementations" in {
    var path: Path = Paths.get(this.getClass().getResource("ATestServiceData.xml").toURI())
    val abytes: Array[Byte] = Files.readAllBytes(path)

    // Add A Test Service Impl node
    client.create().creatingParentsIfNeeded().forPath(ATestServicePath, abytes)
    var expected = new String(abytes)
    var nodeData = new String(client.getData().forPath(ATestServicePath))
    assert(expected == nodeData)

    // Check A Test Service Impl is retrived

    var service = ZookeeperServiceTracker.service[MultiImplTestService]

    service should not be (None)
    assert(service.get.isInstanceOf[MultiImplTestService])
    
    var endpoint = ZookeeperServiceTracker.serviceEndpoint[MultiImplTestService]
    endpoint.get.serviceProxyImplName.get should be (ATestServiceImplName)

    path = Paths.get(this.getClass().getResource("BTestServiceData.xml").toURI())
    val bbytes: Array[Byte] = Files.readAllBytes(path)

    // Add B Test Service Impl node
    client.create().creatingParentsIfNeeded().forPath(BTestServicePath, bbytes)
    expected = new String(bbytes)
    nodeData = new String(client.getData().forPath(BTestServicePath))
    assert(expected == nodeData)

    // Check A Test Service Impl is retrived
    service = ZookeeperServiceTracker.service[MultiImplTestService]

    service should not be (None)
    assert(service.get.isInstanceOf[MultiImplTestService])
    
    endpoint = ZookeeperServiceTracker.serviceEndpoint[MultiImplTestService](BTestServiceImplName)
    endpoint.get.serviceProxyImplName.get should be (BTestServiceImplName)

    // Delete A and B Test Service Impl nodes
    val zooKeeperServicePath = Utils.getZooKeeperPath(MultiImplTestServiceName)
    val serviceNodes: List[String] = client.getChildren().forPath(zooKeeperServicePath)

    for (serviceNode <- serviceNodes) {
      client.delete().forPath(zooKeeperServicePath + "/" + serviceNode)
    }

    logger.warn("Waiting for service node delete to be triggered")
    Thread.sleep(2000)

    ZookeeperServiceTracker.service[MultiImplTestService] should be (None)

    client.create().creatingParentsIfNeeded().forPath(ATestServicePath, abytes)

    logger.warn("Waiting for service node add to be triggered")
    Thread.sleep(2000)

    service = ZookeeperServiceTracker.service[MultiImplTestService]

    service should not be (None)
    assert(service.get.isInstanceOf[MultiImplTestService])
    
    endpoint = ZookeeperServiceTracker.serviceEndpoint[MultiImplTestService](ATestServiceImplName)
    endpoint.get.serviceProxyImplName.get should be (ATestServiceImplName)
  }
}