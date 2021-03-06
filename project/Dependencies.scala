import sbt._

object Dependencies {

	// Versions
	lazy val dcsApiVersion      = "0.5.0-SNAPSHOT"
	lazy val dcsCommonsVersion  = "0.4.0-SNAPSHOT"
	lazy val dcsCoreVersion     = "0.6.0-SNAPSHOT"
	lazy val servletVersion     = "3.1.0"
	lazy val felixVersion       = "5.4.0"
	lazy val cxfDosgiVersion    = "1.7.0"
	lazy val paxCdiVersion      = "0.12.0"
	lazy val cdiApiVersion      = "1.2"
	lazy val logbackVersion     = "1.1.3"
	lazy val curatorVersion     = "2.10.0"
	lazy val zookeeperVersion   = "3.4.7"
	lazy val scalaTestVersion   = "3.0.0"
	lazy val juiVersion         = "0.11"
	lazy val mockitoVersion     = "1.10.19"


	// Libraries

	val dcsApi          = "org.dcs"                          % "org.dcs.api"                        % dcsApiVersion
	val dcsCommons      = "org.dcs"                          % "org.dcs.commons"                    % dcsCommonsVersion
	val dcsCore      = "org.dcs"                          % "org.dcs.core"                    % dcsCoreVersion
	val servletApi      = "javax.servlet"                    % "javax.servlet-api"                  % servletVersion
	val felix           = "org.apache.felix"                 % "org.apache.felix.framework"         % felixVersion
	val cxfDosgi        = "org.apache.cxf.dosgi"             % "cxf-dosgi-ri-dsw-cxf"               % cxfDosgiVersion
	val cxfDosgiLocal   = "org.apache.cxf.dosgi"             % "cxf-dosgi-ri-discovery-local"       % cxfDosgiVersion
	val cxfDosgiDist    = "org.apache.cxf.dosgi"             % "cxf-dosgi-ri-discovery-distributed" % cxfDosgiVersion
	val curatorClient   = "org.apache.curator"               % "curator-client"                     % curatorVersion
	val curatorRecipes  = "org.apache.curator"               % "curator-recipes"                    % curatorVersion
	val curator         = "org.apache.curator"               % "curator-framework"                  % curatorVersion
	val curatorTest     = "org.apache.curator"               % "curator-test"                       % curatorVersion
	val logbackCore     = "ch.qos.logback"                   % "logback-core"                       % logbackVersion
	val logbackClassic  =	"ch.qos.logback"                   % "logback-classic"                    % logbackVersion
	val zookeeper       = "org.apache.zookeeper"             % "zookeeper"                          % zookeeperVersion

	val scalaTest       = "org.scalatest"                    %% "scalatest"                         % scalaTestVersion
	val junitInterface  = "com.novocode"                     % "junit-interface"                    % juiVersion
	val mockitoAll      = "org.mockito"                      % "mockito-all"                        % mockitoVersion

	// Dependencies
	val remoteDependencies = Seq(
		dcsApi % "provided",
		dcsCommons % "provided",
		servletApi,
		felix,
		cxfDosgi,
		cxfDosgiLocal,
		cxfDosgiDist,
		logbackCore,
		logbackClassic,
		curator,
		curatorClient,
    curatorRecipes,
		curatorTest,
		zookeeper,

		dcsCore         % "test",
		scalaTest       % "test",
		junitInterface  % "test",
		mockitoAll      % "test"

	)
}
