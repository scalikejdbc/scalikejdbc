resolvers ++= Seq(
  "sonatype releases" at "http://oss.sonatype.org/content/repositories/releases",
  "sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots"
)

libraryDependencies += "org.hsqldb" % "hsqldb" % "[2,)"

addSbtPlugin("org.scalikejdbc" %% "scalikejdbc-mapper-generator" % "1.7.4")

PSHOT")

