import testgen.TestgenKeys._

scalaVersion := "2.9.2"

crossScalaVersions := Seq("2.9.2", "2.9.1", "2.9.0")

externalResolvers ~= (_.filter(_.name != "Scala-Tools Maven2 Repository"))

resolvers += "sbt-idea-repo" at "http://mpeltonen.github.com/maven/"

resolvers += "typesafe" at "http://repo.typesafe.com/typesafe/releases"

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
  val anorm = "anorm" + _scalaVersion
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
    "com.h2database"          % "h2"                    % "[1.3,)"    % "test",
    "org.apache.derby"        % "derby"                 % "[10.8.2,)" % "test",
    "org.xerial"              % "sqlite-jdbc"           % "3.6.16"    % "test",
    "org.hsqldb"              %  "hsqldb"               % "2.2.8"     % "test",
    "mysql"                   %  "mysql-connector-java" % "[5.1,)"    % "test",
    "postgresql"              %  "postgresql"           % "9.1-901.jdbc4"  % "test",
    "play"                    %  anorm                  % "[2,)"      % "test"
  )
}

seq(lsSettings :_*)

seq(scalariformSettings: _*)

// testgen

seq(testgenSettings: _*)

testgenEncoding in Compile := "UTF-8"

testgenTestTemplate in Compile := "scalatest.FlatSpec"

testgenScalaTestMatchers in Compile := "ShouldMatchers"

testgenLineBreak in Compile := "LF"

// publish

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { x => false }

pomExtra := (
  <url>http://seratch.github.com/scalikejdbc</url>
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
)


