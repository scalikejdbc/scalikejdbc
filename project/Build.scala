import sbt._
import Keys._

object ScalikeJDBCProjects extends Build {

  // [NOTE] Execute the following to bump version
  // sbt "g version 1.3.8-SNAPSHOT"
  lazy val _version = "2.0.0-SNAPSHOT"

  // published dependency version
  lazy val _slf4jApiVersion = "1.7.6"
  lazy val _defaultPlayVersion = "2.2.2"
  lazy val _typesafeConfigVersion = "1.2.0"

  // internal only
  lazy val _logbackVersion = "1.1.1"
  lazy val _h2Version = "1.3.175"
  lazy val _hibernateVersion = "4.3.1.Final"
  lazy val _scalatestVersion = "2.1.0"
  lazy val _specs2Version = "2.3.9"

  lazy val baseSettings = Defaults.defaultSettings ++ Seq(
    organization := "org.scalikejdbc",
    version := _version,
    publishTo <<= version { (v: String) => _publishTo(v) },
    publishMavenStyle := true,
    resolvers ++= _resolvers,
    scalacOptions ++= _scalacOptions,
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { x => false },
    pomExtra := _pomExtra
  )

  lazy val root211 = Project(
    "root211",
    file("root211")
  ).settings(
    baseSettings: _*
  ).aggregate(
    scalikejdbc,
    scalikejdbcConfig,
    scalikejdbcInterpolation,
    scalikejdbcMapperGeneratorCore,
    scalikejdbcTest,
    scalikejdbcInterpolationCore,
    scalikejdbcInterpolationMacro
  )

  // scalikejdbc (core library)
  lazy val scalikejdbc = Project(
    id = "library",
    base = file("scalikejdbc-library"),
    settings = baseSettings ++ Seq(
      name := "scalikejdbc",
      libraryDependencies <++= (scalaVersion) { scalaVersion =>
        Seq(
          // scope: compile
          "commons-dbcp"            %  "commons-dbcp"    % "1.4"            % "compile",
          "org.slf4j"               %  "slf4j-api"       % _slf4jApiVersion % "compile",
          "joda-time"               %  "joda-time"       % "2.3"            % "compile",
          "org.joda"                %  "joda-convert"    % "1.6"            % "compile",
          // scope: test
          "ch.qos.logback"          %  "logback-classic" % _logbackVersion   % "test",
          "org.hibernate"           %  "hibernate-core"  % _hibernateVersion % "test",
          "org.scalatest"           %% "scalatest"       % _scalatestVersion % "test",
          "org.mockito"             %  "mockito-all"     % "1.9.5"           % "test"
        ) ++ {
          scalaVersion match {
            case v if v.startsWith("2.11.") => Seq("org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.0" % "compile")
            case _ => Nil
          }
        } ++ jdbcDriverDependenciesInTestScope
      }
    )
  )

  // scalikejdbc-interpolation-core
  // basic modules that are used by interpolation-macro
  lazy val scalikejdbcInterpolationCore = Project(
    id = "interpolation-core",
    base = file("scalikejdbc-interpolation-core"),
    settings = baseSettings ++ Seq(
      name := "scalikejdbc-interpolation-core",
      libraryDependencies <++= (scalaVersion) { scalaVersion =>
        Seq(
          "org.slf4j"      %  "slf4j-api"        % _slf4jApiVersion  % "compile",
          "ch.qos.logback" %  "logback-classic"  % _logbackVersion   % "test",
          "org.scalatest"  %% "scalatest"        % _scalatestVersion % "test"
        ) ++ jdbcDriverDependenciesInTestScope
      }
    )
  ) dependsOn(scalikejdbc)

  // scalikejdbc-interpolation-macro
  lazy val scalikejdbcInterpolationMacro = Project(
    id = "interpolation-macro",
    base = file("scalikejdbc-interpolation-macro"),
    settings = baseSettings ++ Seq(
      name := "scalikejdbc-interpolation-macro",
      libraryDependencies <++= (scalaVersion) { scalaVersion =>
        Seq(
          "org.scala-lang" %  "scala-reflect"    % scalaVersion      % "compile",
          "org.scala-lang" %  "scala-compiler"   % scalaVersion      % "optional",
          "org.scalatest"  %% "scalatest"        % _scalatestVersion % "test"
        )
      }
    )
  ) dependsOn(scalikejdbcInterpolationCore)

  // scalikejdbc-interpolation
  lazy val scalikejdbcInterpolation = Project(
    id = "interpolation",
    base = file("scalikejdbc-interpolation"),
    settings = baseSettings ++ Seq(
      name := "scalikejdbc-interpolation",
      libraryDependencies <++= (scalaVersion) { scalaVersion =>
        Seq(
          "org.slf4j"      %  "slf4j-api"        % _slf4jApiVersion  % "compile",
          "ch.qos.logback" %  "logback-classic"  % _logbackVersion   % "test",
          "org.hibernate"  %  "hibernate-core"   % _hibernateVersion % "test",
          "org.scalatest"  %% "scalatest"        % _scalatestVersion % "test"
        ) ++ jdbcDriverDependenciesInTestScope
      }
    )
  ) dependsOn(scalikejdbc, scalikejdbcInterpolationCore, scalikejdbcInterpolationMacro)

  // scalikejdbc-mapper-generator-core
  // core library for mapper-generator
  lazy val scalikejdbcMapperGeneratorCore = Project(
    id = "mapper-generator-core",
    base = file("scalikejdbc-mapper-generator-core"),
    settings = baseSettings ++ Seq(
      name := "scalikejdbc-mapper-generator-core",
      libraryDependencies <++= (scalaVersion) { scalaVersion =>
        Seq(
          "org.slf4j"     %  "slf4j-api" % _slf4jApiVersion   % "compile",
          "org.scalatest" %% "scalatest" % _scalatestVersion  % "test",
          "org.specs2"    %% "specs2"    % _specs2Version     % "test"
        ) ++ jdbcDriverDependenciesInTestScope
      }
    )
  ) dependsOn(scalikejdbc, scalikejdbcTest)

  // mapper-generator sbt plugin
  lazy val scalikejdbcMapperGenerator = Project(
    id = "mapper-generator",
    base = file("scalikejdbc-mapper-generator"),
    settings = baseSettings ++ Seq(
      sbtPlugin := true,
      name := "scalikejdbc-mapper-generator",
      libraryDependencies <++= (scalaVersion) { scalaVersion =>
        Seq(
          "org.slf4j"     %  "slf4j-simple" % _slf4jApiVersion  % "compile",
          "org.scalatest" %% "scalatest"    % _scalatestVersion % "test",
          "org.specs2"    %% "specs2"       % _specs2Version    % "test"
        ) ++ jdbcDriverDependenciesInTestScope
      }
    )
  ) dependsOn(scalikejdbc, scalikejdbcTest, scalikejdbcMapperGeneratorCore)

  // scalikejdbc-test
  lazy val scalikejdbcTest = Project(
    id = "test",
    base = file("scalikejdbc-test"),
    settings = baseSettings ++ Seq(
      name := "scalikejdbc-test",
      libraryDependencies <++= (scalaVersion) { scalaVersion =>
        Seq(
          "org.slf4j"      %  "slf4j-api"       % _slf4jApiVersion  % "compile",
          "ch.qos.logback" %  "logback-classic" % _logbackVersion   % "test",
          "org.scalatest"  %% "scalatest"       % _scalatestVersion % "provided",
          "org.specs2"     %% "specs2"          % _specs2Version    % "provided"
        ) ++ jdbcDriverDependenciesInTestScope
      }
    )
  ) dependsOn(scalikejdbc)

  // scalikejdbc-config
  lazy val scalikejdbcConfig = Project(
    id = "config",
    base = file("scalikejdbc-config"),
    settings = baseSettings ++ Seq(
      name := "scalikejdbc-config",
      libraryDependencies <++= (scalaVersion) { scalaVersion =>
        Seq(
          "com.typesafe"   %  "config"          % _typesafeConfigVersion % "compile",
          "org.slf4j"      %  "slf4j-api"       % _slf4jApiVersion       % "compile",
          "org.scalatest"  %% "scalatest"       % _scalatestVersion      % "provided",
          "ch.qos.logback" %  "logback-classic" % _logbackVersion        % "test"
        ) ++ jdbcDriverDependenciesInTestScope
      }
    )
  ) dependsOn(scalikejdbc)

  def _publishTo(v: String) = {
    val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
    else Some("releases" at nexus + "service/local/staging/deploy/maven2")
  }
  val _resolvers = Seq(
    "typesafe repo" at "http://repo.typesafe.com/typesafe/releases",
    "sonatype releases" at "http://oss.sonatype.org/content/repositories/releases",
    "sonatype snaphots" at "http://oss.sonatype.org/content/repositories/snapshots"
  )
  val jdbcDriverDependenciesInTestScope = Seq(
    "com.h2database"    % "h2"                   % _h2Version        % "test",
    "org.apache.derby"  % "derby"                % "10.10.1.1"       % "test",
    "org.xerial"        % "sqlite-jdbc"          % "3.7.15-M1"       % "test",
    "org.hsqldb"        % "hsqldb"               % "2.3.1"           % "test",
    "mysql"             % "mysql-connector-java" % "5.1.29"          % "test",
    "org.postgresql"    % "postgresql"           % "9.3-1100-jdbc41" % "test"
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

