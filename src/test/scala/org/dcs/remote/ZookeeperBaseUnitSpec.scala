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
