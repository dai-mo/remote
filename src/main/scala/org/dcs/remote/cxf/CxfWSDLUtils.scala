package org.dcs.remote.cxf

import org.apache.cxf.BusFactory
import org.slf4j.LoggerFactory
import org.apache.cxf.frontend.ClientProxyFactoryBean
import org.apache.cxf.aegis.databinding.AegisDatabinding
import java.net.URL
import java.net.MalformedURLException
import javax.wsdl.WSDLException
import org.apache.cxf.Bus
import org.apache.cxf.wsdl.WSDLManager
import org.osgi.service.remoteserviceadmin.EndpointDescription
import org.apache.cxf.endpoint.Client
import org.apache.cxf.frontend.ClientProxy
import org.apache.cxf.interceptor.LoggingInInterceptor
import org.apache.cxf.interceptor.LoggingOutInterceptor
import java.lang.reflect.Proxy
import org.apache.cxf.dosgi.dsw.handlers.ServiceInvocationHandler

object CxfWSDLUtils {

  private val logger = LoggerFactory.getLogger(classOf[CxfWSDLUtils])

  def proxy(iClass: Class[_], endpointDescription: EndpointDescription): Option[AnyRef] = {

    var wsdlAddress: Option[URL] =
      try {
        Some(getWsdlAddress(endpointDescription, iClass))
      } catch {
        case ex @( _ : MalformedURLException | _ : WSDLException)  => {
          logger.warn("WSDL address is malformed")
          None
        }
        case ex : IllegalStateException  => throw ex
      }

    if (wsdlAddress != None) {
      // The CXF Bus used to cache the schema definition corresponding to wsdl locations.
      // if the proxy is recreated after a reload of the service on karaf side, the exception
      // `org.apache.ws.commons.schema.XmlSchemaException: Schema name conflict in collection`
      // is thrown.
      // To workaround this it is required to remove the schema definition.
      val defaultBus: Bus = BusFactory.getDefaultBus()
      defaultBus.getExtension(classOf[WSDLManager])
        .removeDefinition(defaultBus.getExtension(classOf[WSDLManager]).getDefinition(wsdlAddress.get.toString()))

      val factory: ClientProxyFactoryBean = new ClientProxyFactoryBean()

      factory.setServiceClass(iClass)
      factory.getServiceFactory().setDataBinding(new AegisDatabinding())
      factory.setAddress(CxfEndpointUtils.address(endpointDescription))
      factory.setWsdlLocation(wsdlAddress.get.toString());

      val clientProxy: Any = factory.create();

      val client: Client = ClientProxy.getClient(clientProxy);

      client.getInInterceptors().add(new LoggingInInterceptor());
      client.getOutInterceptors().add(new LoggingOutInterceptor());
      
      Some(Proxy.newProxyInstance(iClass.getClassLoader(), Array[Class[_]](iClass) , new ServiceInvocationHandler(clientProxy, iClass)))
    } else {
      None
    }
  }

  def getWsdlAddress(endpointDescription: EndpointDescription, iClass: Class[_]): URL = {
    val address = CxfEndpointUtils.address(endpointDescription);
    address match {
      case address if (address.startsWith("http")) => new URL(address + "?wsdl")
      case address if (address.endsWith(".wsdl"))  => classOf[CxfWSDLUtils].getResource(address)
      case _                                       => throw new IllegalStateException("Could not generate WSDL address")
     
    }
  }

}

class CxfWSDLUtils 