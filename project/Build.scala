import sbt._
import Keys._

object ScalikeJDBCProjects extends Build {

  lazy val _organization = "com.github.seratch"

  // [NOTE] Execute the following to bump version
  // sbt "g version 1.3.8-SNAPSHOT"
  lazy val _version = "1.4.7"

  lazy val scalikejdbc = Project(
    id = "library",
    base = file("scalikejdbc-library"),
    settings = Defaults.defaultSettings ++ Seq(
      organization := _organization,
      name := "scalikejdbc",
      version := _version,
      crossScalaVersions := _crossScalaVersions,
      publishTo <<= version { (v: String) => _publishTo(v) },
      publishMavenStyle := true,
      resolvers ++= _resolvers,
      libraryDependencies <++= (scalaVersion) { scalaVersion =>
        val _scalaVersion = "_" + (scalaVersion match {
          case "2.10.0" => "2.10.0"
          case "2.9.3" => "2.9.2"
          case version => version
        })
        val scalatest = "scalatest" + _scalaVersion
        val anorm = "anorm" + _scalaVersion
        Seq(
          // scope: compile
          "commons-dbcp"            %  "commons-dbcp"         % "1.4"         % "compile",
          "org.slf4j"               %  "slf4j-api"            % "1.7.2"       % "compile",
          "joda-time"               %  "joda-time"            % "2.1"         % "compile",
          "org.joda"                %  "joda-convert"         % "1.2"         % "compile",
          // scope: test
          "ch.qos.logback"          %  "logback-classic"      % "1.0.7"       % "test",
          "org.hibernate"           %  "hibernate-core"       % "4.1.9.Final" % "test",
          "org.scalatest"           %  scalatest              % "1.8"         % "test",
          "org.mockito"             %  "mockito-all"          % "1.9.5"       % "test",
          "play"                    %  anorm                  % "[2,)"        % "test"
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

  lazy val scalikejdbcInterpolation = Project(
    id = "interpolation",
    base = file("scalikejdbc-interpolation"),
    settings = Defaults.defaultSettings ++ Seq(
      sbtPlugin := false,
      organization := _organization,
      name := "scalikejdbc-interpolation",
      version := _version,
      scalaVersion := "2.10.0",
      scalaBinaryVersion := "2.10",
      crossScalaVersions := Seq("2.10.0"),
      resolvers ++= _resolvers,
      libraryDependencies <++= (scalaVersion) { scalaVersion =>
        Seq(
          "org.slf4j"      %  "slf4j-api"        % "1.7.2"       % "compile",
          "ch.qos.logback" %  "logback-classic"  % "1.0.7"       % "test",
          "org.hibernate"  %  "hibernate-core"   % "4.1.9.Final" % "test",
          "org.scalatest"  %% "scalatest"        % "[1.8,)"      % "test"
        ) ++ jdbcDriverDependenciesInTestScope
      },
      publishTo <<= version { (v: String) => _publishTo(v) },
      publishMavenStyle := true,
      publishArtifact in Test := false,
      pomIncludeRepository := { x => false },
      pomExtra := _pomExtra,
      scalacOptions ++= _scalacOptions
    )
  ) dependsOn(scalikejdbc)

  lazy val scalikejdbcMapperGenerator = Project(
    id = "mapper-generator",
    base = file("scalikejdbc-mapper-generator"),
    settings = Defaults.defaultSettings ++ Seq(
      sbtPlugin := true,
      organization := _organization,
      name := "scalikejdbc-mapper-generator",
      version := _version,
      scalaBinaryVersion <<= scalaVersion,
      resolvers ++= _resolvers,
      libraryDependencies <++= (scalaVersion) { scalaVersion =>
        (scalaVersion match {
          case "2.10.0" => Seq(
            "org.slf4j"     %  "slf4j-simple" % "1.7.2"   % "compile",
            "org.scalatest" %% "scalatest"    % "[1.8,)"  % "test",
            "org.specs2"    %% "specs2"       % "[1.13,)" % "test"
           )
          case "2.9.3" => Seq(
            "org.slf4j"     %  "slf4j-simple"    % "1.7.2"   % "compile",
            "org.scalatest" %  "scalatest_2.9.2" % "[1.8,)"  % "test",
            "org.specs2"    %% "specs2_2.9.2"    % "[1.12,)" % "test"
           )
          case _ => Seq(
            "org.slf4j"     %  "slf4j-simple" % "1.7.2"   % "compile",
            "org.scalatest" %% "scalatest"    % "[1.8,)"  % "test",
            "org.specs2"    %% "specs2"       % "[1.12,)" % "test"
           )
        }) ++ jdbcDriverDependenciesInTestScope
      },
      publishTo <<= version { (v: String) => _publishTo(v) },
      publishMavenStyle := true,
      publishArtifact in Test := false,
      pomIncludeRepository := { x => false },
      pomExtra := _pomExtra,
      scalacOptions ++= _scalacOptions
    )
  ) dependsOn(scalikejdbc, scalikejdbcTest)

  lazy val scalikejdbcPlayPlugin = Project(
    id = "play-plugin",
    base = file("scalikejdbc-play-plugin"),
    settings = Defaults.defaultSettings ++ Seq(
      sbtPlugin := false,
      organization := _organization,
      name := "scalikejdbc-play-plugin",
      version := _version,
      crossScalaVersions := Seq("2.10.0", "2.9.1"),
      resolvers ++= _resolvers,
      libraryDependencies <++= (scalaVersion) { scalaVersion =>
        scalaVersion match {
          case "2.10.0" => {
            val playVersion = "2.1.0"
            Seq(
              "play" % "play_2.10" % playVersion % "provided",
              "play" % "play-test_2.10" % playVersion % "test",
              "com.h2database" % "h2" % "[1.3,)" % "test"
            )
          }
          case _ => {
            val playVersion = "2.0.4"
            Seq(
              "play" % "play_2.9.1" % playVersion % "provided",
              "play" % "play-test_2.9.1" % playVersion % "test"
            )
          }
        }
      },
      publishTo <<= version { (v: String) => _publishTo(v) },
      publishMavenStyle := true,
      publishArtifact in Test := false,
      pomIncludeRepository := { x => false },
      pomExtra := _pomExtra,
      scalacOptions ++= _scalacOptions
    )
  ) dependsOn(scalikejdbc)

  lazy val scalikejdbcTest = Project(
    id = "test",
    base = file("scalikejdbc-test"),
    settings = Defaults.defaultSettings ++ Seq(
      sbtPlugin := false,
      organization := _organization,
      name := "scalikejdbc-test",
      version := _version,
      crossScalaVersions := _crossScalaVersions,
      resolvers ++= _resolvers,
      libraryDependencies <++= (scalaVersion) { scalaVersion =>
        Seq(
          "org.slf4j"      %  "slf4j-api"            % "1.7.2"   % "compile",
          "org.scalatest"  %% "scalatest"            % "[1.8,)"  % "provided",
          "ch.qos.logback" %  "logback-classic"      % "1.0.7"   % "test"
        ) ++ (scalaVersion match {
          case "2.10.0" => Seq("org.specs2" %% "specs2"       % "[1.13,)" % "provided")
          case "2.9.3"  => Seq("org.specs2" %  "specs2_2.9.2" % "[1.12,)" % "provided")
          case _ => Seq("org.specs2" %% "specs2" % "[1.12,)"  % "provided")
        }) ++ jdbcDriverDependenciesInTestScope
      },
      publishTo <<= version { (v: String) => _publishTo(v) },
      publishMavenStyle := true,
      publishArtifact in Test := false,
      pomIncludeRepository := { x => false },
      pomExtra := _pomExtra,
      scalacOptions ++= _scalacOptions
    )
  ) dependsOn(scalikejdbc)


  lazy val scalikejdbcConfig = Project(
    id = "config",
    base = file("scalikejdbc-config"),
    settings = Defaults.defaultSettings ++ Seq(
      sbtPlugin := false,
      organization := _organization,
      name := "scalikejdbc-config",
      version := _version,
      crossScalaVersions := _crossScalaVersions,
      resolvers ++= _resolvers,
      libraryDependencies <++= (scalaVersion) { scalaVersion =>
        (scalaVersion match {
          case "2.9.3" => Seq(
            "com.typesafe"   %  "config"               % "1.0.0"   % "compile",
            "org.slf4j"      %  "slf4j-api"            % "1.7.2"   % "compile",
            "org.scalatest"  %  "scalatest_2.9.2"      % "[1.8,)"  % "provided",
            "ch.qos.logback" %  "logback-classic"      % "1.0.7"   % "test"
           )
          case _ => Seq(
            "com.typesafe"   %  "config"               % "1.0.0"   % "compile",
            "org.slf4j"      %  "slf4j-api"            % "1.7.2"   % "compile",
            "org.scalatest"  %% "scalatest"            % "[1.8,)"  % "provided",
            "ch.qos.logback" %  "logback-classic"      % "1.0.7"   % "test"
           )
        }) ++ jdbcDriverDependenciesInTestScope
      },
      publishTo <<= version { (v: String) => _publishTo(v) },
      publishMavenStyle := true,
      publishArtifact in Test := false,
      pomIncludeRepository := { x => false },
      pomExtra := _pomExtra,
      scalacOptions ++= _scalacOptions
    )
  ) dependsOn(scalikejdbc)

  val _crossScalaVersions = Seq("2.10.0", "2.9.3", "2.9.2", "2.9.1")
  def _publishTo(v: String) = {
    val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
    else Some("releases" at nexus + "service/local/staging/deploy/maven2")
  }
  val _resolvers = Seq(
    "typesafe repo" at "http://repo.typesafe.com/typesafe/repo",
    "sonatype releases" at "http://oss.sonatype.org/content/repositories/releases"
  )
  val jdbcDriverDependenciesInTestScope = Seq(
    "com.h2database"    % "h2"                   % "[1.3,)"        % "test",
    "org.apache.derby"  % "derby"                % "[10.8.2,)"     % "test",
    "org.xerial"        % "sqlite-jdbc"          % "3.7.2"         % "test",
    "org.hsqldb"        % "hsqldb"               % "2.2.9"         % "test",
    "mysql"             % "mysql-connector-java" % "5.1.23"        % "test",
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

