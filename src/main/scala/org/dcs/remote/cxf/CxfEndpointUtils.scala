/*
 * Copyright (c) 2017-2018 brewlabs SAS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.dcs.remote.cxf


import org.dcs.api.service.ProcessorServiceDefinition
import org.osgi.service.remoteserviceadmin.EndpointDescription

object CxfEndpointUtils {
	private val CxfBasePath: String = "cxf/"
	private val AddressKey: String = "org.apache.cxf.ws.address"
	val EndpointIdKey: String = "endpoint.id"
	val ClassNameKey: String = "className"
	val ObjectClassKey: String = "objectClass"
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
		val indexOfCxfBasePath = endpointId.indexOf(CxfBasePath)
		var name: Option[String] = None
		if(indexOfCxfBasePath != -1) {
			val startIndex = endpointId.indexOf(CxfBasePath) + CxfBasePath.length()
			name = Some(endpointId.substring(startIndex).replaceAll("/", "."))
		}
		name
	}

	def matchesPropertyValue(props: Map[String, AnyRef], property: String, regex: String): Boolean = {
		val propertyValue =
			if(property == ClassNameKey)
				props.get(EndpointIdKey)
			else
				props.get(property)


		if(propertyValue.isDefined) {
			propertyValue.get match {
				case s:String if property == ClassNameKey =>
					serviceProxyImplName(s).exists(_.matches("(?i)" + regex))
				case s:String => s.matches("(?i)" + regex)
				case _ => false
			}
		} else
			false
	}

	def toProcessorServiceDefinition(props: Map[String, AnyRef]): ProcessorServiceDefinition = {
		ProcessorServiceDefinition(props.get(EndpointIdKey).map(_.asInstanceOf[String]).flatMap(serviceProxyImplName).get,
			props.get(ProcessorTypeKey).map(_.asInstanceOf[String]).get,
			props.get(ObjectClassKey).map(_.asInstanceOf[Array[String]].contains(StatefulServiceInterface)).get)
	}
}