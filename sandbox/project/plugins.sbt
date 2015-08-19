resolvers ++= Seq(
  "Sonatype releases" at "http://oss.sonatype.org/content/repositories/releases",
  "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots"
)

// Don't forget adding your JDBC driver
libraryDependencies += "org.hsqldb" % "hsqldb" % "2.+"

addSbtPlugin("org.scalikejdbc" %% "scalikejdbc-mapper-generator" % "2.2.8")

