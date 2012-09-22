externalResolvers ~= (_.filter(_.name != "Scala-Tools Maven2 Repository"))

resolvers += "sonatype" at "http://oss.sonatype.org/content/repositories/releases/"

libraryDependencies += "org.hsqldb" % "hsqldb" % "[2,)"

addSbtPlugin("com.github.seratch" %% "scalikejdbc-mapper-generator" % "[1.3,)")

addSbtPlugin("com.github.seratch" %% "testgenerator" % "1.1.0")

