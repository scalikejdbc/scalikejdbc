import MimaSettings.mimaSettings

publish / skip := true

def sbt2 = "2.0.0-RC12"
val Scala3: String = sys.props.getOrElse("scalikejdbc_scala_3_version", "3.3.7")
def Scala212 = "2.12.21"
def Scala213 = "2.13.18"

val scalaVersions = Seq(Scala212, Scala213, Scala3)

ThisBuild / version := "4.4.0-SNAPSHOT"
ThisBuild / publishTo := {
  val centralSnapshots =
    "https://central.sonatype.com/repository/maven-snapshots/"
  if (isSnapshot.value) Some("central-snapshots" at centralSnapshots)
  else localStaging.value
}

val isScala3 = Def.setting(
  CrossVersion.partialVersion(scalaVersion.value).exists(_._1 == 3)
)

val excludeTestsIfWindows = Set(
  "basic_test.accounts.AccountDatabaseSpec",
  // stripMargin test
  "scalikejdbc.SQLInterpolationStringSuite",
  // stripMargin test
  "scalikejdbc.interpolation.SQLSyntaxSpec",
  // ???
  "scalikejdbc.jodatime.JodaTypeBinderSpec",
  "somewhere.DatabasePublisherTckTest",
)

lazy val _organization = "org.scalikejdbc"

// published dependency version
lazy val _slf4jApiVersion = "2.0.17"
lazy val _typesafeConfigVersion = "1.4.6"
lazy val _reactiveStreamsVersion = "1.0.4"

// internal only
lazy val _logbackVersion = "1.5.24"
lazy val _h2Version = "1.4.199" // TODO: Upgrade to 2.x
lazy val _postgresqlVersion = "42.7.10"
lazy val _hibernateVersion = "7.3.1.Final"
def scalatestVersion = "3.2.20"
lazy val mockitoVersion = "4.11.0"
val specs2 = "org.specs2" %% "specs2-core" % "4.23.0" % "provided"

val mysqlConnectorJ =
  "com.mysql" % "mysql-connector-j" % "9.6.0" % Test exclude (
    "com.google.protobuf",
    "protobuf-java"
  )

def gitHash: String = try {
  sys.process.Process("git rev-parse HEAD").lineStream_!.head
} catch {
  case e: Exception =>
    println(e)
    "master"
}

lazy val baseSettings = Def.settings(
  organization := _organization,
  publishMavenStyle := true,
  // Note: if you are publishing an sbt plugin you will also need to configure sbtPluginPublishLegacyMavenStyle := false for that project. Context: sbt publishes plugins with file names that do not conform to the maven specification. Sonatype OSSRH didn't validate this, but Sonatype Central does: File name 'sbt-my-plugin-0.0.1.jar' is not valid. See also: sbt/sbt#3410
  sbtPluginPublishLegacyMavenStyle := false,
  // https://github.com/sbt/sbt/issues/2217
  fullResolvers ~= { _.filterNot(_.name == "jcenter") },
  Global / transitiveClassifiers := Seq(Artifact.SourceClassifier),
  exportJars := false,
  javacOptions ++= Seq(
    "-source",
    "1.8",
    "-target",
    "1.8",
    "-encoding",
    "UTF-8",
    "-Xlint:-options"
  ),
  doc / javacOptions := Seq("-source", "1.8"),
  Test / fork := true,
  Test / baseDirectory := (ThisBuild / baseDirectory).value,
  Test / testOptions ++= {
    if (scalaBinaryVersion.value == "3") {
      Seq(Tests.Exclude(Set("scalikejdbc.specs2.mutable.AutoRollbackSpec")))
    } else {
      Nil
    }
  },
  Test / testOptions ++= {
    if (scala.util.Properties.isWin) {
      Seq(Tests.Exclude(excludeTestsIfWindows))
    } else {
      Nil
    }
  },
  scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature"),
  scalacOptions ++= {
    scalaBinaryVersion.value match {
      case "2.12" =>
        Seq(
          "-Yno-adapted-args",
          "-Xfuture",
          "-language:higherKinds",
          "-Xsource:3",
        )
      case "2.13" =>
        Seq(
          "-Xsource:3-cross",
        )
      case _ =>
        Seq(
          "-Wconf:msg=Implicit parameters should be provided with a:error"
        )
    }
  },
  (Compile / doc / scalacOptions) ++= Seq(
    "-sourcepath",
    (LocalRootProject / baseDirectory).value.getAbsolutePath,
    "-doc-source-url",
    s"https://github.com/scalikejdbc/scalikejdbc/tree/${gitHash}€{FILE_PATH}.scala"
  ),
  publishMavenStyle := true,
  Test / publishArtifact := false,
  pomIncludeRepository := { x => false },
  Test / logBuffered := false,
  Test / parallelExecution := false,
  pomExtra := _pomExtra
)

lazy val scalikejdbcJodaTime = projectMatrix
  .defaultAxes(VirtualAxis.jvm)
  .withId("joda-time")
  .in(file("scalikejdbc-joda-time"))
  .jvmPlatform(scalaVersions)
  .settings(
    baseSettings,
    mimaSettings,
    name := "scalikejdbc-joda-time",
    libraryDependencies ++= scalaTestDependenciesInTestScope.value,
    libraryDependencies ++= Seq(
      "org.mockito" % "mockito-core" % mockitoVersion % "test",
      "joda-time" % "joda-time" % "2.14.1",
      "org.joda" % "joda-convert" % "3.0.1"
    ),
  )
  .dependsOn(
    scalikejdbcLibrary,
    scalikejdbcCore % "test->test",
    scalikejdbcInterpolation % "test->test"
  )

// scalikejdbc library
lazy val scalikejdbcLibrary = projectMatrix
  .defaultAxes(VirtualAxis.jvm)
  .withId("library")
  .in(file("scalikejdbc-library"))
  .jvmPlatform(scalaVersions)
  .settings(
    baseSettings,
    mimaSettings,
    name := "scalikejdbc",
    Compile / scalacOptions ++= {
      if (scalaBinaryVersion.value == "2.13") {
        Seq(
          "-Wconf:msg=package object inheritance is deprecated:warning", // TODO
        )
      } else {
        Nil
      }
    },
    libraryDependencies ++= scalaTestDependenciesInTestScope.value ++
      Seq("com.h2database" % "h2" % _h2Version % "test"),
  )
  .dependsOn(scalikejdbcCore, scalikejdbcInterpolation)

// scalikejdbc (core library)
lazy val scalikejdbcCore = projectMatrix
  .defaultAxes(VirtualAxis.jvm)
  .withId("core")
  .in(file("scalikejdbc-core"))
  .jvmPlatform(scalaVersions)
  .settings(
    baseSettings,
    mimaSettings,
    name := "scalikejdbc-core",
    buildInfoPackage := "scalikejdbc",
    buildInfoObject := "ScalikejdbcBuildInfo",
    buildInfoKeys := Seq[BuildInfoKey](version, scalaVersion),
    (Compile / sourceGenerators) += task {
      val dir = (Compile / sourceManaged).value
      val file = dir / "scalikejdbc" / "OneToXSQL.scala"
      IO.write(file, GenerateOneToXSQL.value)
      Seq(file)
    },
    (Compile / sourceGenerators) += task[Seq[File]] {
      val dir = (Compile / sourceManaged).value
      (3 to 21).map { n =>
        val file = dir / "scalikejdbc" / s"OneToManies${n}SQL.scala"
        IO.write(file, GenerateOneToManies(n))
        file
      }
    },
    libraryDependencies ++= {
      Seq(
        // scope: compile
        "org.apache.commons" % "commons-dbcp2" % "2.14.0" % "compile",
        "org.slf4j" % "slf4j-api" % _slf4jApiVersion % "compile",
        "org.scala-lang.modules" %% "scala-parser-combinators" % "2.4.0" % "compile",
        "org.scala-lang.modules" %% "scala-collection-compat" % "2.14.0",
        // scope: provided
        "commons-dbcp" % "commons-dbcp" % "1.4" % "provided",
        "com.jolbox" % "bonecp" % "0.8.0.RELEASE" % "provided",
        // scope: test
        "com.zaxxer" % "HikariCP" % "4.0.3" % "test",
        "ch.qos.logback" % "logback-classic" % _logbackVersion % "test",
        "org.hibernate" % "hibernate-core" % _hibernateVersion % "test",
        "org.mockito" % "mockito-core" % mockitoVersion % "test"
      ) ++ scalaTestDependenciesInTestScope.value ++ jdbcDriverDependenciesInTestScope
    },
  )
  .enablePlugins(BuildInfoPlugin)

// scalikejdbc-interpolation-macro
lazy val scalikejdbcInterpolationMacro = projectMatrix
  .defaultAxes(VirtualAxis.jvm)
  .withId("interpolation-macro")
  .in(file("scalikejdbc-interpolation-macro"))
  .jvmPlatform(scalaVersions)
  .settings(
    baseSettings,
    mimaSettings,
    name := "scalikejdbc-interpolation-macro",
    libraryDependencies ++= {
      if (isScala3.value) {
        Nil
      } else {
        Seq(
          "org.scala-lang" % "scala-reflect" % scalaVersion.value % "compile",
          "org.scala-lang" % "scala-compiler" % scalaVersion.value % "optional"
        )
      }
    },
    libraryDependencies ++= scalaTestDependenciesInTestScope.value,
  )
  .dependsOn(scalikejdbcCore)

// scalikejdbc-interpolation
lazy val scalikejdbcInterpolation = projectMatrix
  .defaultAxes(VirtualAxis.jvm)
  .withId("interpolation")
  .in(file("scalikejdbc-interpolation"))
  .jvmPlatform(scalaVersions)
  .settings(
    baseSettings,
    mimaSettings,
    name := "scalikejdbc-interpolation",
    libraryDependencies ++= {
      Seq(
        "org.slf4j" % "slf4j-api" % _slf4jApiVersion % "compile",
        "ch.qos.logback" % "logback-classic" % _logbackVersion % "test",
        "org.hibernate" % "hibernate-core" % _hibernateVersion % "test"
      ) ++ scalaTestDependenciesInTestScope.value ++ jdbcDriverDependenciesInTestScope
    },
  )
  .dependsOn(scalikejdbcCore, scalikejdbcInterpolationMacro)

// scalikejdbc-orm
lazy val scalikejdbcOrm = projectMatrix
  .defaultAxes(VirtualAxis.jvm)
  .withId("orm")
  .in(file("scalikejdbc-orm"))
  .jvmPlatform(scalaVersions)
  .settings(
    baseSettings,
    mimaSettings,
    name := "scalikejdbc-orm",
    libraryDependencies ++= {
      Seq(
        "org.slf4j" % "slf4j-api" % _slf4jApiVersion % "compile",
        "ch.qos.logback" % "logback-classic" % _logbackVersion % "test",
        "org.hibernate" % "hibernate-core" % _hibernateVersion % "test"
      ) ++ scalaTestDependenciesInTestScope.value ++ jdbcDriverDependenciesInTestScope
    },
  )
  .dependsOn(
    scalikejdbcCore,
    scalikejdbcInterpolation,
    scalikejdbcInterpolationMacro,
    scalikejdbcSyntaxSupportMacro,
    scalikejdbcConfig,
    scalikejdbcJodaTime,
    scalikejdbcTest % "test"
  )

// scalikejdbc-mapper-generator-core
// core library for mapper-generator
lazy val scalikejdbcMapperGeneratorCore = projectMatrix
  .defaultAxes(VirtualAxis.jvm)
  .withId("mapper-generator-core")
  .in(file("scalikejdbc-mapper-generator-core"))
  .jvmPlatform(scalaVersions)
  .settings(
    baseSettings,
    mimaSettings,
    name := "scalikejdbc-mapper-generator-core",
    libraryDependencies ++= {
      Seq("org.slf4j" % "slf4j-api" % _slf4jApiVersion % "compile") ++
        scalaTestDependenciesInTestScope.value ++
        jdbcDriverDependenciesInTestScope
    },
  )
  .dependsOn(scalikejdbcLibrary)

// mapper-generator sbt plugin
lazy val scalikejdbcMapperGenerator = projectMatrix
  .defaultAxes(VirtualAxis.jvm)
  .withId("mapper-generator")
  .in(file("scalikejdbc-mapper-generator"))
  .jvmPlatform(
    if (scala.util.Properties.isJavaAtLeast("17")) {
      Seq(
        Scala212,
        scala_version_from_sbt_version.ScalaVersionFromSbtVersion(sbt2)
      )
    } else {
      Seq(Scala212)
    }
  )
  .settings(
    baseSettings,
    scriptedBufferLog := false,
    pluginCrossBuild / sbtVersion := {
      scalaBinaryVersion.value match {
        case "2.12" =>
          sbtVersion.value
        case _ =>
          sbt2
      }
    },
    scriptedLaunchOpts ++= {
      val javaVmArgs = {
        import scala.collection.JavaConverters._
        java.lang.management.ManagementFactory.getRuntimeMXBean.getInputArguments.asScala.toList
      }
      javaVmArgs.filter(a => Seq("-XX", "-Xss").exists(a.startsWith)) ++ Seq(
        "-Xmx3G"
      )
    },
    scriptedLaunchOpts ++= Seq(
      "-Dplugin.version=" + version.value,
      "-Dslf4j.version=" + _slf4jApiVersion,
      "-Dmysql.version=" + mysqlConnectorJ.revision,
      "-Dpostgresql.version=" + _postgresqlVersion,
      "-Dh2.version=" + _h2Version,
      "-Dspecs2.version=" + specs2.revision,
      "-Dscalatest.version=" + scalatestVersion
    ),
    name := "scalikejdbc-mapper-generator",
    libraryDependencies ++= {
      Seq("org.slf4j" % "slf4j-simple" % _slf4jApiVersion % "compile") ++
        scalaTestDependenciesInTestScope.value ++
        jdbcDriverDependenciesInTestScope
    },
  )
  .configure(p =>
    p.id match {
      case "mapper-generator2_12" =>
        p.dependsOn(scalikejdbcMapperGeneratorCore.jvm(Scala212))
      case "mapper-generator3" =>
        p.dependsOn(scalikejdbcMapperGeneratorCore.jvm(Scala3))
    }
  )
  .enablePlugins(SbtPlugin)

// scalikejdbc-test
lazy val scalikejdbcTest = projectMatrix
  .defaultAxes(VirtualAxis.jvm)
  .withId("test")
  .in(file("scalikejdbc-test"))
  .jvmPlatform(scalaVersions)
  .settings(
    baseSettings,
    mimaSettings,
    name := "scalikejdbc-test",
    libraryDependencies ++= {
      Seq(
        "org.slf4j" % "slf4j-api" % _slf4jApiVersion % "compile",
        "ch.qos.logback" % "logback-classic" % _logbackVersion % "test",
        "org.scalatest" %% "scalatest-core" % scalatestVersion % "provided",
        specs2,
      ) ++ jdbcDriverDependenciesInTestScope ++ scalaTestDependenciesInTestScope.value
    },
  )
  .dependsOn(scalikejdbcLibrary, scalikejdbcJodaTime % "test")

// scalikejdbc-config
lazy val scalikejdbcConfig = projectMatrix
  .defaultAxes(VirtualAxis.jvm)
  .withId("config")
  .in(file("scalikejdbc-config"))
  .jvmPlatform(scalaVersions)
  .settings(
    baseSettings,
    mimaSettings,
    name := "scalikejdbc-config",
    libraryDependencies ++= {
      Seq(
        "com.typesafe" % "config" % _typesafeConfigVersion % "compile",
        "org.slf4j" % "slf4j-api" % _slf4jApiVersion % "compile",
        "ch.qos.logback" % "logback-classic" % _logbackVersion % "test"
      ) ++ scalaTestDependenciesInTestScope.value ++ jdbcDriverDependenciesInTestScope
    },
  )
  .dependsOn(scalikejdbcCore)

// scalikejdbc-streams
lazy val scalikejdbcStreams = projectMatrix
  .defaultAxes(VirtualAxis.jvm)
  .withId("streams")
  .in(file("scalikejdbc-streams"))
  .jvmPlatform(scalaVersions)
  .settings(
    baseSettings,
    mimaSettings,
    name := "scalikejdbc-streams",
    libraryDependencies ++= {
      Seq(
        "org.reactivestreams" % "reactive-streams" % _reactiveStreamsVersion % "compile",
        "org.slf4j" % "slf4j-api" % _slf4jApiVersion % "compile",
        "ch.qos.logback" % "logback-classic" % _logbackVersion % "test",
        "org.scalatestplus" %% "testng-7-5" % "3.2.17.0" % "test",
        "org.reactivestreams" % "reactive-streams-tck" % _reactiveStreamsVersion % "test",
        "org.reactivestreams" % "reactive-streams-examples" % _reactiveStreamsVersion % "test"
      ) ++ scalaTestDependenciesInTestScope.value ++ jdbcDriverDependenciesInTestScope
    },
  )
  .dependsOn(scalikejdbcLibrary)

// scalikejdbc-support
lazy val scalikejdbcSyntaxSupportMacro = projectMatrix
  .defaultAxes(VirtualAxis.jvm)
  .withId("syntax-support-macro")
  .in(file("scalikejdbc-syntax-support-macro"))
  .jvmPlatform(scalaVersions)
  .settings(
    baseSettings,
    name := "scalikejdbc-syntax-support-macro",
    libraryDependencies ++= {
      Seq(
        "ch.qos.logback" % "logback-classic" % _logbackVersion % "test",
        "org.hibernate" % "hibernate-core" % _hibernateVersion % "test"
      ) ++ scalaTestDependenciesInTestScope.value ++ jdbcDriverDependenciesInTestScope
    },
  )
  .dependsOn(scalikejdbcLibrary)

lazy val scalaTestDependenciesInTestScope = Def.setting {
  Seq(
    "org.scalatest" %% "scalatest-flatspec" % scalatestVersion % "test",
    "org.scalatest" %% "scalatest-funspec" % scalatestVersion % "test",
    "org.scalatest" %% "scalatest-shouldmatchers" % scalatestVersion % "test"
  )
}

val jdbcDriverDependenciesInTestScope = Seq(
  "com.h2database" % "h2" % _h2Version % "test",
  "org.apache.derby" % "derby" % "10.17.1.0" % "test",
  "org.xerial" % "sqlite-jdbc" % "3.53.0.0" % "test",
  "org.hsqldb" % "hsqldb" % "2.5.2" % "test",
  mysqlConnectorJ,
  "org.postgresql" % "postgresql" % _postgresqlVersion % "test"
)

val _pomExtra = <url>https://scalikejdbc.org/</url>
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
        <name>Kazuhiro Sera</name>
        <url>https://git.io/sera</url>
      </developer>
      <developer>
        <id>tototoshi</id>
        <name>Toshiyuki Takahashi</name>
        <url>https://github.com/tototoshi</url>
      </developer>
      <developer>
        <id>xuwei-k</id>
        <name>Kenji Yoshida</name>
        <url>https://github.com/xuwei-k</url>
      </developer>
      <developer>
        <id>gakuzzzz</id>
        <name>Manabu Nakamura</name>
        <url>https://github.com/gakuzzzz</url>
      </developer>
      <developer>
        <id>kxbmap</id>
        <name>kxbmap</name>
        <url>https://github.com/kxbmap</url>
      </developer>
      <developer>
        <id>tkawachi</id>
        <name>Takashi Kawachi</name>
        <url>https://github.com/tkawachi</url>
      </developer>
      <developer>
        <id>yoskhdia</id>
        <name>Yoshitaka Okuda</name>
        <url>https://github.com/yoskhdia</url>
      </developer>
    </developers>

TaskKey[Unit]("updateScalikejdbcCliSetupScriptSbt") := {
  val launcherLine = "https://repo1.maven.org/maven2/org/scala-sbt/sbt-launch/"
  Seq[(String, String => String)](
    "scalikejdbc-cli/scripts/setup.bat" -> (v =>
      s"""  call cscript "%self_path%" //E:JScript //Nologo https://repo1.maven.org/maven2/org/scala-sbt/sbt-launch/${v}/sbt-launch-${v}.jar"""
    ),
    "scalikejdbc-cli/scripts/setup.sh" -> (v =>
      s"""wget https://repo1.maven.org/maven2/org/scala-sbt/sbt-launch/${v}/sbt-launch-${v}.jar"""
    ),
  ).map { case (path, func) =>
    IO.writeLines(
      file(path),
      IO.readLines(file(path)).map {
        case line if line.contains(launcherLine) =>
          func(sbtVersion.value)
        case line =>
          line
      }
    )
  }
}
