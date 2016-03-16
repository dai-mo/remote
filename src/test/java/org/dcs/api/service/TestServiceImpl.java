package org.dcs.api.service;

import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService
public class TestServiceImpl implements TestService {

	@WebMethod
	@Override
	public void testMethod() {		
	}

}
