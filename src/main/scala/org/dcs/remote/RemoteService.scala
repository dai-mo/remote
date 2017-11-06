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

  override def serviceDetails(processorServiceClassName: String, stateful: Boolean): ProcessorDetails = {
    service(processorServiceClassName, stateful).details()
  }

  override def service(processorServiceClassName: String, stateful: Boolean): RemoteProcessorService = {
    if (stateful)
      loadService[StatefulRemoteProcessorService](processorServiceClassName)
    else
      loadService[RemoteProcessorService](processorServiceClassName)
  }

  override def service(processorServiceClassName: String): RemoteProcessorService = {
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
