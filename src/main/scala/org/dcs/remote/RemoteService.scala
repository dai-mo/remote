package org.dcs.remote

object RemoteService {
  
  ZookeeperServiceTracker.start
  
	def service(serviceClass: Class[_]): Option[Any] = {
		ZookeeperServiceTracker.service(serviceClass)
	}

	def close  =  {
	  ZookeeperServiceTracker.close
	}
	
class RemoteService

}