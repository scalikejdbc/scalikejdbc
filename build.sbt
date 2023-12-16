import MimaSettings.mimaSettings

publish / skip := true

def Scala3 = "3.3.1"
def Scala212 = "2.12.18"
def Scala213 = "2.13.12"

ThisBuild / version := "4.1.1-SNAPSHOT"

val isScala3 = Def.setting(
  CrossVersion.partialVersion(scalaVersion.value).exists(_._1 == 3)
)

lazy val _organization = "org.scalikejdbc"

// published dependency version
lazy val _slf4jApiVersion = "2.0.9"
lazy val _typesafeConfigVersion = "1.4.3"
lazy val _reactiveStreamsVersion = "1.0.4"

// internal only
lazy val _logbackVersion = "1.2.13"
lazy val _h2Version = "1.4.199"
lazy val _postgresqlVersion = "9.4.1212"
lazy val _hibernateVersion = "6.4.1.Final"
def scalatestVersion = "3.2.17"
lazy val mockitoVersion = "4.11.0"
val specs2 = "org.specs2" %% "specs2-core" % "4.20.3" % "provided"

val mysqlConnectorJ = "com.mysql" % "mysql-connector-j" % "8.2.0" % Test

def gitHash: String = try {
  sys.process.Process("git rev-parse HEAD").lineStream_!.head
} catch {
  case e: Exception =>
    println(e)
    "master"
}

lazy val baseSettings = Def.settings(
  organization := _organization,
  publishTo := sonatypePublishToBundle.value,
  publishMavenStyle := true,
  crossScalaVersions := Seq(Scala212, Scala213, Scala3),
  resolvers ++= _resolvers,
  // https://github.com/sbt/sbt/issues/2217
  fullResolvers ~= { _.filterNot(_.name == "jcenter") },
  Global / transitiveClassifiers := Seq(Artifact.SourceClassifier),
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
  addCommandAlias("SetScala3", s"++ ${Scala3}! -v"),
  addCommandAlias("SetScala212", s"++ ${Scala212}! -v"),
  addCommandAlias("SetScala213", s"++ ${Scala213}! -v"),
  scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature"),
  scalacOptions ++= {
    if (scalaBinaryVersion.value == "2.13") {
      Seq(
        "-Wconf:msg=constructor modifiers are assumed by synthetic:silent",
      )
    } else {
      Nil
    }
  },
  scalacOptions ++= {
    if (isScala3.value) {
      Seq(
        "-language:higherKinds,implicitConversions",
      )
    } else {
      Seq(
        "-language:higherKinds",
        "-Xsource:3"
      )
    }
  },
  scalacOptions ++= PartialFunction
    .condOpt(CrossVersion.partialVersion(scalaVersion.value)) {
      case Some((2, v)) if v <= 12 =>
        Seq(
          "-Yno-adapted-args",
          "-Xfuture"
        )
    }
    .toList
    .flatten,
  (Compile / doc / scalacOptions) ++= Seq(
    "-sourcepath",
    (LocalRootProject / baseDirectory).value.getAbsolutePath,
    "-doc-source-url",
    s"https://github.com/scalikejdbc/scalikejdbc/tree/${gitHash}€{FILE_PATH}.scala"
  ),
  (Compile / packageSrc / mappings) ++= (Compile / managedSources).value.map {
    f =>
      // to merge generated sources into sources.jar as well
      (f, f.relativeTo((Compile / sourceManaged).value).get.getPath)
  },
  publishMavenStyle := true,
  Test / publishArtifact := false,
  pomIncludeRepository := { x => false },
  Test / logBuffered := false,
  Test / parallelExecution := false,
  pomExtra := _pomExtra
)

lazy val scala213projects = List(
  scalikejdbcJodaTime,
  scalikejdbcCore,
  scalikejdbcLibrary,
  scalikejdbcInterpolationMacro,
  scalikejdbcInterpolation,
  scalikejdbcMapperGeneratorCore,
  scalikejdbcTest,
  scalikejdbcConfig,
  scalikejdbcStreams,
  scalikejdbcSyntaxSupportMacro
)

lazy val root213 = Project(
  "root213",
  file("root213")
).settings(
  baseSettings,
  publish / skip := true,
  commands += Command.command("testSequential") {
    scala213projects.map(_.id + "/test").sorted ::: _
  }
).aggregate(
  scala213projects.map(p => p: ProjectReference): _*
)

lazy val scalikejdbcJodaTime = Project(
  id = "joda-time",
  base = file("scalikejdbc-joda-time")
).settings(
  baseSettings,
  mimaSettings,
  name := "scalikejdbc-joda-time",
  libraryDependencies ++= scalaTestDependenciesInTestScope.value,
  libraryDependencies ++= Seq(
    "org.mockito" % "mockito-core" % mockitoVersion % "test",
    "joda-time" % "joda-time" % "2.12.5",
    "org.joda" % "joda-convert" % "2.2.3"
  ),
).dependsOn(
  scalikejdbcLibrary,
  scalikejdbcCore % "test->test",
  scalikejdbcInterpolation % "test->test"
)

// scalikejdbc library
lazy val scalikejdbcLibrary = Project(
  id = "library",
  base = file("scalikejdbc-library")
).settings(
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
).dependsOn(scalikejdbcCore, scalikejdbcInterpolation)

// scalikejdbc (core library)
lazy val scalikejdbcCore = Project(
  id = "core",
  base = file("scalikejdbc-core")
).settings(
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
      "org.apache.commons" % "commons-dbcp2" % "2.11.0" % "compile",
      "org.slf4j" % "slf4j-api" % _slf4jApiVersion % "compile",
      "org.scala-lang.modules" %% "scala-parser-combinators" % "2.3.0" % "compile",
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.11.0",
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
).enablePlugins(BuildInfoPlugin)

// scalikejdbc-interpolation-macro
lazy val scalikejdbcInterpolationMacro = Project(
  id = "interpolation-macro",
  base = file("scalikejdbc-interpolation-macro")
).settings(
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
).dependsOn(scalikejdbcCore)

// scalikejdbc-interpolation
lazy val scalikejdbcInterpolation = Project(
  id = "interpolation",
  base = file("scalikejdbc-interpolation")
).settings(
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
).dependsOn(scalikejdbcCore, scalikejdbcInterpolationMacro)

// scalikejdbc-mapper-generator-core
// core library for mapper-generator
lazy val scalikejdbcMapperGeneratorCore = Project(
  id = "mapper-generator-core",
  base = file("scalikejdbc-mapper-generator-core")
).settings(
  baseSettings,
  mimaSettings,
  name := "scalikejdbc-mapper-generator-core",
  libraryDependencies ++= {
    Seq("org.slf4j" % "slf4j-api" % _slf4jApiVersion % "compile") ++
      scalaTestDependenciesInTestScope.value ++
      jdbcDriverDependenciesInTestScope
  },
).dependsOn(scalikejdbcLibrary)

// mapper-generator sbt plugin
lazy val scalikejdbcMapperGenerator = Project(
  id = "mapper-generator",
  base = file("scalikejdbc-mapper-generator")
).settings(
  baseSettings,
  // Don't update to sbt 1.3.x
  // https://github.com/sbt/sbt/issues/5049
  crossSbtVersions := "1.2.8" :: Nil,
  crossScalaVersions := Seq(Scala212),
  scriptedBufferLog := false,
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
    // TODO https://github.com/scalikejdbc/scalikejdbc/issues/742
    "-Dmysql.version=" + "5.1.49", // mysqlConnectorJ.revision
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
).dependsOn(scalikejdbcCore, scalikejdbcMapperGeneratorCore)
  .enablePlugins(SbtPlugin)

// scalikejdbc-test
lazy val scalikejdbcTest = Project(
  id = "test",
  base = file("scalikejdbc-test")
).settings(
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
).dependsOn(scalikejdbcLibrary, scalikejdbcJodaTime % "test")

// scalikejdbc-config
lazy val scalikejdbcConfig = Project(
  id = "config",
  base = file("scalikejdbc-config")
).settings(
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
).dependsOn(scalikejdbcCore)

// scalikejdbc-streams
lazy val scalikejdbcStreams = Project(
  id = "streams",
  base = file("scalikejdbc-streams")
).settings(
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
).dependsOn(scalikejdbcLibrary)

// scalikejdbc-support
lazy val scalikejdbcSyntaxSupportMacro = Project(
  id = "syntax-support-macro",
  base = file("scalikejdbc-syntax-support-macro")
).settings(
  baseSettings,
  name := "scalikejdbc-syntax-support-macro",
  libraryDependencies ++= {
    Seq(
      "ch.qos.logback" % "logback-classic" % _logbackVersion % "test",
      "org.hibernate" % "hibernate-core" % _hibernateVersion % "test"
    ) ++ scalaTestDependenciesInTestScope.value ++ jdbcDriverDependenciesInTestScope
  },
).dependsOn(scalikejdbcLibrary)

val _resolvers = Seq(
  "typesafe repo" at "https://repo.typesafe.com/typesafe/releases",
  "sonatype staging" at "https://oss.sonatype.org/content/repositories/staging",
  "sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
)
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
  "org.xerial" % "sqlite-jdbc" % "3.44.1.0" % "test",
  "org.hsqldb" % "hsqldb" % "2.5.2" % "test",
  mysqlConnectorJ,
  "org.postgresql" % "postgresql" % _postgresqlVersion % "test"
)

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
