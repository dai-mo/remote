package org.dcs.remote

import scala.reflect.ClassTag

trait ServiceTracker {
  def start
  def service[T](implicit tag: ClassTag[T]): Option[T]
  def service[T](serviceImplName: String)(implicit tag: ClassTag[T]): Option[T]
  def close
}