package org.dcs.api.service;

import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService
public class TestServiceImpl implements MultiImplTestService {

	@WebMethod
	@Override
	public void testMethod() {		
	}

}
