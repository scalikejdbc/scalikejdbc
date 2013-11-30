resolvers ++= Seq(
  Classpaths.typesafeResolver,
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
)

addSbtPlugin("com.github.scct" %% "sbt-scct" % "0.2")

addSbtPlugin("com.github.theon" %% "xsbt-coveralls-plugin" % "0.0.5-SNAPSHOT")

