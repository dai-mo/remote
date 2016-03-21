package org.dcs.remote.cxf;

import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;

import javax.wsdl.WSDLException;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.dosgi.dsw.handlers.ServiceInvocationHandler;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.wsdl.WSDLManager;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CxfWSDLUtils {

	private static final Logger logger = LoggerFactory.getLogger(CxfWSDLUtils.class);


	public static Object createProxy(Class<?> iClass, EndpointDescription endpoint){		

		
		ClientProxyFactoryBean factory = new ClientProxyFactoryBean();
		
		factory.setServiceClass(iClass);
		factory.getServiceFactory().setDataBinding(new AegisDatabinding());
		factory.setAddress(CxfEndpointUtils.getAddress(endpoint));
		
		URL wsdlAddress;
		try {
			wsdlAddress = getWsdlAddress(endpoint, iClass);
			// The CXF Bus used to cache the schema definition corresponding to wsdl locations.
			// if the proxy is recreated after a reload of the service on karaf side, the exception
			// `org.apache.ws.commons.schema.XmlSchemaException: Schema name conflict in collection`
			// is thrown.
			// To workaround this it is required to remove the schema definition.
			Bus defaultBus = BusFactory.getDefaultBus();
			defaultBus.getExtension(WSDLManager.class)
				.removeDefinition(defaultBus.getExtension(WSDLManager.class).getDefinition(wsdlAddress.toString()));
		} catch (MalformedURLException | WSDLException ex) {
			logger.warn("WSDL address is malformed");
			return null;
		}
		
		factory.setWsdlLocation(wsdlAddress.toString());
		Object clientProxy = factory.create();
				
		final Client client = ClientProxy.getClient(clientProxy);
    
    client.getInInterceptors().add(new LoggingInInterceptor());
    client.getOutInterceptors().add(new LoggingOutInterceptor());

//    long TIME_OUT = 60000;
//    final HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
//
//    httpClientPolicy.setReceiveTimeout(TIME_OUT);
//    httpClientPolicy.setAllowChunking(false);
//    httpClientPolicy.setConnectionTimeout(TIME_OUT);
//
//    final HTTPConduit http = (HTTPConduit) client.getConduit();
//    http.setClient(httpClientPolicy);
    
    return getProxy(clientProxy, iClass);
	}



	private static URL getWsdlAddress(EndpointDescription endpoint, Class<?> iClass) throws MalformedURLException {
		String address = CxfEndpointUtils.getAddress(endpoint);
		URL serviceUrl = null;
		if (address != null) {
			if(address.startsWith("http")) {
				address += "?wsdl";
				serviceUrl = new URL(address);
			} else if (address.endsWith(".wsdl")){
				serviceUrl = CxfWSDLUtils.class.getResource(address);     
			}
		}

		return serviceUrl;
	}

	protected static Object getProxy(Object serviceProxy, Class<?> iType) {
		return Proxy.newProxyInstance(iType.getClassLoader(), new Class[] {
				iType
		}, new ServiceInvocationHandler(serviceProxy, iType));
	}
}
