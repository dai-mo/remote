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

package org.dcs.remote

import java.nio.file.{Files, Path, Paths}

import org.dcs.api.service.{RemoteProcessorService, StatefulRemoteProcessorService}
import org.dcs.remote.cxf.ZookeeperWSDLSpec.client

/**
  * Created by cmathew on 05.04.17.
  */
object zkClient extends ZookeeperServiceTracker

class ZookeeperBaseUnitSpec extends RemoteBaseUnitSpec {

  def addService(serviceDefFileName: String,
                 servicePath: String): Unit = {
    val path: Path = Paths.get(this.getClass.getResource(serviceDefFileName).toURI)
    val serviceDef: Array[Byte] = Files.readAllBytes(path)

    client.create().creatingParentsIfNeeded().forPath(servicePath, serviceDef)
    val expected = new String(serviceDef)
    val nodeData = new String(client.getData.forPath(servicePath))
    assert(expected == nodeData)
  }

  def addStatelessRemoteService(serviceDefFileName: String,
                                servicePath: String,
                                serviceClassName: String): RemoteProcessorService = {
    addService(serviceDefFileName, servicePath)
    val service = zkClient.service[RemoteProcessorService](serviceClassName)
    service should not be None
    service.get
  }

  def addStatefulRemoteService(serviceDefFileName: String,
                               servicePath: String,
                               serviceClassName: String): RemoteProcessorService = {
    addService(serviceDefFileName, servicePath)
    val service = zkClient.service[StatefulRemoteProcessorService](serviceClassName)
    service should not be None
    service.get
  }
}
