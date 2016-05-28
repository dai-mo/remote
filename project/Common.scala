
import sbt._
import Keys._

import com.typesafe.sbt.osgi.SbtOsgi.autoImport._
import com.typesafe.sbt.osgi.SbtOsgi

import Dependencies._
import Global._

object Common {
	lazy val commonSettings = Seq(
			organization := "org.dcs",
			version := "1.0.0-SNAPSHOT",
			scalaVersion := scVersion,
			crossPaths := false,
			checksums in update := Nil,
			javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint:unchecked"),
			javacOptions in doc := Seq("-source", "1.8")
			)
}
