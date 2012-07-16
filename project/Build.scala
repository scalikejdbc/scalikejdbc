import sbt._
import Keys._

object ScalikeJDBCProjects extends Build {

  lazy val _organization = "com.github.seratch"
  lazy val _version = "1.3.5-SNAPSHOT"

  lazy val scalikejdbc = Project(
    id = "library", 
    base = file("scalikejdbc-library"), 
    settings = Defaults.defaultSettings ++ Seq(
      organization := _organization,
      name := "scalikejdbc",
      version := _version,
      scalaVersion := "2.9.2",
      crossScalaVersions := _crossScalaVersions,
      publishTo <<= version { (v: String) => _publishTo(v) },
      publishMavenStyle := true,
      resolvers ++= _resolvers,
      libraryDependencies <++= (scalaVersion) { scalaVersion =>
        val _scalaVersion = "_" + (scalaVersion match {
          case "2.9.2" => "2.9.1"
          case version => version
        })
        val time = "time" + _scalaVersion
        val unfilteredFilter = "unfiltered-filter" + _scalaVersion
        val unfilteredJetty = "unfiltered-jetty" + _scalaVersion
        val unfilteredSpec = "unfiltered-spec" + _scalaVersion
        val scalacheck = "scalacheck" + _scalaVersion
        Seq(
          // scope: compile
          "commons-dbcp"            %  "commons-dbcp"         % "1.4"      % "compile",
          "org.slf4j"               %  "slf4j-api"            % "1.6.4"    % "compile",
          "joda-time"               %  "joda-time"            % "2.1"      % "compile",
          "org.joda"                %  "joda-convert"         % "1.2"      % "compile",
          // scope: test
          "org.scala-tools.time"    %  time                   % "0.5"       % "test",
          "ch.qos.logback"          %  "logback-classic"      % "1.0.2"     % "test",
          "net.databinder"          %  unfilteredFilter       % "0.6.1"     % "test",
          "net.databinder"          %  unfilteredJetty        % "0.6.1"     % "test",
          "net.databinder"          %  unfilteredSpec         % "0.6.1"     % "test",
          "org.scalatest"           %% "scalatest"            % "1.7.2"     % "test",
          "org.scala-tools.testing" %  scalacheck             % "1.9"       % "test",
          "play"                    %  "anorm_2.9.1"          % "[2,)"      % "test"
        ) ++ jdbcDriverDependenciesInTestScope
      },
      sbtPlugin := false,
      scalacOptions ++= _scalacOptions,
      publishMavenStyle := true,
      publishArtifact in Test := false,
      pomIncludeRepository := { x => false },
      pomExtra := _pomExtra
    )
  )

  lazy val scalikejdbcMapperGenerator = Project(
    id = "mapper-generator", 
    base = file("scalikejdbc-mapper-generator"), 
    settings = Defaults.defaultSettings ++ Seq(
      sbtPlugin := true,
      organization := _organization,
      name := "scalikejdbc-mapper-generator",
      version := _version,
      scalaVersion := "2.9.1",
      crossScalaVersions := Seq("2.9.2", "2.9.1"),
      resolvers ++= _resolvers,
      libraryDependencies ++= Seq(
        _organization %% "scalikejdbc" % _version,
        "ch.qos.logback" % "logback-classic" % "1.0.2",
        "org.scalatest" %% "scalatest" % "[1.7,)" % "test"
      ) ++ jdbcDriverDependenciesInTestScope,
      publishTo <<= version { (v: String) => _publishTo(v) },
      publishMavenStyle := true,
      publishArtifact in Test := false,
      pomIncludeRepository := { x => false },
      pomExtra := _pomExtra,
      scalacOptions ++= _scalacOptions
    )
  )

  lazy val scalikejdbcPlayPlugin = Project(
    id = "play-plugin", 
    base = file("scalikejdbc-play-plugin"), 
    settings = Defaults.defaultSettings ++ Seq(
      sbtPlugin := false,
      organization := _organization,
      name := "scalikejdbc-play-plugin",
      version := _version,
      scalaVersion := "2.9.1",
      crossScalaVersions := Seq("2.9.2", "2.9.1"),
      resolvers ++= _resolvers,
      libraryDependencies <++= (scalaVersion) { scalaVersion =>
        Seq(
          _organization %% "scalikejdbc" % _version,
          "play" % "play_2.9.1" % "2.0.2" % "provided",
          "play" % "play-test_2.9.1" % "2.0.2" % "test"
        )
      },
      publishTo <<= version { (v: String) => _publishTo(v) },
      publishMavenStyle := true,
      publishArtifact in Test := false,
      pomIncludeRepository := { x => false },
      pomExtra := _pomExtra,
      scalacOptions ++= _scalacOptions
    )
  )

  val _crossScalaVersions = Seq("2.9.2", "2.9.1", "2.9.0")
  def _publishTo(v: String) = {
    val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")  
    else Some("releases" at nexus + "service/local/staging/deploy/maven2")
  }
  val _resolvers = Seq(
    "typesafe releases" at "http://repo.typesafe.com/typesafe/releases",
    "sonatype releases" at "http://oss.sonatype.org/content/repositories/releases/"
  )
  val jdbcDriverDependenciesInTestScope = Seq(
    "com.h2database"    % "h2"                   % "[1.3,)"        % "test",
    "org.apache.derby"  % "derby"                % "[10.8.2,)"     % "test",
    "org.xerial"        % "sqlite-jdbc"          % "3.6.16"        % "test",
    "org.hsqldb"        % "hsqldb"               % "2.2.8"         % "test",
    "mysql"             % "mysql-connector-java" % "[5.1,)"        % "test",
    "postgresql"        % "postgresql"           % "9.1-901.jdbc4" % "test"
  )
  val _scalacOptions = Seq("-deprecation", "-unchecked")
  val _pomExtra = <url>http://seratch.github.com/scalikejdbc</url>
      <licenses>
        <license>
          <name>Apache License, Version 2.0</name>
          <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:seratch/scalikejdbc.git</url>
        <connection>scm:git:git@github.com:seratch/scalikejdbc.git</connection>
      </scm>
      <developers>
        <developer>
          <id>seratch</id>
          <name>Kazuhuiro Sera</name>
          <url>http://seratch.net/</url>
        </developer>
      </developers>
}


