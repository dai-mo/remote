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

import org.dcs.api.service.{ProcessorDetails, ProcessorServiceDefinition, RemoteProcessorService, StatefulRemoteProcessorService}
import org.dcs.commons.error.{DCSException, ErrorConstants, HttpException}
import org.dcs.remote.cxf.CxfEndpointUtils

import scala.reflect.ClassTag
import scala.util.control.NonFatal

class  RemoteService {
  this: ServiceTracker =>

  start
  init

  def loadService[T](implicit tag: ClassTag[T]): T = {
    try {
      val s = service[T]
      if(s == None) throw new DCSException(ErrorConstants.DCS201) else s.get
    } catch {
      case NonFatal(t) => throw new DCSException(ErrorConstants.DCS201)
    }
  }
  def loadService[T](serviceImplName: String)(implicit tag: ClassTag[T]) =  {
    try {
      val s = service[T](serviceImplName)
      if(s == None) throw new DCSException(ErrorConstants.DCS201) else s.get
    } catch {
      case NonFatal(t) => throw new DCSException(ErrorConstants.DCS201)
    }
  }

  def dispose = close

  def serviceDetails(processorServiceClassName: String, stateful: Boolean): ProcessorDetails = {
    service(processorServiceClassName, stateful).details()
  }

  def service(processorServiceClassName: String, stateful: Boolean): RemoteProcessorService = {
    if (stateful)
      loadService[StatefulRemoteProcessorService](processorServiceClassName)
    else
      loadService[RemoteProcessorService](processorServiceClassName)
  }

  def service(processorServiceClassName: String): RemoteProcessorService = {
    val psds: List[ProcessorServiceDefinition] =
      filterServiceByProperty(CxfEndpointUtils.ClassNameKey, processorServiceClassName)

    if(psds.isEmpty)
      throw new HttpException(ErrorConstants.DCS301.http(400))
    else {
      val psd = psds.head
      service(psd.processorServiceClassName, psd.stateful)
    }
  }

}

object ZkRemoteService extends RemoteService with ZookeeperServiceTracker
