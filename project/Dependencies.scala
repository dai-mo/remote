import sbt._

object Dependencies {

	    // Versions	    
	    lazy val dcsTestVersion   = "1.0.0-SNAPSHOT"
	    lazy val dcsCommonsVersion= "1.0.0-SNAPSHOT"
	    lazy val servletVersion   = "3.1.0"
	    lazy val felixVersion     = "5.4.0"
	    lazy val cxfDosgiVersion  = "1.7.0"
			lazy val paxCdiVersion    = "0.12.0"
			lazy val cdiApiVersion    = "1.2"
			lazy val logbackVersion   = "1.1.3"
			lazy val curatorVersion   = "2.10.0"
			lazy val zookeeperVersion = "3.4.7"
			lazy val scalaTestVersion = "2.2.6"
			lazy val juiVersion       = "0.11"
			lazy val jacksonVersion   = "2.4.5"


			// Libraries
			val dcsTest         = "org.dcs"                          % "org.dcs.test"                       % dcsTestVersion
			val dcsCommons      = "org.dcs"                          % "org.dcs.commons"                    % dcsCommonsVersion
			val servletApi      = "javax.servlet"                    % "javax.servlet-api"                  % servletVersion
			val felix           = "org.apache.felix"                 % "org.apache.felix.framework"         % felixVersion
			val cxfDosgi        = "org.apache.cxf.dosgi"             % "cxf-dosgi-ri-dsw-cxf"               % cxfDosgiVersion
			val cxfDosgiLocal   = "org.apache.cxf.dosgi"             % "cxf-dosgi-ri-discovery-local"       % cxfDosgiVersion
			val cxfDosgiDist    = "org.apache.cxf.dosgi"             % "cxf-dosgi-ri-discovery-distributed" % cxfDosgiVersion
			val curatorClient   = "org.apache.curator"               % "curator-client"                     % curatorVersion
			val curator         = "org.apache.curator"               % "curator-framework"                  % curatorVersion
			val curatorTest     = "org.apache.curator"               % "curator-test"                       % curatorVersion
			val logbackCore     = "ch.qos.logback"                   % "logback-core"                       % logbackVersion
			val logbackClassic  =	"ch.qos.logback"                   % "logback-classic"                    % logbackVersion
			val zookeeper       = "org.apache.zookeeper"             % "zookeeper"                          % zookeeperVersion
			val jksonDatabind   = "com.fasterxml.jackson.core"       % "jackson-databind"                   % jacksonVersion
			val jksonCore       = "com.fasterxml.jackson.core"       % "jackson-core"                       % jacksonVersion
			val jksonDataFormat = "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml"            % jacksonVersion

			val scalaTest       = "org.scalatest"                    %% "scalatest"                         % scalaTestVersion
			val junitInterface  = "com.novocode"                     % "junit-interface"                    % juiVersion

			// Dependencies
			val remoteDependencies = Seq(
			    dcsCommons,
			    servletApi,
			    felix,
			    cxfDosgi,
			    cxfDosgiLocal,
			    cxfDosgiDist,
					logbackCore,
					logbackClassic,
					curator,
					curatorClient,
					curatorTest,
					zookeeper,
					jksonCore,
					jksonDatabind,
					jksonDataFormat,

					dcsTest         % "test",
					scalaTest       % "test",
					junitInterface  % "test"
					)
}
