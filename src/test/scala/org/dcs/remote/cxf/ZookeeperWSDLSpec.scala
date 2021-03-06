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

package org.dcs.remote.cxf

import java.util.List

import org.apache.curator.RetryPolicy
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.curator.test.TestingServer
import org.apache.cxf.dosgi.discovery.zookeeper.util.Utils
import org.dcs.api.service.{MultiImplTestService, SingleImplTestService}
import org.dcs.remote.cxf.ZookeeperWSDLSpec._
import org.dcs.remote.{ZookeeperBaseUnitSpec, ZookeeperServiceTracker, zkClient}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConversions.asScalaBuffer


object ZookeeperWSDLSpec {

  val logger: Logger = LoggerFactory.getLogger(classOf[ZookeeperWSDLSpec])

  var zkTestServer: TestingServer = _
  var client: CuratorFramework = _

  val ATestServicePath =
    "/osgi/service_registry/org/dcs/api/service/MultiImplTestService/localhost#9000##org#dcs#api#service#impl#ATestServiceImpl"

  val BTestServicePath =
    "/osgi/service_registry/org/dcs/api/service/MultiImplTestService/localhost#9000##org#dcs#api#service#impl#BTestServiceImpl"

  val MultiImplTestServiceName = "org.dcs.api.service.MultiImplTestService"
  val ATestServiceImplName = "org.dcs.api.service.impl.ATestServiceImpl"
  val BTestServiceImplName = "org.dcs.api.service.impl.BTestServiceImpl"

  val SingleImplTestServicePath =
    "/osgi/service_registry/org/dcs/api/service/SingleImplTestService/localhost#9000##org#dcs#api#service#SingleImplTestService"

  val SingleImplTestServiceName = "org.dcs.api.service.SingleImplTestService"

  val ZookeeperServer = "localhost:2282"

  
}


class ZookeeperWSDLSpec extends ZookeeperBaseUnitSpec {

  before {
    zkTestServer = new TestingServer(2282)

    val retryPolicy: RetryPolicy = new ExponentialBackoffRetry(1000, 3)
    client = CuratorFrameworkFactory.newClient(ZookeeperServer, retryPolicy)
    client.start()
  }

  after {
    client.close()
    zkTestServer.stop()
  }

  override def beforeEach(): Unit = {
    zkClient.start
  }

  override def afterEach(): Unit = {
    zkClient.close
  }

  "The Zookeeper Service Tracker" should "work in the case of services with a single implementation" in {
    addService("SingleImplTestServiceData.xml", SingleImplTestServicePath)

    // Check Single Impl Test Service Impl is retrieved

    var service = zkClient.service[SingleImplTestService]

    service should not be None
    assert(service.get.isInstanceOf[SingleImplTestService])    
    
    var endpoint = zkClient.serviceEndpoint[SingleImplTestService]
    endpoint.get.serviceProxyImplName should be (None)

    // Delete A and B Test Service Impl nodes
    val zooKeeperServicePath = Utils.getZooKeeperPath(SingleImplTestServiceName)
    val serviceNodes: List[String] = client.getChildren.forPath(zooKeeperServicePath)

    for (serviceNode <- serviceNodes) {
      client.delete().forPath(zooKeeperServicePath + "/" + serviceNode)
    }

    logger.warn("Waiting for service node delete to be triggered")
    Thread.sleep(2000)

    zkClient.service[SingleImplTestService] should be (None)

    addService("SingleImplTestServiceData.xml", SingleImplTestServicePath)

    logger.warn("Waiting for service node add to be triggered")
    Thread.sleep(2000)

    service = zkClient.service[SingleImplTestService]
    service should not be None
    assert(service.get.isInstanceOf[SingleImplTestService])   
    
    endpoint = zkClient.serviceEndpoint[SingleImplTestService]
    endpoint.get.serviceProxyImplName should be (None)
  }

  "The Zookeeper Service Tracker" should "work in the case of services with multiple implementations" in {
    addService("ATestServiceData.xml", ATestServicePath)

    // Check A Test Service Impl is retrieved
    var service = zkClient.service[MultiImplTestService]

    service should not be None
    assert(service.get.isInstanceOf[MultiImplTestService])
    
    var endpoint = zkClient.serviceEndpoint[MultiImplTestService]
    endpoint.get.serviceProxyImplName.get should be (ATestServiceImplName)

    addService("BTestServiceData.xml", BTestServicePath)
    // Check A Test Service Impl is retrieved
    service = zkClient.service[MultiImplTestService]

    service should not be None
    assert(service.get.isInstanceOf[MultiImplTestService])
    
    endpoint = zkClient.serviceEndpoint[MultiImplTestService](BTestServiceImplName)
    endpoint.get.serviceProxyImplName.get should be (BTestServiceImplName)

    // Delete A and B Test Service Impl nodes
    val zooKeeperServicePath = Utils.getZooKeeperPath(MultiImplTestServiceName)
    val serviceNodes: List[String] = client.getChildren.forPath(zooKeeperServicePath)

    for (serviceNode <- serviceNodes) {
      client.delete().forPath(zooKeeperServicePath + "/" + serviceNode)
    }

    logger.warn("Waiting for service node delete to be triggered")
    Thread.sleep(2000)

    zkClient.service[MultiImplTestService] should be (None)

    addService("ATestServiceData.xml", ATestServicePath)

    logger.warn("Waiting for service node add to be triggered")
    Thread.sleep(2000)

    service = zkClient.service[MultiImplTestService]

    service should not be None
    assert(service.get.isInstanceOf[MultiImplTestService])
    
    endpoint = zkClient.serviceEndpoint[MultiImplTestService](ATestServiceImplName)
    endpoint.get.serviceProxyImplName.get should be (ATestServiceImplName)
  }

  "The Zookeeper Service Tracker" should "correctly filter services over tags / type" in {

    val StatelessTestProcessorServiceClassName =  "org.dcs.api.service.StatelessTestService"
    val StatefulTestProcessorServiceClassName = "org.dcs.api.service.StatefulTestService"

    addStatelessRemoteService("StatelessTestServiceData.xml",
      ZookeeperServiceTracker.StatelessRemoteServicePath + "/dcs-core#8181##cxf#org#dcs#api#service#StatelessTestService",
      StatelessTestProcessorServiceClassName)

    addStatefulRemoteService("StatefulTestServiceData.xml",
      ZookeeperServiceTracker.StatefulRemoteServicePath + "/dcs-core#8181##cxf#org#dcs#api#service#StatefulTestService",
      StatefulTestProcessorServiceClassName)

    zkClient.loadServiceCaches()
    var pds = zkClient.services()
    assert(pds.size == 2)

    pds = zkClient.filterServiceByProperty(CxfEndpointUtils.TagsKey, ".*TEST.*")
    assert(pds.size == 2)
    assert(pds.exists(_.processorServiceClassName == StatefulTestProcessorServiceClassName))
    assert(pds.exists(_.processorServiceClassName == StatelessTestProcessorServiceClassName))

    pds = zkClient.filterServiceByProperty(CxfEndpointUtils.TagsKey, ".*state.*")
    assert(pds.size == 2)
    assert(pds.exists(_.processorServiceClassName == StatefulTestProcessorServiceClassName))
    assert(pds.exists(_.processorServiceClassName == StatelessTestProcessorServiceClassName))

    pds = zkClient.filterServiceByProperty(CxfEndpointUtils.TagsKey, ".*stateLESS.*")
    assert(pds.size == 1)
    assert(pds.head.processorServiceClassName == StatelessTestProcessorServiceClassName)

    pds = zkClient.filterServiceByProperty(CxfEndpointUtils.TagsKey, ".*stateful.*")
    assert(pds.size == 1)
    assert(pds.head.processorServiceClassName == StatefulTestProcessorServiceClassName)

    pds = zkClient.filterServiceByProperty(CxfEndpointUtils.ProcessorTypeKey, "Worker")
    assert(pds.size == 1)
    assert(pds.head.processorServiceClassName == StatelessTestProcessorServiceClassName)

    pds = zkClient.filterServiceByProperty(CxfEndpointUtils.ProcessorTypeKey, "ingestion")
    assert(pds.size == 1)
    assert(pds.head.processorServiceClassName == StatefulTestProcessorServiceClassName)

    pds = zkClient.filterServiceByProperty(CxfEndpointUtils.ClassNameKey, StatefulTestProcessorServiceClassName)
    assert(pds.size == 1)
    assert(pds.head.processorServiceClassName == StatefulTestProcessorServiceClassName)

    pds = zkClient.filterServiceByProperty(CxfEndpointUtils.ProcessorTypeKey, "£$%^&")
    assert(pds.isEmpty)
  }
}