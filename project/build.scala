import sbt._
import Keys._

object ScalikeJDBCBuild extends Build {

  lazy val jdbcPattern = Project("scalikejdbc", file("."), settings = mainSettings)

  lazy val mainSettings: Seq[Project.Setting[_]] = Defaults.defaultSettings ++ Seq(
    sbtPlugin := false,
    organization := "com.github.seratch",
    name := "scalikejdbc",
    version := "0.3.0",
    publishTo <<= (version) { version: String =>
      Some(
        Resolver.file("GitHub Pages", Path.userHome / "github" / "seratch.github.com" / "mvn-repo" / {
          if (version.trim.endsWith("SNAPSHOT")) "snapshots" else "releases" 
        })
      )
    },
    /*
    publishTo := Some(
      Resolver.file(
        "Github Pages",
        Path.userHome / "github" / "seratch.github.com" / "mvn-repo" / "releases" asFile
      )
        (Patterns(true, Resolver.mavenStyleBasePattern))
    ),
    */
    publishMavenStyle := true,
    scalacOptions ++= Seq("-deprecation", "-unchecked")
  )

}


