resolvers ++= Seq(
  "seratch" at "http://seratch.github.com/mvn-repo/releases",
  "sbt-idea-repo" at "http://mpeltonen.github.com/maven/"
)

addSbtPlugin("com.github.seratch" %% "testgen-sbt" % "0.1")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "0.11.0")

