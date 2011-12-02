resolvers ++= Seq(
  "seratch" at "http://seratch.github.com/mvn-repo/releases",
  "sbt-idea-repo" at "http://mpeltonen.github.com/maven/"
)

addSbtPlugin("com.github.seratch" %% "testgen-sbt" % "0.1")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "0.11.2")

resolvers ++= Seq(
  "less is" at "http://repo.lessis.me",
  "coda" at "http://repo.codahale.com")

addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.1")

