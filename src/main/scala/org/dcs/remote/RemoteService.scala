package org.dcs.remote

import scala.reflect.ClassTag

object RemoteService {
  
  ZookeeperServiceTracker.start
  
	def service[T](implicit tag: ClassTag[T]): Option[T] = {
		ZookeeperServiceTracker.service[T]
	}

	def close  =  {
	  ZookeeperServiceTracker.close
	}
	
class RemoteService

}