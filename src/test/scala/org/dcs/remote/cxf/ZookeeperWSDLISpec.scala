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

import org.apache.curator.RetryPolicy
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.curator.test.TestingServer
import org.dcs.api.service.RemoteProcessorService
import org.dcs.remote.{ZookeeperBaseUnitSpec, zkClient}
import org.scalatest.Ignore
import org.slf4j.{Logger, LoggerFactory}

object ZookeeperWSDLISpec {

  val logger: Logger = LoggerFactory.getLogger(classOf[ZookeeperWSDLISpec])

  var zkTestServer: TestingServer = _
  var client: CuratorFramework = _

  val FilterProcessorServiceName = "org.dcs.core.service.FilterProcessorService"

  val ZookeeperRemoteServer = "zookeeper:2181"


}

@Ignore
class ZookeeperWSDLISpec extends ZookeeperBaseUnitSpec {
  import ZookeeperWSDLISpec._

  before {

    val retryPolicy: RetryPolicy = new ExponentialBackoffRetry(1000, 3)
    client = CuratorFrameworkFactory.newClient(ZookeeperRemoteServer, retryPolicy)
    client.start()
  }

  after {
    client.close()
  }

  override def beforeEach(): Unit = {
    zkClient.start
  }

  override def afterEach(): Unit = {
    zkClient.close
  }

  "The Zookeeper Service Tracker" should "work in the case of remote services with a single implementation" in {


    // Check Single Impl Test Service Impl is retrieved

    var service = zkClient.service[RemoteProcessorService]

    service should not be None
    assert(service.get.isInstanceOf[RemoteProcessorService])

    var endpoint = zkClient.serviceEndpoint[RemoteProcessorService](FilterProcessorServiceName)
    endpoint.get.serviceProxyImplName.get should be (FilterProcessorServiceName)

  }
}