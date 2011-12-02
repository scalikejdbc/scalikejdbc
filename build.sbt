name := "scalikejdbc"

organization := "com.github.seratch"

crossScalaVersions := Seq("2.9.1", "2.9.0-1", "2.9.0")

scalaVersion := "2.9.1"

resolvers ++= Seq(
  "scala-tools-snapshots" at "http://scala-tools.org/repo-snapshots",
  "scala-tools-releases"  at "http://scala-tools.org/repo-releases",
  "seratch-github-repo"   at "http://seratch.github.com/mvn-repo/releases",
  "sbt-idea-repo"         at "http://mpeltonen.github.com/maven/"
)

libraryDependencies <++= (scalaVersion) { scalaVersion =>
  Seq(
    // scope: provided
    "commons-dbcp"            %  "commons-dbcp"         % "1.4"      % "provided",
    "ch.qos.logback"          %  "logback-classic"      % "1.0.0"    % "provided",
    // scope: test
    "net.databinder"          %% "unfiltered-filter"    % "0.5.1"    % "test",
    "net.databinder"          %% "unfiltered-jetty"     % "0.5.1"    % "test",
    "net.databinder"          %% "unfiltered-spec"      % "0.5.1"    % "test",
    "net.databinder"          %% "unfiltered-scalate"   % "0.5.1"    % "test",
    "junit"                   %  "junit"                % "4.9"      % "test",
    "org.scalatest"           %% "scalatest"            % "1.6.1"    % "test",
    "org.scala-tools.testing" %% "scalacheck"           % "1.9"      % "test",
    "org.hsqldb"              %  "hsqldb"               % "[2,)"     % "test",
    "mysql"                   %  "mysql-connector-java" % "5.1.18"   % "test",
    "postgresql"              %  "postgresql"           % "9.1-901.jdbc3"  % "test"
  )
}

seq(lsSettings :_*)
