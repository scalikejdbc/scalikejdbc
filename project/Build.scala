import sbt._
import Keys._

import play.Project._

object ScalikeJDBCProjects extends Build {

  lazy val _organization = "org.scalikejdbc"

  // [NOTE] Execute the following to bump version
  // sbt "g version 1.3.8-SNAPSHOT"
  lazy val _version = "1.7.1-SNAPSHOT"

  // published dependency version
  lazy val _slf4jApiVersion = "1.7.5"
  lazy val _defaultPlayVersion = "2.2.1"
  lazy val _typesafeConfigVersion = "1.0.2"

  // internal only
  lazy val _logbackVersion = "1.0.13"
  lazy val _h2Version = "1.3.173"
  lazy val _hibernateVersion = "4.1.12.Final"
  lazy val _scalatestVersion = "1.9.1"
  lazy val _specs2Scala291Version = "1.12.4"
  lazy val _specs2Scala29Version = "1.12.4.1"
  lazy val _specs2Scala210Version = "2.2"

  // scalikejdbc (core library)
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
        val anorm = "anorm_" + (scalaVersion match {
          case "2.10.2" | "2.10.1" | "2.10.0" => "2.10"
          case _ => "2.9.1"
        })
        val anormDependency = scalaVersion match {
          case "2.10.2" | "2.10.1" | "2.10.0" => "com.typesafe.play" % anorm % _defaultPlayVersion % "test"
          case _ =>                              "play"              % anorm % "2.0.8" % "test"
        }
        Seq(
          // scope: compile
          "commons-dbcp"            %  "commons-dbcp"    % "1.4"            % "compile",
          "org.slf4j"               %  "slf4j-api"       % _slf4jApiVersion % "compile",
          "joda-time"               %  "joda-time"       % "2.3"            % "compile",
          "org.joda"                %  "joda-convert"    % "1.4"            % "compile",
          // scope: test
          "ch.qos.logback"          %  "logback-classic" % _logbackVersion   % "test",
          "org.hibernate"           %  "hibernate-core"  % _hibernateVersion % "test",
          "org.scalatest"           %% "scalatest"       % _scalatestVersion % "test",
          "org.mockito"             %  "mockito-all"     % "1.9.5"           % "test",
          anormDependency
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

  // scalikejdbc-interpolation-core
  // basic modules that are used by interpolation-macro
  lazy val scalikejdbcInterpolationCore = Project(
    id = "interpolation-core",
    base = file("scalikejdbc-interpolation-core"),
    settings = Defaults.defaultSettings ++ Seq(
      sbtPlugin := false,
      organization := _organization,
      name := "scalikejdbc-interpolation-core",
      version := _version,
      scalaVersion := "2.10.0",
      scalaBinaryVersion := "2.10",
      resolvers ++= _resolvers,
      libraryDependencies <++= (scalaVersion) { scalaVersion =>
        Seq(
          "org.slf4j"      %  "slf4j-api"        % _slf4jApiVersion  % "compile",
          "ch.qos.logback" %  "logback-classic"  % _logbackVersion   % "test",
          "org.scalatest"  %% "scalatest"        % _scalatestVersion % "test"
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

  // scalikejdbc-interpolation-macro
  lazy val scalikejdbcInterpolationMacro = Project(
    id = "interpolation-macro",
    base = file("scalikejdbc-interpolation-macro"),
    settings = Defaults.defaultSettings ++ Seq(
      sbtPlugin := false,
      organization := _organization,
      name := "scalikejdbc-interpolation-macro",
      version := _version,
      scalaVersion := "2.10.0",
      scalaBinaryVersion := "2.10",
      resolvers ++= _resolvers,
      libraryDependencies <++= (scalaVersion) { scalaVersion =>
        Seq(
          "org.scala-lang" %  "scala-reflect"    % scalaVersion      % "compile",
          "org.scala-lang" %  "scala-compiler"   % scalaVersion      % "optional",
          "org.scalatest"  %% "scalatest"        % _scalatestVersion % "test"
        )
      },
      publishTo <<= version { (v: String) => _publishTo(v) },
      publishMavenStyle := true,
      publishArtifact in Test := false,
      pomIncludeRepository := { x => false },
      pomExtra := _pomExtra,
      scalacOptions ++= _scalacOptions
    )
  ) dependsOn(scalikejdbcInterpolationCore)

  // scalikejdbc-interpolation
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
      resolvers ++= _resolvers,
      libraryDependencies <++= (scalaVersion) { scalaVersion =>
        Seq(
          "org.slf4j"      %  "slf4j-api"        % _slf4jApiVersion  % "compile",
          "ch.qos.logback" %  "logback-classic"  % _logbackVersion   % "test",
          "org.hibernate"  %  "hibernate-core"   % _hibernateVersion % "test",
          "org.scalatest"  %% "scalatest"        % _scalatestVersion % "test"
        ) ++ jdbcDriverDependenciesInTestScope
      },
      publishTo <<= version { (v: String) => _publishTo(v) },
      publishMavenStyle := true,
      publishArtifact in Test := false,
      pomIncludeRepository := { x => false },
      pomExtra := _pomExtra,
      scalacOptions ++= _scalacOptions
    )
  ) dependsOn(scalikejdbc, scalikejdbcInterpolationCore, scalikejdbcInterpolationMacro)

  // scalikejdbc-mapper-generator-core
  // core library for mapper-generator
  lazy val scalikejdbcMapperGeneratorCore = Project(
    id = "mapper-generator-core",
    base = file("scalikejdbc-mapper-generator-core"),
    settings = Defaults.defaultSettings ++ Seq(
      sbtPlugin := false,
      organization := _organization,
      name := "scalikejdbc-mapper-generator-core",
      version := _version,
      resolvers ++= _resolvers,
      libraryDependencies <++= (scalaVersion) { scalaVersion =>
        (scalaVersion match {
          case "2.10.2" | "2.10.1" | "2.10.0" => Seq(
            "org.slf4j"     %  "slf4j-api" % _slf4jApiVersion       % "compile",
            "org.scalatest" %% "scalatest" % _scalatestVersion      % "test",
            "org.specs2"    %% "specs2"    % _specs2Scala210Version % "test"
          )
          case "2.9.1" => Seq(
            "org.slf4j"     %  "slf4j-api" % _slf4jApiVersion      % "compile",
            "org.scalatest" %% "scalatest" % _scalatestVersion     % "test",
            "org.specs2"    %% "specs2"    % _specs2Scala291Version % "test"
          )
          case _ => Seq(
            "org.slf4j"     %  "slf4j-api" % _slf4jApiVersion      % "compile",
            "org.scalatest" %% "scalatest" % _scalatestVersion     % "test",
            "org.specs2"    %% "specs2"    % _specs2Scala29Version % "test"
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

  // mapper-generator sbt plugin
  lazy val scalikejdbcMapperGenerator = Project(
    id = "mapper-generator",
    base = file("scalikejdbc-mapper-generator"),
    settings = Defaults.defaultSettings ++ Seq(
      sbtPlugin := true,
      organization := _organization,
      name := "scalikejdbc-mapper-generator",
      version := _version,
      resolvers ++= _resolvers,
      libraryDependencies <++= (scalaVersion) { scalaVersion =>
        // sbt 0.12.x uses Scala 2.9.2
        Seq(
          "org.slf4j"     %  "slf4j-simple" % _slf4jApiVersion      % "compile",
          "org.scalatest" %% "scalatest"    % _scalatestVersion     % "test",
          (scalaVersion match {
            case "2.10.2" => "org.specs2" %% "specs2" % _specs2Scala210Version % "test"
            case "2.9.2"  => "org.specs2" %% "specs2" % _specs2Scala29Version % "test"
          })
        ) ++ jdbcDriverDependenciesInTestScope
      },
      publishTo <<= version { (v: String) => _publishTo(v) },
      publishMavenStyle := true,
      publishArtifact in Test := false,
      pomIncludeRepository := { x => false },
      pomExtra := _pomExtra,
      scalacOptions ++= _scalacOptions
    )
  ) dependsOn(scalikejdbc, scalikejdbcTest, scalikejdbcMapperGeneratorCore)

  // scalikejdbc-play-plugin
  // support: Play 2.0.x, 2.1.x
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
        (scalaVersion match {
          case "2.10.2" | "2.10.1" | "2.10.0" => {
            Seq(
              "com.typesafe.play" % "play_2.10"      % _defaultPlayVersion % "provided",
              "com.typesafe.play" % "play-test_2.10" % _defaultPlayVersion % "test",
              "com.h2database"    % "h2"             % _h2Version          % "test"
            )
          }
          case _ => {
            val play20Version = "2.0.8"
            Seq(
              "play" % "play_2.9.1"      % play20Version % "provided",
              "play" % "play-test_2.9.1" % play20Version % "test"
            )
          }
        })
      },
      testOptions in Test += Tests.Argument(TestFrameworks.Specs2, "sequential", "true"),
      publishTo <<= version { (v: String) => _publishTo(v) },
      publishMavenStyle := true,
      publishArtifact in Test := false,
      pomIncludeRepository := { x => false },
      pomExtra := _pomExtra,
      scalacOptions ++= _scalacOptions
    )
  ) dependsOn(scalikejdbc)

  // scalikejdbc-play-fixture-plugin
  // support: Play 2.1.x, 2.2.x
  lazy val scalikejdbcPlayFixturePlugin = Project(
    id = "play-fixture-plugin",
    base = file("scalikejdbc-play-fixture-plugin"),
    settings = Defaults.defaultSettings ++ Seq(
      sbtPlugin := false,
      organization := _organization,
      name := "scalikejdbc-play-fixture-plugin",
      version := _version,
      crossScalaVersions := Seq("2.10.0"),
      resolvers ++= _resolvers,
      libraryDependencies ++= Seq(
        "com.typesafe.play" %% "play"      % _defaultPlayVersion % "provided",
        "com.typesafe.play" %% "play-test" % _defaultPlayVersion % "test",
        "com.h2database"    %  "h2"        % _h2Version          % "test"
      ),
      testOptions in Test += Tests.Argument(TestFrameworks.Specs2, "sequential", "true"),
      publishTo <<= version { (v: String) => _publishTo(v) },
      publishMavenStyle := true,
      publishArtifact in Test := false,
      pomIncludeRepository := { x => false },
      pomExtra := _pomExtra,
      scalacOptions ++= _scalacOptions
    )
  ).dependsOn(
    scalikejdbcPlayPlugin
  ).aggregate(
    scalikejdbcPlayPlugin
  )

  // play zentasks example
  lazy val scalikejdbcPlayPluginTestZentasks = {
    val appName         = "play-plugin-test-zentasks"
    val appVersion      = "1.0"

    val appDependencies = Seq(
      "com.github.tototoshi" %% "play-flyway" % "0.2.0",
      "com.h2database"       %  "h2"          % _h2Version,
      "org.postgresql"       %  "postgresql"  % "9.2-1003-jdbc4"
    )

    play.Project(appName, appVersion, appDependencies, path = file("scalikejdbc-play-plugin/test/zentasks")).settings(
      scalaVersion in ThisBuild := "2.10.2",
      resolvers ++= Seq(
        "sonatype releases"  at "http://oss.sonatype.org/content/repositories/releases",
        "sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots"
      )
    ).dependsOn(
      scalikejdbcPlayFixturePlugin,
      scalikejdbcInterpolation
     ).aggregate(
      scalikejdbcPlayPlugin,
      scalikejdbcPlayFixturePlugin,
      scalikejdbcInterpolation
    )
  }

  // scalikejdbc-test
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
        (scalaVersion match {
          case "2.10.2" | "2.10.1" | "2.10.0" => Seq(
            "org.slf4j"      %  "slf4j-api"       % _slf4jApiVersion       % "compile",
            "ch.qos.logback" %  "logback-classic" % _logbackVersion        % "test",
            "org.scalatest"  %% "scalatest"       % _scalatestVersion      % "provided",
            "org.specs2"     %% "specs2"          % _specs2Scala210Version % "provided"
          )
          case "2.9.1" => Seq(
            "org.slf4j"      %  "slf4j-api"       % _slf4jApiVersion       % "compile",
            "ch.qos.logback" %  "logback-classic" % _logbackVersion        % "test",
            "org.scalatest"  %% "scalatest"       % _scalatestVersion      % "provided",
            "org.specs2"     %% "specs2"          % _specs2Scala291Version % "provided"
          )
          case _ => Seq(
            "org.slf4j"      %  "slf4j-api"       % _slf4jApiVersion      % "compile",
            "ch.qos.logback" %  "logback-classic" % _logbackVersion       % "test",
            "org.scalatest"  %% "scalatest"       % _scalatestVersion     % "provided",
            "org.specs2"     %% "specs2"          % _specs2Scala29Version % "provided"
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

  // scalikejdbc-config
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
        Seq(
          "com.typesafe"   %  "config"          % _typesafeConfigVersion % "compile",
          "org.slf4j"      %  "slf4j-api"       % _slf4jApiVersion       % "compile",
          "org.scalatest"  %% "scalatest"       % _scalatestVersion      % "provided",
          "ch.qos.logback" %  "logback-classic" % _logbackVersion        % "test"
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
    "com.h2database"    % "h2"                   % _h2Version       % "test",
    "org.apache.derby"  % "derby"                % "10.10.1.1"      % "test",
    "org.xerial"        % "sqlite-jdbc"          % "3.7.2"          % "test",
    "org.hsqldb"        % "hsqldb"               % "2.3.0"          % "test",
    "mysql"             % "mysql-connector-java" % "5.1.26"         % "test",
    "org.postgresql"    % "postgresql"           % "9.2-1003-jdbc4" % "test"
  )
  //val _scalacOptions = Seq("-deprecation", "-unchecked", "-Ymacro-debug-lite", "-Xlog-free-terms", "Yshow-trees", "-feature")
  val _scalacOptions = Seq("-deprecation", "-unchecked")
  val _pomExtra = <url>http://scalikejdbc.org/</url>
      <licenses>
        <license>
          <name>Apache License, Version 2.0</name>
          <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:scalikejdbc/scalikejdbc.git</url>
        <connection>scm:git:git@github.com:scalikejdbc/scalikejdbc.git</connection>
      </scm>
      <developers>
        <developer>
          <id>seratch</id>
          <name>Kazuhuiro Sera</name>
          <url>http://git.io/sera</url>
        </developer>
      </developers>
}

