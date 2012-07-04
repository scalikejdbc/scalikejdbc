package scalikejdbc

import scalikejdbc._

import org.specs2.mutable.Specification

import play.api._
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import play.api.Play.current

object PlayPluginSpec extends Specification {

  def fakeApp = FakeApplication(
    withoutPlugins = Seq("play.api.cache.EhCachePlugin"),
    additionalPlugins = Seq("scalikejdbc.PlayPlugin"),
    additionalConfiguration = Map(
      "logger.root" -> "INFO",
      "logger.play" -> "INFO",
      "logger.application" -> "DEBUG",
      "dbplugin" -> "disabled",
      "evolutionplugin" -> "disabled",
      "db.default.driver" -> "org.h2.Driver",
      "db.default.url" -> "jdbc:h2:mem:play",
      "db.default.user" -> "sa",
      "db.default.password" -> "sa",
      "db.default.schema" -> "",
      "db.global.loggingSQLAndTime.enabled" -> "true",
      "db.global.loggingSQLAndTime.logLevel" -> "debug",
      "db.global.loggingSQLAndTime.warningEnabled" -> "true",
      "db.global.loggingSQLAndTime.warningThreasholdMillis" -> "1",
      "db.global.loggingSQLAndTime.warningLogLevel" -> "warn",
      "scalikejdbc.global.loggingSQLAndTime.enabled" -> "true",
      "scalikejdbc.global.loggingSQLAndTime.logLevel" -> "debug",
      "scalikejdbc.global.loggingSQLAndTime.warningEnabled" -> "true",
      "scalikejdbc.global.loggingSQLAndTime.warningThreasholdMillis" -> "1",
      "scalikejdbc.global.loggingSQLAndTime.warningLogLevel" -> "warn"
    )
  )

  lazy val fakeAppWithDBPlugin = FakeApplication(
    withoutPlugins = Seq("play.api.cache.EhCachePlugin"),
    additionalPlugins = Seq("scalikejdbc.PlayPlugin"),
    additionalConfiguration = Map(
      "db.default.driver" -> "org.h2.Driver",
      "db.default.url" -> "jdbc:h2:mem:play",
      "db.default.user" -> "sa",
      "db.default.password" -> "sa",
      "db.default.schema" -> "",
      "scalikejdbc.global.loggingSQLAndTime.enabled" -> "true",
      "scalikejdbc.global.loggingSQLAndTime.logLevel" -> "debug",
      "scalikejdbc.global.loggingSQLAndTime.warningEnabled" -> "true",
      "scalikejdbc.global.loggingSQLAndTime.warningThreasholdMillis" -> "1",
      "scalikejdbc.global.loggingSQLAndTime.warningLogLevel" -> "warn"
    )
  )

  def plugin = fakeApp.plugin[PlayPlugin].get

  def simpleTest(table: String) = {
    try {
      case class User(id: Long, name: Option[String])
      DB autoCommit { implicit s =>
        SQL("DROP TABLE " + table + " IF EXISTS").execute.apply()
        SQL("CREATE TABLE " + table + " (ID BIGINT PRIMARY KEY NOT NULL, NAME VARCHAR(256))").execute.apply()
        val insert = SQL("INSERT INTO " + table + " (ID, NAME) VALUES (/*'id*/123, /*'name*/'Alice')")
        insert.bindByName('id -> 1, 'name -> "Alice").update.apply()
        insert.bindByName('id -> 2, 'name -> "Bob").update.apply()
        insert.bindByName('id -> 3, 'name -> "Eve").update.apply()
      }
      val users = DB readOnly { implicit s =>
        SQL("SELECT * FROM " + table).map(rs => User(rs.long("id"), Option(rs.string("name")))).list.apply()
      }
      users.size should equalTo(3)
    } finally {
      DB autoCommit { implicit s =>
        SQL("DROP TABLE " + table + " IF EXISTS").execute.apply()
      }
    }
  }

  "Play plugin" should {

    "be available when DB plugin is not active" in {
      running(fakeApp) { simpleTest("user_1") }
      running(fakeApp) { simpleTest("user_2") }
      running(fakeApp) { simpleTest("user_3") }
    }

    "be available when DB plugin is also active" in {
      running(fakeAppWithDBPlugin) { simpleTest("user_withdbplugin") }
    }

  }

}

