package org.dcs.api.service;

import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService
public interface SingleImplTestService {
	
	@WebMethod
	public void testMethod();

}
