import sbt._
import Keys._

object ScalikeJDBCBuild extends Build {

  lazy val jdbcPattern = Project("scalikejdbc", file("."), settings = mainSettings)

  lazy val mainSettings: Seq[Project.Setting[_]] = Defaults.defaultSettings ++ Seq(
    sbtPlugin := false,
    organization := "com.github.seratch",
    name := "scalikejdbc",
    version := "0.1.1",
    publishTo := Some(
      Resolver.file(
        "Github Pages",
        Path.userHome / "github" / "seratch.github.com" / "mvn-repo" / "releases" asFile
      )
        (Patterns(true, Resolver.mavenStyleBasePattern))
    ),
    publishMavenStyle := true,
    scalacOptions ++= Seq("-deprecation", "-unchecked")
  )

}


