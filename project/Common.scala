
import sbt._
import Keys._

import com.typesafe.sbt.osgi.SbtOsgi.autoImport._
import com.typesafe.sbt.osgi.SbtOsgi

import Dependencies._
import Global._

object Common {
	lazy val commonSettings = Seq(
			organization := "org.dcs",
			scalaVersion := "2.11.7",
			crossPaths := false,
			checksums in update := Nil,
			javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint:unchecked"),
			javacOptions in doc := Seq("-source", "1.8")
			)
}
