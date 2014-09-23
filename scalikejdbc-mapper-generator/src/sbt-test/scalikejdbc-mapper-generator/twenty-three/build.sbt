scalikejdbcSettings

val scalikejdbcVersion = System.getProperty("plugin.version")

scalacOptions ++= Seq("-Xlint", "-language:_", "-deprecation", "-unchecked", "-Xfatal-warnings")

libraryDependencies ++= Seq(
  "org.scalikejdbc" %% "scalikejdbc"      % scalikejdbcVersion,
  "org.scalikejdbc" %% "scalikejdbc-test" % scalikejdbcVersion % "test",
  "org.slf4j"       %  "slf4j-simple"     % "1.7.7",
  "com.h2database"  %  "h2"               % "1.4.177",
  "org.scalatest"   %% "scalatest"        % "2.1.5"            % "test",
  "org.specs2"      %% "specs2-core"      % "2.4.4"            % "test"
)

mainClass := Some("initializer")

