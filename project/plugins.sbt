resolvers ++= Seq(
  "sonatype releases" at "http://oss.sonatype.org/content/repositories/releases",
  "sbt-idea repository" at "http://mpeltonen.github.com/maven/",
  "sbt-plugin-releases-for-travis-ci" at "http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/" // for Travis CI with sbt 0.11.3
)

addSbtPlugin("com.github.seratch" % "testgenerator" % "1.1.0")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.1.0")

addSbtPlugin("com.typesafe.sbtscalariform" % "sbtscalariform" % "0.5.1")

addSbtPlugin("com.jsuereth" % "xsbt-gpg-plugin" % "0.6")

