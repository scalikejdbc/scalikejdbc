import sbt._
import Keys._
import MimaSettings.mimaSettings
import sbtbuildinfo.Plugin._

object ScalikeJDBCProjects extends Build {

  lazy val _version = "3.0.0-SNAPSHOT"

  lazy val _organization = "org.scalikejdbc"

  // published dependency version
  lazy val _slf4jApiVersion = "1.7.21"
  lazy val _typesafeConfigVersion = "1.3.1"

  // internal only
  lazy val _logbackVersion = "1.1.7"
  lazy val _h2Version = "1.4.193"
  // 6.0.x is still under development? https://dev.mysql.com/downloads/connector/j/
  lazy val _mysqlVersion = "5.1.40"
  lazy val _postgresqlVersion = "9.4.1212"
  lazy val _hibernateVersion = "5.2.4.Final"
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
    // https://github.com/sbt/sbt/issues/2217
    fullResolvers ~= { _.filterNot(_.name == "jcenter") },
    transitiveClassifiers in Global := Seq(Artifact.SourceClassifier),
    incOptions := incOptions.value.withNameHashing(true),
    scalatestVersion := "3.0.0",
    specs2Version := "3.8.6",
    //scalaVersion := "2.11.8",
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-encoding", "UTF-8", "-Xlint:-options"),
    javacOptions in doc := Seq("-source", "1.8"),
    scalacOptions ++= _scalacOptions,
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
          "org.scala-lang"          %  "scala-reflect"   % scalaVersion.value,
          "org.apache.commons"      %  "commons-dbcp2"   % "2.1.1"           % "compile",
          "org.slf4j"               %  "slf4j-api"       % _slf4jApiVersion  % "compile",
          "joda-time"               %  "joda-time"       % "2.9.5"           % "compile",
          "org.joda"                %  "joda-convert"    % "1.8.1"           % "compile",
          // scope: provided
          "commons-dbcp"            %  "commons-dbcp"    % "1.4"             % "provided",
          "com.jolbox"              %  "bonecp"          % "0.8.0.RELEASE"   % "provided",
          // scope: test
          "com.zaxxer"              %  "HikariCP"        % "2.4.7"           % "test",
          "ch.qos.logback"          %  "logback-classic" % _logbackVersion   % "test",
          "org.hibernate"           %  "hibernate-core"  % _hibernateVersion % "test",
          "org.mockito"             %  "mockito-all"     % "1.10.+"          % "test"
        ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, scalaMajor)) if scalaMajor >= 11 =>
            Seq("org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4" % "compile")
          case Some((2, 10)) =>
            libraryDependencies.value ++ Seq(
              compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
              "org.scalamacros" %% "quasiquotes" % "2.1.0" cross CrossVersion.binary)
          case _ =>
            Nil
        }) ++ scalaTestDependenciesInTestScope(scalatestVersion.value) ++ jdbcDriverDependenciesInTestScope
      },
      unmanagedSourceDirectories in Compile <+= (scalaVersion, sourceDirectory in Compile){(v, dir) =>
        if (v.startsWith("2.10")) dir / "scala2.10"
        else dir / "scala2.11"
      }
    )
  )

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
        "-Dh2.version=" + _h2Version,
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
          "org.specs2"     %% "specs2-core"     % specs2Version.value % "provided" excludeAll(
            ExclusionRule(organization = "org.spire-math")
          )
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
      }
    )
  ) dependsOn(scalikejdbcLibrary)

  def macroDependenciesInCompileScope(scalaVersion: String) = {
    if (scalaVersion.startsWith("2.10")) Seq(
      "org.scalamacros" % "quasiquotes_2.10" % "2.1.0" % "compile",
      compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
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
    Seq(
      ("org.specs2" %% "specs2-core" % v % "test").excludeAll(
        ExclusionRule(organization = "org.spire-math")
      )
    )

  val jdbcDriverDependenciesInTestScope = Seq(
    "com.h2database"    % "h2"                   % _h2Version         % "test",
    "org.apache.derby"  % "derby"                % "10.13.1.1"        % "test",
    "org.xerial"        % "sqlite-jdbc"          % "3.8.11.2"         % "test",
    "org.hsqldb"        % "hsqldb"               % "2.3.4"            % "test",
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

