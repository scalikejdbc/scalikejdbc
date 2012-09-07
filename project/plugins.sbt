resolvers ++= Seq(
  "sonatype releases" at "http://oss.sonatype.org/content/repositories/releases",
  "sbt-idea repository" at "http://mpeltonen.github.com/maven/"
)

addSbtPlugin("com.github.seratch" % "testgenerator" % "1.1.0")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.1.0")

addSbtPlugin("com.typesafe.sbtscalariform" % "sbtscalariform" % "0.5.1")

resolvers += Resolver.url("sbt-plugin-releases for Travis CI", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.jsuereth" % "xsbt-gpg-plugin" % "0.6")

