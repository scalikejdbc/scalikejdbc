import sbt._
import Keys._

import scalikejdbc._

object AppBuild extends Build {

  def prepare = Command.command("prepare") { state =>
    // creating users table
    Class.forName("org.hsqldb.jdbc.JDBCDriver")
    ConnectionPool.singleton("jdbc:hsqldb:file:db/test", "", "")
    DB autoCommit { implicit s =>
      try {
        SQL("select count(1) from users").map(rs => rs.long(0)).single.apply()
      } catch { case e =>
        SQL("create table users(id int generated always as identity, name varchar(30) not null)").execute.apply()
      }
    }
    state
  }

  lazy val root = Project("root", file("."), settings = mainSettings)

  lazy val mainSettings: Seq[Project.Setting[_]] = Defaults.defaultSettings ++ Seq(
    commands ++= Seq(prepare),
    sbtPlugin := false,
    organization := "com.github.seratch",
    name := "scalikejdbc-mapper-generator-scripted",
    version := "0.0.1",
    scalacOptions ++= Seq("-deprecation", "-unchecked"),
    resolvers ++= Seq(
      "sonatype" at "http://oss.sonatype.org/content/repositories/releases/"
    ),
    libraryDependencies <++= (scalaVersion) { scalaVersion =>
      Seq(
        "com.github.seratch" %% "scalikejdbc" % "1.4.9-SNAPSHOT",
        "org.slf4j" % "slf4j-simple" % "1.6.4",
        "org.hsqldb" % "hsqldb" % "[2,)",
        "org.scalatest" %% "scalatest" % "[1.7,)" % "test"
      )
    }
  )

}
