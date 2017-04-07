package org.dcs.remote

import org.dcs.api.service.ProcessorServiceDefinition

import scala.reflect.ClassTag

trait ServiceTracker {
  def start
  def service[T](implicit tag: ClassTag[T]): Option[T]
  def service[T](serviceImplName: String)(implicit tag: ClassTag[T]): Option[T]
  def filterServiceByProperty(property: String, regex: String): List[ProcessorServiceDefinition]
  def services(): List[ProcessorServiceDefinition]
  def close
}