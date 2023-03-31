import MimaSettings.mimaSettings

lazy val _version = "3.5.1-SNAPSHOT"

lazy val _organization = "org.scalikejdbc"

// published dependency version
lazy val _slf4jApiVersion = "1.7.36"
lazy val _typesafeConfigVersion = "1.4.2"
lazy val _reactiveStreamsVersion = "1.0.4"

// internal only
lazy val _logbackVersion = "1.2.11"
lazy val _h2Version = "1.4.199"
// TODO update to 8.x? https://github.com/scalikejdbc/scalikejdbc/issues/742
lazy val _mysqlVersion = "5.1.49"
lazy val _postgresqlVersion = "9.4.1212"
lazy val _hibernateVersion = "6.2.0.CR3"
lazy val scalatestVersion = SettingKey[String]("scalatestVersion")
lazy val specs2Version = SettingKey[String]("specs2Version")
lazy val parserCombinatorsVersion = settingKey[String]("")
lazy val mockitoVersion = "4.11.0"
lazy val collectionCompatVersion = settingKey[String]("")

def gitHash: String = try {
  sys.process.Process("git rev-parse HEAD").lineStream_!.head
} catch {
  case e: Exception =>
    println(e)
    "master"
}

mimaFailOnNoPrevious := false

lazy val baseSettings = Seq(
  ThisBuild / mimaReportSignatureProblems := true,
  organization := _organization,
  version := _version,
  publishTo := _publishTo(version.value),
  publishMavenStyle := true,
  resolvers ++= _resolvers,
  // https://github.com/sbt/sbt/issues/2217
  fullResolvers ~= { _.filterNot(_.name == "jcenter") },
  transitiveClassifiers in Global := Seq(Artifact.SourceClassifier),
  scalatestVersion := "3.2.0",
  specs2Version := "4.10.6",
  parserCombinatorsVersion := "1.1.2",
  collectionCompatVersion := "2.1.6",
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-encoding", "UTF-8", "-Xlint:-options"),
  javacOptions in doc := Seq("-source", "1.8"),
  fork in Test := true,
  baseDirectory in Test := file("."),
  scalacOptions ++= _scalacOptions,
  scalacOptions ++= PartialFunction.condOpt(CrossVersion.partialVersion(scalaVersion.value)) {
    case Some((2, v)) if v <= 12 =>
      Seq(
        "-Yno-adapted-args",
        "-Xfuture"
      )
  }.toList.flatten,
  scalacOptions in (Compile, doc) ++= Seq(
    "-sourcepath", (baseDirectory in LocalRootProject).value.getAbsolutePath,
    "-doc-source-url", s"https://github.com/scalikejdbc/scalikejdbc/tree/${gitHash}€{FILE_PATH}.scala"
  ),
  mappings in (Compile, packageSrc) ++= (managedSources in Compile).value.map{ f =>
    // to merge generated sources into sources.jar as well
    (f, f.relativeTo((sourceManaged in Compile).value).get.getPath)
  },
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { x => false },
  logBuffered in Test := false,
  parallelExecution in Test := false,
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
  mimaFailOnNoPrevious := false,
  commands += Command.command("testSequential"){
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
    "org.joda" % "joda-convert" % "2.2.2"
  )
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
  (sourceGenerators in Compile) += task{
    val dir = (sourceManaged in Compile).value
    val file = dir / "scalikejdbc" / "DeprecatedOneToManiesTraversable.scala"
    IO.write(file, GenerateDeprecatedOneToManiesTraversable.value)
    Seq(file)
  },
  libraryDependencies ++= scalaTestDependenciesInTestScope.value ++
    Seq("com.h2database" % "h2" % _h2Version % "test")
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
  TaskKey[Unit]("checkScalariform") := {
    val diff = sys.process.Process("git diff").!!
    if(diff.nonEmpty){
      sys.error("Working directory is dirty!\n" + diff)
    }
  },
  (sourceGenerators in Compile) += task{
    val dir = (sourceManaged in Compile).value
    val file = dir / "scalikejdbc" / "OneToXSQL.scala"
    IO.write(file, GenerateOneToXSQL.value)
    Seq(file)
  },
  (sourceGenerators in Compile) += task[Seq[File]]{
    val dir = (sourceManaged in Compile).value
    (3 to 21).map{ n =>
      val file = dir / "scalikejdbc" / s"OneToManies${n}SQL.scala"
      IO.write(file, GenerateOneToManies(n))
      file
    }
  },
  libraryDependencies ++= {
    Seq(
      // scope: compile
      "org.apache.commons"      %  "commons-dbcp2"   % "2.9.0"           % "compile",
      "org.slf4j"               %  "slf4j-api"       % _slf4jApiVersion  % "compile",
      "org.scala-lang.modules"  %% "scala-parser-combinators" % parserCombinatorsVersion.value % "compile",
      "org.scala-lang.modules"  %% "scala-collection-compat" % collectionCompatVersion.value,
      // scope: provided
      "commons-dbcp"            %  "commons-dbcp"    % "1.4"             % "provided",
      "com.jolbox"              %  "bonecp"          % "0.8.0.RELEASE"   % "provided",
      // scope: test
      "com.zaxxer"              %  "HikariCP"        % "4.0.3"           % "test",
      "ch.qos.logback"          %  "logback-classic" % _logbackVersion   % "test",
      "org.hibernate"           %  "hibernate-core"  % _hibernateVersion % "test",
      "org.mockito"             %  "mockito-core"    % mockitoVersion    % "test"
    ) ++ scalaTestDependenciesInTestScope.value ++ jdbcDriverDependenciesInTestScope
  }
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
    Seq(
      "org.scala-lang" %  "scala-reflect"    % scalaVersion.value % "compile",
      "org.scala-lang" %  "scala-compiler"   % scalaVersion.value % "optional"
    ) ++ scalaTestDependenciesInTestScope.value
  }
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
      "org.slf4j"      %  "slf4j-api"        % _slf4jApiVersion  % "compile",
      "ch.qos.logback" %  "logback-classic"  % _logbackVersion   % "test",
      "org.hibernate"  %  "hibernate-core"   % _hibernateVersion % "test"
    ) ++ scalaTestDependenciesInTestScope.value ++ jdbcDriverDependenciesInTestScope
  }
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
    Seq("org.slf4j"     %  "slf4j-api" % _slf4jApiVersion   % "compile") ++
      scalaTestDependenciesInTestScope.value ++
      jdbcDriverDependenciesInTestScope
  }
).dependsOn(scalikejdbcLibrary)

// mapper-generator sbt plugin
lazy val scalikejdbcMapperGenerator = Project(
  id = "mapper-generator",
  base = file("scalikejdbc-mapper-generator")
).settings(
  baseSettings,
  mimaFailOnNoPrevious := false,
  // Don't update to sbt 1.3.x
  // https://github.com/sbt/sbt/issues/5049
  crossSbtVersions := "1.2.8" :: Nil,
  scriptedBufferLog := false,
  scriptedLaunchOpts ++= {
    val javaVmArgs = {
      import scala.collection.JavaConverters._
      java.lang.management.ManagementFactory.getRuntimeMXBean.getInputArguments.asScala.toList
    }
    javaVmArgs.filter(
      a => Seq("-XX","-Xss").exists(a.startsWith)
    ) ++ Seq("-Xmx3G")
  },
  scriptedLaunchOpts ++= Seq(
    "-Dplugin.version=" + version.value,
    "-Dslf4j.version=" + _slf4jApiVersion,
    "-Dmysql.version=" + _mysqlVersion,
    "-Dpostgresql.version=" + _postgresqlVersion,
    "-Dh2.version=" + _h2Version,
    "-Dspecs2.version=" + specs2Version.value,
    "-Dscalatest.version=" + scalatestVersion.value
  ),
  name := "scalikejdbc-mapper-generator",
  libraryDependencies ++= {
    Seq("org.slf4j"     %  "slf4j-simple" % _slf4jApiVersion  % "compile") ++
      scalaTestDependenciesInTestScope.value ++
      jdbcDriverDependenciesInTestScope
  }
).dependsOn(scalikejdbcCore, scalikejdbcMapperGeneratorCore).enablePlugins(SbtPlugin)

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
      "org.slf4j"      %  "slf4j-api"       % _slf4jApiVersion  % "compile",
      "ch.qos.logback" %  "logback-classic" % _logbackVersion   % "test",
      "org.scalatest"  %% "scalatest-core"  % scalatestVersion.value % "provided",
      "org.specs2"     %% "specs2-core"     % specs2Version.value % "provided" excludeAll(
        ExclusionRule(organization = "org.spire-math")
      )
    ) ++ jdbcDriverDependenciesInTestScope ++ scalaTestDependenciesInTestScope.value
  }
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
      "com.typesafe"   %  "config"          % _typesafeConfigVersion % "compile",
      "org.slf4j"      %  "slf4j-api"       % _slf4jApiVersion       % "compile",
      "ch.qos.logback" %  "logback-classic" % _logbackVersion        % "test"
    ) ++ scalaTestDependenciesInTestScope.value ++ jdbcDriverDependenciesInTestScope
  }
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
      "org.reactivestreams" %  "reactive-streams"          % _reactiveStreamsVersion % "compile",
      "org.slf4j"           %  "slf4j-api"                 % _slf4jApiVersion        % "compile",
      "ch.qos.logback"      %  "logback-classic"           % _logbackVersion         % "test",
      "org.scalatestplus"   %% "testng-7-5"                % "3.2.15.0"              % "test",
      "org.reactivestreams" %  "reactive-streams-tck"      % _reactiveStreamsVersion % "test",
      "org.reactivestreams" %  "reactive-streams-examples" % _reactiveStreamsVersion % "test"
    ) ++ scalaTestDependenciesInTestScope.value ++ jdbcDriverDependenciesInTestScope
  },
).dependsOn(scalikejdbcLibrary)

// scalikejdbc-support
lazy val scalikejdbcSyntaxSupportMacro = Project(
  id = "syntax-support-macro",
  base = file("scalikejdbc-syntax-support-macro")
).settings(
  baseSettings,
  mimaSettings,
  name := "scalikejdbc-syntax-support-macro",
  libraryDependencies ++= {
    Seq(
      "ch.qos.logback"  %  "logback-classic"  % _logbackVersion   % "test",
      "org.hibernate"   %  "hibernate-core"   % _hibernateVersion % "test"
    ) ++ scalaTestDependenciesInTestScope.value ++ jdbcDriverDependenciesInTestScope
  }
).dependsOn(scalikejdbcLibrary)

def _publishTo(v: String) = {
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
val _resolvers = Seq(
  "typesafe repo" at "https://repo.typesafe.com/typesafe/releases",
  "sonatype staging" at "https://oss.sonatype.org/content/repositories/staging",
  "sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
)
lazy val scalaTestDependenciesInTestScope = Def.setting {
  Seq("org.scalatest" %% "scalatest" % scalatestVersion.value % "test")
}

val jdbcDriverDependenciesInTestScope = Seq(
  "com.h2database"    % "h2"                   % _h2Version         % "test",
  "org.apache.derby"  % "derby"                % "10.16.1.1"        % "test",
  "org.xerial"        % "sqlite-jdbc"          % "3.41.0.0"         % "test",
  "org.hsqldb"        % "hsqldb"               % "2.5.2"            % "test",
  "mysql"             % "mysql-connector-java" % _mysqlVersion      % "test",
  "org.postgresql"    % "postgresql"           % _postgresqlVersion % "test"
)

val _scalacOptions = Seq("-language:higherKinds", "-deprecation", "-unchecked", "-feature", "-Xsource:3")
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
        <url>http://git.io/sera</url>
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
