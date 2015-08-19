import sbt._
import Keys._
import MimaSettings.mimaSettings
import sbtbuildinfo.Plugin._

object ScalikeJDBCProjects extends Build {

  lazy val _version = "2.2.9-SNAPSHOT"

  lazy val _organization = "org.scalikejdbc"

  // published dependency version
  lazy val _slf4jApiVersion = "1.7.12"
  lazy val _typesafeConfigVersion = "1.2.1"

  // internal only
  lazy val _logbackVersion = "1.1.3"
  lazy val _h2Version = "1.4.188"
  lazy val _mysqlVersion = "5.1.36"
  lazy val _postgresqlVersion = "9.4-1201-jdbc41"
  lazy val _hibernateVersion = "4.3.11.Final"
  lazy val scalatestVersion = SettingKey[String]("scalatestVersion")
  lazy val specs2Version = SettingKey[String]("specs2Version")

  private def gitHash: String = try {
    sys.process.Process("git rev-parse HEAD").lines_!.head
  } catch {
    case e: Exception =>
      println(e)
      "master"
  }

  lazy val baseSettings = Seq(
    organization := _organization,
    version := _version,
    publishTo <<= version { (v: String) => _publishTo(v) },
    publishMavenStyle := true,
    resolvers ++= _resolvers,
    transitiveClassifiers in Global := Seq(Artifact.SourceClassifier),
    incOptions := incOptions.value.withNameHashing(true),
    scalatestVersion := {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, v)) if v <= 11 => "2.2.5"
        case _ => "2.2.5-M2"
      }
    },
    specs2Version := {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, v)) if v <= 11 => "2.4.17"
        case _ => "3.6.3"
      }
    },
    //scalaVersion := "2.11.7",
    scalacOptions ++= _scalacOptions,
    scalacOptions in (Compile, doc) ++= Seq(
      "-sourcepath", (baseDirectory in LocalRootProject).value.getAbsolutePath,
      "-doc-source-url", s"https://github.com/scalikejdbc/scalikejdbc/tree/${gitHash}€{FILE_PATH}.scala"
    ),
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { x => false },
    logBuffered in Test := false,
    parallelExecution in Test := false,
    pomExtra := _pomExtra
  )

  val root211Id = "root211"
  val mapperGeneratorId = "mapper-generator"

  lazy val root211 = Project(
    root211Id,
    file("root211")
  ).settings(
    baseSettings: _*
  ).settings(
    commands += Command.command("testSequential"){
      projects.map(_.id).filterNot(Set(root211Id, mapperGeneratorId)).map(_ + "/test").sorted ::: _
    }
  ).copy(
    aggregate = projects.filterNot(p => Set(root211Id, mapperGeneratorId).contains(p.id)).map(p => p: ProjectReference)
  )

  // scalikejdbc library
  lazy val scalikejdbcLibrary = Project(
    id = "library",
    base = file("scalikejdbc-library"),
    settings = baseSettings ++ mimaSettings ++ Seq(
      name := "scalikejdbc",
      libraryDependencies ++= scalaTestDependenciesInTestScope(scalatestVersion.value) ++
        Seq("com.h2database" % "h2" % _h2Version % "test")
    )
  ) dependsOn(scalikejdbcCore, scalikejdbcInterpolation)

  // scalikejdbc (core library)
  lazy val scalikejdbcCore = Project(
    id = "core",
    base = file("scalikejdbc-core"),
    settings = baseSettings ++ mimaSettings ++ buildInfoSettings ++ Seq(
      name := "scalikejdbc-core",
      sourceGenerators in Compile <+= buildInfo,
      buildInfoPackage := "scalikejdbc",
      buildInfoObject := "ScalikejdbcBuildInfo",
      buildInfoKeys := Seq[BuildInfoKey](version, scalaVersion),
      TaskKey[Unit]("checkScalariform") := {
        val diff = "git diff".!!
        if(diff.nonEmpty){
          sys.error("Working directory is dirty!\n" + diff)
        }
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
          "commons-dbcp"            %  "commons-dbcp"    % "1.4"             % "compile",
          "org.slf4j"               %  "slf4j-api"       % _slf4jApiVersion  % "compile",
          "joda-time"               %  "joda-time"       % "2.8.2"           % "compile",
          "org.joda"                %  "joda-convert"    % "1.7"             % "compile",
          // scope: provided
          // commons-dbcp2 will be the default CP implementation since ScalikeJDBC 2.1
          "org.apache.commons"      %  "commons-dbcp2"   % "2.0.+"           % "provided",
          "com.jolbox"              %  "bonecp"          % "0.8.0.RELEASE"   % "provided",
          // scope: test
          "com.zaxxer"              %  "HikariCP"        % "1.4.+"           % "test",
          "ch.qos.logback"          %  "logback-classic" % _logbackVersion   % "test",
          "org.hibernate"           %  "hibernate-core"  % _hibernateVersion % "test",
          "org.mockito"             %  "mockito-all"     % "1.10.+"          % "test"
        ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, scalaMajor)) if scalaMajor >= 11 =>
            Seq("org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4" % "compile")
          case _ =>
            Nil
        }) ++ scalaTestDependenciesInTestScope(scalatestVersion.value) ++ jdbcDriverDependenciesInTestScope
      }
    )
  )

  lazy val scalikejdbcJSR310 = Project(
    id = "jsr310",
    base = file("scalikejdbc-jsr310"),
    settings = baseSettings ++ /*mimaSettings ++*/ Seq(
      name := "scalikejdbc-jsr310",
      libraryDependencies ++= Seq(
        "com.github.seratch" %  "java-time-backport" % "1.0.0"    % "provided,test",
        "com.h2database"     %  "h2"                 % _h2Version % "test"
      ) ++ scalaTestDependenciesInTestScope(scalatestVersion.value)
    )
  ) dependsOn(scalikejdbcLibrary)

  // scalikejdbc-interpolation-macro
  lazy val scalikejdbcInterpolationMacro = Project(
    id = "interpolation-macro",
    base = file("scalikejdbc-interpolation-macro"),
    settings = baseSettings ++ mimaSettings ++ Seq(
      name := "scalikejdbc-interpolation-macro",
      libraryDependencies ++= {
        Seq(
          "org.scala-lang" %  "scala-reflect"    % scalaVersion.value % "compile",
          "org.scala-lang" %  "scala-compiler"   % scalaVersion.value % "optional"
        ) ++ scalaTestDependenciesInTestScope(scalatestVersion.value)
      }
    )
  ) dependsOn(scalikejdbcCore)

  // scalikejdbc-interpolation
  lazy val scalikejdbcInterpolation = Project(
    id = "interpolation",
    base = file("scalikejdbc-interpolation"),
    settings = baseSettings ++ mimaSettings ++ Seq(
      name := "scalikejdbc-interpolation",
      libraryDependencies ++= {
        Seq(
          "org.slf4j"      %  "slf4j-api"        % _slf4jApiVersion  % "compile",
          "ch.qos.logback" %  "logback-classic"  % _logbackVersion   % "test",
          "org.hibernate"  %  "hibernate-core"   % _hibernateVersion % "test"
        ) ++ scalaTestDependenciesInTestScope(scalatestVersion.value) ++ jdbcDriverDependenciesInTestScope
      }
    )
  ) dependsOn(scalikejdbcCore, scalikejdbcInterpolationMacro)

  // scalikejdbc-mapper-generator-core
  // core library for mapper-generator
  lazy val scalikejdbcMapperGeneratorCore = Project(
    id = "mapper-generator-core",
    base = file("scalikejdbc-mapper-generator-core"),
    settings = baseSettings ++ mimaSettings ++ Seq(
      name := "scalikejdbc-mapper-generator-core",
      libraryDependencies ++= {
        Seq("org.slf4j"     %  "slf4j-api" % _slf4jApiVersion   % "compile") ++
          scalaTestDependenciesInTestScope(scalatestVersion.value) ++
          specs2DependenciesInTestScope(specs2Version.value) ++
          jdbcDriverDependenciesInTestScope
      }
    )
  ) dependsOn(scalikejdbcLibrary)

  // mapper-generator sbt plugin
  lazy val scalikejdbcMapperGenerator = Project(
    id = mapperGeneratorId,
    base = file("scalikejdbc-mapper-generator"),
    settings = baseSettings ++ ScriptedPlugin.scriptedSettings ++ Seq(
      sbtPlugin := true,
      ScriptedPlugin.scriptedBufferLog := false,
      ScriptedPlugin.scriptedLaunchOpts ++= sys.process.javaVmArguments.filter(
        a => Seq("-XX","-Xss").exists(a.startsWith)
      ) ++ Seq("-Xmx3G"),
      ScriptedPlugin.scriptedLaunchOpts ++= Seq(
        "-Dplugin.version=" + version.value,
        "-Dslf4j.version=" + _slf4jApiVersion,
        "-Dmysql.version=" + _mysqlVersion,
        "-Dpostgresql.version=" + _postgresqlVersion,
        "-Dh2.version=1.4.181",
        "-Dspecs2.version=" + specs2Version.value,
        "-Dscalatest.version=" + scalatestVersion.value
      ),
      name := "scalikejdbc-mapper-generator",
      libraryDependencies ++= {
        Seq("org.slf4j"     %  "slf4j-simple" % _slf4jApiVersion  % "compile") ++
          scalaTestDependenciesInTestScope(scalatestVersion.value) ++
          specs2DependenciesInTestScope(specs2Version.value) ++
          jdbcDriverDependenciesInTestScope
      }
    )
  ) dependsOn(scalikejdbcCore, scalikejdbcMapperGeneratorCore)

  // scalikejdbc-test
  lazy val scalikejdbcTest = Project(
    id = "test",
    base = file("scalikejdbc-test"),
    settings = baseSettings ++ mimaSettings ++ Seq(
      name := "scalikejdbc-test",
      libraryDependencies ++= {
        Seq(
          "org.slf4j"      %  "slf4j-api"       % _slf4jApiVersion  % "compile",
          "ch.qos.logback" %  "logback-classic" % _logbackVersion   % "test",
          "org.scalatest"  %% "scalatest"       % scalatestVersion.value % "provided",
          "org.specs2"     %% "specs2-core"     % specs2Version.value % "provided"
        ) ++ jdbcDriverDependenciesInTestScope
      }
    )
  ) dependsOn(scalikejdbcLibrary)

  // scalikejdbc-config
  lazy val scalikejdbcConfig = Project(
    id = "config",
    base = file("scalikejdbc-config"),
    settings = baseSettings ++ mimaSettings ++ Seq(
      name := "scalikejdbc-config",
      libraryDependencies ++= {
        Seq(
          "com.typesafe"   %  "config"          % _typesafeConfigVersion % "compile",
          "org.slf4j"      %  "slf4j-api"       % _slf4jApiVersion       % "compile",
          "ch.qos.logback" %  "logback-classic" % _logbackVersion        % "test"
        ) ++ scalaTestDependenciesInTestScope(scalatestVersion.value) ++ jdbcDriverDependenciesInTestScope
      }
    )
  ) dependsOn(scalikejdbcCore)

  // scalikejdbc-support
  lazy val scalikejdbcSyntaxSupportMacro = Project(
    id = "syntax-support-macro",
    base = file("scalikejdbc-syntax-support-macro"),
    settings = baseSettings ++ Seq(
      name := "scalikejdbc-syntax-support-macro",
      libraryDependencies ++= {
        Seq(
          "ch.qos.logback"  %  "logback-classic"  % _logbackVersion   % "test",
          "org.hibernate"   %  "hibernate-core"   % _hibernateVersion % "test"
        ) ++ scalaTestDependenciesInTestScope(scalatestVersion.value) ++ jdbcDriverDependenciesInTestScope ++ macroDependenciesInCompileScope(scalaVersion.value)
      },
      unmanagedSourceDirectories in Compile <+= (scalaVersion, sourceDirectory in Compile){(v, dir) =>
        if (v.startsWith("2.10")) dir / "scala2.10"
        else dir / "scala2.11"
      }
    )
  ) dependsOn(scalikejdbcLibrary)

  def macroDependenciesInCompileScope(scalaVersion: String) = {
    if (scalaVersion.startsWith("2.10")) Seq(
      "org.scalamacros" %% "quasiquotes" % "2.0.1" % "compile",
      compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)
    ) else Seq()
  }

  def _publishTo(v: String) = {
    val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
    else Some("releases" at nexus + "service/local/staging/deploy/maven2")
  }
  val _resolvers = Seq(
    "typesafe repo" at "http://repo.typesafe.com/typesafe/releases",
    "sonatype releases" at "https://oss.sonatype.org/content/repositories/releases",
    "sonatype snaphots" at "https://oss.sonatype.org/content/repositories/snapshots"
  )
  def scalaTestDependenciesInTestScope(v: String) =
    Seq("org.scalatest" %% "scalatest" % v % "test")

  def specs2DependenciesInTestScope(v: String) =
    Seq("org.specs2" %% "specs2-core" % v % "test")

  val jdbcDriverDependenciesInTestScope = Seq(
    "com.h2database"    % "h2"                   % _h2Version         % "test",
    "org.apache.derby"  % "derby"                % "10.11.1.1"        % "test",
    "org.xerial"        % "sqlite-jdbc"          % "3.8.11.1"         % "test",
    "org.hsqldb"        % "hsqldb"               % "2.3.3"            % "test",
    "mysql"             % "mysql-connector-java" % _mysqlVersion      % "test",
    "org.postgresql"    % "postgresql"           % _postgresqlVersion % "test"
  )
  //val _scalacOptions = Seq("-deprecation", "-unchecked", "-Ymacro-debug-lite", "-Xlog-free-terms", "Yshow-trees", "-feature")
  val _scalacOptions = Seq("-deprecation", "-unchecked", "-feature")
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
      </developers>

}

