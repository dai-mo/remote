package org.dcs.api.service;

import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService
public interface TestService {
	
	@WebMethod
	public void testMethod();

}
