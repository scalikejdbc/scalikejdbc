resolvers ++= Seq(
  "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
)
// Don't forget adding your JDBC driver
libraryDependencies += "org.hsqldb" % "hsqldb" % "2.+"
addSbtPlugin("org.scalikejdbc" %% "scalikejdbc-mapper-generator" % "3.3.0")
