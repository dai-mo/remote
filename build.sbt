import Dependencies._
import Common._


lazy val root = (project in file(".")).
  configs(IntegrationTest).
  settings(commonSettings: _*).
  settings(Defaults.itSettings: _*).
  settings(
    name := "org.dcs.remote",
    moduleName := name.value,
    libraryDependencies ++= remoteDependencies
  )
