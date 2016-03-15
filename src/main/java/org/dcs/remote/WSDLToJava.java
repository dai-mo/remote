package org.dcs.remote;

public class WSDLToJava {
	
	public static Object toServiceObject(String serviceAddress) {
		return serviceAddress + "?wsdl";
	}

}
