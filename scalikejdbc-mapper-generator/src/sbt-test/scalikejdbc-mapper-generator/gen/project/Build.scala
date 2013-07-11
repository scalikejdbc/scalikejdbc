import sbt._
import Keys._

import scalikejdbc._

object AppBuild extends Build {

  val driverName = "org.hsqldb.jdbc.JDBCDriver"
  val jdbcURL = "jdbc:hsqldb:file:db/test"
  val user = ""
  val password = ""

  def prepare = Command.command("prepare") { state =>
    // creating users table
    Class.forName(driverName)
    try{
      ConnectionPool.singleton(jdbcURL, user, password)
      DB autoCommit { implicit s =>
        try {
          SQL("select count(1) from users").map(rs => rs.long(0)).single.apply()
        } catch { case e =>
          SQL("create table users(id int generated always as identity primary key, name varchar(30) not null)").execute.apply()
        }
      }
    } finally ConnectionPool.closeAll()
    state
  }

  lazy val root = Project("root", file("."), settings = mainSettings)

  lazy val scalikejdbcVersion = "1.6.5-SNAPSHOT"

  def invoke(loader: ClassLoader, className: String, methodName: String, params: AnyRef*){
    val clazz = loader.loadClass(className + "$")
    val method = clazz.getMethod(methodName, params.map(_.getClass) :_*)
    method.invoke(clazz.getField("MODULE$").get(null), params :_*)
  }

  lazy val mainSettings: Seq[Project.Setting[_]] = Defaults.defaultSettings ++ Seq(
    commands ++= Seq(prepare),
    sbtPlugin := false,
    organization := "com.github.seratch",
    name := "scalikejdbc-mapper-generator-scripted",
    version := "0.0.1",
    scalaVersion := "2.9.2",
    scalacOptions ++= Seq("-deprecation", "-unchecked"),
    resolvers ++= Seq(
      "sonatype" at "http://oss.sonatype.org/content/repositories/releases/"
    ),
    testOptions in Test += Tests.Setup{ loader =>
      invoke(loader, "app.models.test.Spec", "before", driverName, jdbcURL, user, password)
    },
    testOptions in Test += Tests.Cleanup{ loader =>
      invoke(loader, "app.models.test.Spec", "after")
    },
    parallelExecution := false,
    libraryDependencies <++= (scalaVersion) { scalaVersion =>
      Seq(
        "com.github.seratch" %% "scalikejdbc" % scalikejdbcVersion,
        "com.github.seratch" %% "scalikejdbc-test" % scalikejdbcVersion % "test",
        "org.slf4j" % "slf4j-simple" % "1.6.4",
        "org.hsqldb" % "hsqldb" % "[2,)",
        "org.scalatest" %% "scalatest" % "[1.7,)" % "test",
        "org.specs2" %% "specs2" % "1.12.4.1" % "test"
      )
    }
  )

}
