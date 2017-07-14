package org.dcs.remote

import org.dcs.commons.error.{DCSException, ErrorConstants}

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

}

object ZkRemoteService extends RemoteService with ZookeeperServiceTracker
