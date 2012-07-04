import sbt._
import Keys._

object MyBuild extends Build {

  lazy val root = Project("root", file("."), settings = mainSettings)

  lazy val mainSettings: Seq[Project.Setting[_]] = Defaults.defaultSettings ++ Seq(
    sbtPlugin := false,
    organization := "com.example",
    name := "testing-scalikejdbc-mapper-generator",
    version := "0.0.1",
    scalacOptions ++= Seq("-deprecation", "-unchecked"),
    scalaVersion := "2.9.2",
    externalResolvers ~= (_.filter(_.name != "Scala-Tools Maven2 Repository")),
    resolvers ++= Seq(
      "sonatype" at "http://oss.sonatype.org/content/repositories/releases/"
    ),
    libraryDependencies <++= (scalaVersion) { scalaVersion =>
      Seq(
        "com.github.seratch" %% "scalikejdbc" % "1.3.0",
        "org.slf4j" % "slf4j-simple" % "1.6.4",
        "org.hsqldb" % "hsqldb" % "[2,)",
        "org.scalatest" %% "scalatest" % "[1.7,)" % "test"
      )
    }
  )

}


