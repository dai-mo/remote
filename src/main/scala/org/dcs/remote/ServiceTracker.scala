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

package org.dcs.remote

import org.dcs.api.service.{ProcessorDetails, ProcessorServiceDefinition, RemoteProcessorService}

import scala.reflect.ClassTag

trait ServiceTracker {
  def start: Unit
  def init: Unit
  def service[T](implicit tag: ClassTag[T]): Option[T]
  def service[T](serviceImplName: String)(implicit tag: ClassTag[T]): Option[T]
  def filterServiceByProperty(property: String, regex: String): List[ProcessorServiceDefinition]
  def services(): List[ProcessorServiceDefinition]

  def close
}
