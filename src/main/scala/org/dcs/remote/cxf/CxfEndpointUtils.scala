package org.dcs.remote.cxf

import java.util
import java.util.Map

import org.apache.cxf.dosgi.endpointdesc.PropertiesMapper
import org.dcs.api.service.ProcessorServiceDefinition
import org.osgi.service.remoteserviceadmin.EndpointDescription
import org.osgi.xmlns.rsa.v1_0.EndpointDescriptionType

import scala.collection.JavaConverters._

object CxfEndpointUtils {
  private val CxfBasePath: String = "cxf/"
	private val AddressKey: String = "org.apache.cxf.ws.address"
	private val EndpointIdKey: String = "endpoint.id"
	private val ObjectClassKey: String = "objectClass"

  val TagsKey: String = "org.dcs.processor.tags"
  val ProcessorTypeKey: String = "org.dcs.processor.type"
  val StatefulServiceInterface = "org.dcs.api.service.StatefulRemoteProcessorService"
	
	def address(endpointDescription: EndpointDescription): String  = 
	  endpointDescription.getProperties().get(AddressKey).asInstanceOf[String]
	
	def endpointId(endpointDescription: EndpointDescription): String = 
		endpointDescription.getProperties().get(EndpointIdKey).asInstanceOf[String]
	
	
	def serviceInterfaceNames(endpointDescription: EndpointDescription): Array[String] =
		endpointDescription.getProperties().get(ObjectClassKey).asInstanceOf[Array[String]]
	
	
	def serviceProxyImplName(endpointDescription: EndpointDescription): Option[String] = {
		serviceProxyImplName(endpointId(endpointDescription))
	}

	def serviceProxyImplName(endpointId: String): Option[String] = {
		val indexOfCxfBasePath = endpointId.indexOf(CxfBasePath);
		var name: Option[String] = None
		if(indexOfCxfBasePath != -1) {
			val startIndex = endpointId.indexOf(CxfBasePath) + CxfBasePath.length();
			name = Some(endpointId.substring(startIndex).replaceAll("/", "."))
		}
		name
	}

  def matchesPropertyValue(props: util.Map[String, AnyRef], property: String, regex: String): Boolean = {
    val propertyValue = Option(props.get(property)).map(_.asInstanceOf[String])
    if(propertyValue.isDefined) propertyValue.get.matches("(?i)" + regex) else false
  }

	def toProcessorServiceDefinition(props: util.Map[String, AnyRef]): ProcessorServiceDefinition = {
    ProcessorServiceDefinition(serviceProxyImplName(props.get(EndpointIdKey).asInstanceOf[String]).get,
      props.get(ProcessorTypeKey).asInstanceOf[String],
      props.get(ObjectClassKey).asInstanceOf[Array[String]].contains(StatefulServiceInterface))
	}
}