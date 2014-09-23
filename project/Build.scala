import sbt._
import Keys._
import com.typesafe.tools.mima.plugin.MimaPlugin
import com.typesafe.tools.mima.plugin.MimaKeys.{previousArtifact, reportBinaryIssues, binaryIssueFilters}

object ScalikeJDBCProjects extends Build {

  // [NOTE] Execute the following to bump version
  // sbt "g version 1.3.8-SNAPSHOT"
  lazy val _version = "2.1.3-SNAPSHOT"
  lazy val compatibleVersion = "2.1.0"

  lazy val _organization = "org.scalikejdbc"

  // published dependency version
  lazy val _slf4jApiVersion = "1.7.7"
  lazy val _typesafeConfigVersion = "1.2.1"

  // internal only
  lazy val _logbackVersion = "1.1.2"
  lazy val _h2Version = "1.4.+"
  lazy val _hibernateVersion = "4.3.6.Final"
  lazy val _scalatestVersion = "2.2.2"
  lazy val _specs2Version = "2.4.4"

  val mimaProblemFilters = {
    import com.typesafe.tools.mima.core._
    import com.typesafe.tools.mima.core.ProblemFilters._
    Seq(
      /*
      // since 2.0.1
      exclude[MissingMethodProblem]("scalikejdbc.DBConnection.futureLocalTx"),
      exclude[MissingMethodProblem]("scalikejdbc.LoanPattern.futureUsing"),
      // since 2.0.5
      exclude[MissingMethodProblem]("scalikejdbc.DBConnection.autoClose"),
      exclude[MissingMethodProblem]("scalikejdbc.DBConnection.scalikejdbc$DBConnection$$autoCloseEnabled"),
      exclude[MissingMethodProblem]("scalikejdbc.DBConnection.scalikejdbc$DBConnection$$autoCloseEnabled_="),
      exclude[MissingMethodProblem]("scalikejdbc.DBSession.fetchSize"),
      exclude[MissingMethodProblem]("scalikejdbc.DBSession.scalikejdbc$DBSession$$_fetchSize_="),
      exclude[MissingMethodProblem]("scalikejdbc.DBSession.scalikejdbc$DBSession$$_fetchSize"),
      exclude[MissingMethodProblem]("scalikejdbc.config.TypesafeConfigReaderWithEnv"),
      // since 2.1.0
      exclude[MissingMethodProblem]("scalikejdbc.WrappedResultSet.dateTime"),
      exclude[MissingMethodProblem]("scalikejdbc.WrappedResultSet.localDate"),
      exclude[MissingMethodProblem]("scalikejdbc.WrappedResultSet.localTime"),
      exclude[MissingMethodProblem]("scalikejdbc.WrappedResultSet.localDateTime"),
      exclude[MissingMethodProblem]("scalikejdbc.WrappedResultSet.dateTimeOpt"),
      exclude[MissingMethodProblem]("scalikejdbc.WrappedResultSet.localDateOpt"),
      exclude[MissingMethodProblem]("scalikejdbc.WrappedResultSet.localTimeOpt"),
      exclude[MissingMethodProblem]("scalikejdbc.WrappedResultSet.localDateTimeOpt"),
      exclude[MissingTypesProblem]("scalikejdbc.mapper.GeneratorConfig$"),
      exclude[MissingMethodProblem]("scalikejdbc.mapper.GeneratorConfig.apply"),
      exclude[MissingMethodProblem]("scalikejdbc.mapper.GeneratorConfig.apply"),
      exclude[MissingMethodProblem]("scalikejdbc.mapper.GeneratorConfig.copy"),
      exclude[MissingMethodProblem]("scalikejdbc.mapper.GeneratorConfig.this")
      */
      exclude[MissingMethodProblem]("scalikejdbc.mapper.GeneratorConfig.copy"),
      exclude[MissingMethodProblem]("scalikejdbc.mapper.GeneratorConfig.this"),
      exclude[MissingTypesProblem]("scalikejdbc.mapper.GeneratorConfig$"),
      exclude[MissingMethodProblem]("scalikejdbc.mapper.GeneratorConfig.apply")
    )
  }

  val mimaSettings = MimaPlugin.mimaDefaultSettings ++ Seq(
    previousArtifact := Some(_organization % s"${name.value}_${scalaBinaryVersion.value}" % compatibleVersion),
    test in Test := {
      reportBinaryIssues.value
      (test in Test).value
    },
    binaryIssueFilters ++= mimaProblemFilters
  )

  lazy val baseSettings = Seq(
    organization := _organization,
    version := _version,
    publishTo <<= version { (v: String) => _publishTo(v) },
    publishMavenStyle := true,
    resolvers ++= _resolvers,
    transitiveClassifiers in Global := Seq(Artifact.SourceClassifier),
    incOptions := incOptions.value.withNameHashing(true),
    //scalaVersion := "2.11.1",
    scalacOptions ++= _scalacOptions,
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { x => false },
    logBuffered in Test := false,
    //parallelExecution in Test := false,
    pomExtra := _pomExtra
  )

  val root211Id = "root211"
  val mapperGeneratorId = "mapper-generator"

  lazy val root211 = Project(
    root211Id,
    file("root211")
  ).settings(
    baseSettings: _*
  ).copy(
    aggregate = projects.filterNot(p => Set(root211Id, mapperGeneratorId).contains(p.id)).map(p => p: ProjectReference)
  )

  // scalikejdbc library
  lazy val scalikejdbcLibrary = Project(
    id = "library",
    base = file("scalikejdbc-library"),
    settings = baseSettings ++ mimaSettings ++ Seq(
      name := "scalikejdbc",
      libraryDependencies ++= scalaTestDependenciesInTestScope ++
        Seq("com.h2database" % "h2" % _h2Version % "test")
    )
  ) dependsOn(scalikejdbcCore, scalikejdbcInterpolation)

  // scalikejdbc (core library)
  lazy val scalikejdbcCore = Project(
    id = "core",
    base = file("scalikejdbc-core"),
    settings = baseSettings ++ mimaSettings ++ Seq(
      name := "scalikejdbc-core",
      libraryDependencies <++= (scalaVersion) { scalaVersion =>
        Seq(
          // scope: compile
          "commons-dbcp"            %  "commons-dbcp"    % "1.4"             % "compile",
          "org.slf4j"               %  "slf4j-api"       % _slf4jApiVersion  % "compile",
          "joda-time"               %  "joda-time"       % "2.4"             % "compile",
          "org.joda"                %  "joda-convert"    % "1.7"             % "compile",
          // scope: provided
          // commons-dbcp2 will be the default CP implementation since ScalikeJDBC 2.1
          "org.apache.commons"      %  "commons-dbcp2"   % "2.0.+"           % "provided",
          "com.jolbox"              %  "bonecp"          % "0.8.0.RELEASE"   % "provided",
          // scope: test
          "com.zaxxer"              %  "HikariCP"        % "1.4.+"           % "test",
          "ch.qos.logback"          %  "logback-classic" % _logbackVersion   % "test",
          "org.hibernate"           %  "hibernate-core"  % _hibernateVersion % "test",
          "org.mockito"             %  "mockito-all"     % "1.9.+"           % "test"
        ) ++ (scalaVersion match {
          case v if v.startsWith("2.11.") => Seq("org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.2" % "compile")
          case _ => Nil
        }) ++ scalaTestDependenciesInTestScope ++ jdbcDriverDependenciesInTestScope
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
      ) ++ scalaTestDependenciesInTestScope
    )
  ) dependsOn(scalikejdbcLibrary)

  // scalikejdbc-interpolation-core
  // basic modules that are used by interpolation-macro
  lazy val scalikejdbcInterpolationCore = Project(
    id = "interpolation-core",
    base = file("scalikejdbc-interpolation-core"),
    settings = baseSettings ++ mimaSettings ++ Seq(
      name := "scalikejdbc-interpolation-core",
      libraryDependencies ++= {
        Seq(
          "org.slf4j"      %  "slf4j-api"        % _slf4jApiVersion  % "compile",
          "ch.qos.logback" %  "logback-classic"  % _logbackVersion   % "test"
        ) ++ scalaTestDependenciesInTestScope ++ jdbcDriverDependenciesInTestScope
      }
    )
  ) dependsOn(scalikejdbcCore)

  // scalikejdbc-interpolation-macro
  lazy val scalikejdbcInterpolationMacro = Project(
    id = "interpolation-macro",
    base = file("scalikejdbc-interpolation-macro"),
    settings = baseSettings ++ mimaSettings ++ Seq(
      name := "scalikejdbc-interpolation-macro",
      libraryDependencies <++= (scalaVersion) { scalaVersion =>
        Seq(
          "org.scala-lang" %  "scala-reflect"    % scalaVersion      % "compile",
          "org.scala-lang" %  "scala-compiler"   % scalaVersion      % "optional"
        ) ++ scalaTestDependenciesInTestScope
      }
    )
  ) dependsOn(scalikejdbcInterpolationCore)

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
        ) ++ scalaTestDependenciesInTestScope ++ jdbcDriverDependenciesInTestScope
      }
    )
  ) dependsOn(scalikejdbcInterpolationCore, scalikejdbcInterpolationMacro)

  // scalikejdbc-mapper-generator-core
  // core library for mapper-generator
  lazy val scalikejdbcMapperGeneratorCore = Project(
    id = "mapper-generator-core",
    base = file("scalikejdbc-mapper-generator-core"),
    settings = baseSettings ++ mimaSettings ++ Seq(
      name := "scalikejdbc-mapper-generator-core",
      libraryDependencies ++= {
        Seq("org.slf4j"     %  "slf4j-api" % _slf4jApiVersion   % "compile") ++
          scalaTestDependenciesInTestScope ++
          specs2DependenciesInTestScope ++
          jdbcDriverDependenciesInTestScope
      }
    )
  ) dependsOn(scalikejdbcLibrary, scalikejdbcTest)

  // mapper-generator sbt plugin
  lazy val scalikejdbcMapperGenerator = Project(
    id = mapperGeneratorId,
    base = file("scalikejdbc-mapper-generator"),
    settings = baseSettings ++ ScriptedPlugin.scriptedSettings ++ Seq(
      sbtPlugin := true,
      ScriptedPlugin.scriptedBufferLog := false,
      ScriptedPlugin.scriptedLaunchOpts ++= sys.process.javaVmArguments.filter(
        a => Seq("-Xmx","-Xms","-XX").exists(a.startsWith)
      ),
      ScriptedPlugin.scriptedLaunchOpts += ("-Dplugin.version=" + version.value),
      name := "scalikejdbc-mapper-generator",
      libraryDependencies ++= {
        Seq("org.slf4j"     %  "slf4j-simple" % _slf4jApiVersion  % "compile") ++
          scalaTestDependenciesInTestScope ++
          specs2DependenciesInTestScope ++
          jdbcDriverDependenciesInTestScope
      }
    )
  ) dependsOn(scalikejdbcCore, scalikejdbcTest, scalikejdbcMapperGeneratorCore)

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
          "org.scalatest"  %% "scalatest"       % _scalatestVersion % "provided",
          "org.specs2"     %% "specs2-core"     % _specs2Version    % "provided"
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
        ) ++ scalaTestDependenciesInTestScope ++ jdbcDriverDependenciesInTestScope
      }
    )
  ) dependsOn(scalikejdbcCore)

  // scalikejdbc-support
  lazy val scalikejdbcSyntaxSupportMacro = Project(
    id = "syntax-support-macro",
    base = file("scalikejdbc-syntax-support-macro"),
    settings = baseSettings ++ Seq(
      name := "scalikejdbc-syntax-support-macro",
      libraryDependencies <++= (scalaVersion) { scalaVersion =>
        Seq(
          "ch.qos.logback"  %  "logback-classic"  % _logbackVersion   % "test",
          "org.hibernate"   %  "hibernate-core"   % _hibernateVersion % "test"
        ) ++ scalaTestDependenciesInTestScope ++ jdbcDriverDependenciesInTestScope ++ macroDependenciesInCompileScope(scalaVersion)
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
  lazy val scalaTestDependenciesInTestScope =
    Seq("org.scalatest" %% "scalatest" % _scalatestVersion % "test")

  lazy val specs2DependenciesInTestScope =
    Seq("org.specs2" %% "specs2-core" % _specs2Version % "test")

  val jdbcDriverDependenciesInTestScope = Seq(
    "com.h2database"    % "h2"                   % _h2Version        % "test",
    "org.apache.derby"  % "derby"                % "10.11.1.1"       % "test",
    "org.xerial"        % "sqlite-jdbc"          % "3.7.2"           % "test",
    "org.hsqldb"        % "hsqldb"               % "2.3.2"           % "test",
    "mysql"             % "mysql-connector-java" % "5.1.+"           % "test",
    "org.postgresql"    % "postgresql"           % "9.3-1102-jdbc41" % "test"
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

