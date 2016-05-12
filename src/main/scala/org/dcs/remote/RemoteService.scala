package org.dcs.remote

import scala.reflect.ClassTag

trait RemoteService {
  this: ServiceTracker =>

  start

  def loadService[T](implicit tag: ClassTag[T]): Option[T] = service[T]

  def loadService[T](serviceImplName: String)(implicit tag: ClassTag[T]) =  service[T](serviceImplName)
  
  def dispose = close

}

object ZkRemoteService extends RemoteService with ZookeeperServiceTracker