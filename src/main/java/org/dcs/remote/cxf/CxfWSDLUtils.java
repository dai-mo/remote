package org.dcs.remote.cxf;

import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.handlers.ServiceInvocationHandler;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CxfWSDLUtils {

	private static final Logger logger = LoggerFactory.getLogger(CxfWSDLUtils.class);


	public static Object createProxy(Class<?> iClass, EndpointDescription endpoint) {
		URL wsdlAddress;
		try {
			wsdlAddress = getWsdlAddress(endpoint, iClass);
		} catch (MalformedURLException ex) {
			logger.warn("WSDL address is malformed");
			return null;
		}

		logger.info("Creating a " + endpoint.getInterfaces().toArray()[0] + " client, wsdl address is "
				+ getProperty(endpoint, Constants.WSDL_CONFIG_PREFIX));

		String serviceNs = getProperty(endpoint, Constants.WSDL_SERVICE_NAMESPACE);
		if (serviceNs == null) {
			serviceNs = PackageUtils.getNamespace(PackageUtils.getPackageName(iClass));
		}
		String serviceName = getProperty(endpoint, Constants.WSDL_SERVICE_NAME);
		if (serviceName == null) {
			serviceName = iClass.getSimpleName();
		}
		QName serviceQname = getServiceQName(iClass, endpoint.getProperties(),
				Constants.WSDL_SERVICE_NAMESPACE,
				Constants.WSDL_SERVICE_NAME);
		QName portQname = getPortQName(serviceQname.getNamespaceURI(),
				endpoint.getProperties(), Constants.WSDL_PORT_NAME);
		Service service = Service.create(wsdlAddress, serviceQname);
		Object proxy = getProxy(portQname == null ? service.getPort(iClass) : service.getPort(portQname, iClass),
				iClass);		
		return proxy;
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

	public static String getProperty(EndpointDescription endpoint, String name) {
		return getProperty(endpoint.getProperties(), name);
	}

	public static String getProperty(Map<String, Object> dict, String name) {
		Object value = dict.get(name);
		return value instanceof String ? (String) value : null;
	}

	protected static QName getServiceQName(Class<?> iClass, Map<String, Object> sd, String nsPropName,
			String namePropName) {
		String serviceNs = getProperty(sd, nsPropName);
		String serviceName = getProperty(sd, namePropName);
		if (iClass == null && (serviceNs == null || serviceName == null)) {
			return null;
		}
		if (serviceNs == null) {
			serviceNs = PackageUtils.getNamespace(PackageUtils.getPackageName(iClass));
		}
		if (serviceName == null && iClass != null) {
			serviceName = iClass.getSimpleName();
		}
		return new QName(serviceNs, serviceName);
	}

	protected static QName getPortQName(String ns, Map<String, Object> sd, String propName) {
		String portName = getProperty(sd, propName);
		if (portName == null) {
			return null;
		}
		return new QName(ns, portName);
	}

	protected static Object getProxy(Object serviceProxy, Class<?> iType) {
		return Proxy.newProxyInstance(iType.getClassLoader(), new Class[] {
				iType
		}, new ServiceInvocationHandler(serviceProxy, iType));
	}
}
