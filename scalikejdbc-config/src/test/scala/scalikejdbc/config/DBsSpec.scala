package scalikejdbc.config

import scalikejdbc._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class DBsSpec extends AnyFunSpec with Matchers {

  def fixture = new {}

  describe("DBs") {

    describe("#setup") {
      it("should setup default connection with no argument") {
        DBs.setup()
        val res = DB readOnly { implicit session =>
          SQL("SELECT 1 as one").map(rs => rs.int("one")).single.apply()
        }
        res should be(Some(1))
        DBs.close()
      }
      it("should setup a connection pool") {
        DBs.setup("foo")
        val res = NamedDB("foo") readOnly { implicit session =>
          SQL("SELECT 1 as one").map(rs => rs.int("one")).single.apply()
        }
        res should be(Some(1))
        DBs.close("foo")
      }
      it("should setup env & top level config") {
        DBs.setup("topLevelDefaults")
        val res = NamedDB("topLevelDefaults") readOnly { implicit session =>
          SQL("SELECT 1 as one").map(rs => rs.int("one")).single.apply()
        }
        res should be(Some(1))
        DBs.close("topLevelDefaults")
      }
      describe("When an unknown database name is passed") {
        it("throws Configuration Exception") {
          intercept[ConfigurationException] {
            DBs.setup("unknown")
          }
        }
      }
    }

    describe("#setupAll") {
      it("should read application.conf and setup all connection pool") {
        DBs.setupAll()
        val res = NamedDB("foo") readOnly { implicit session =>
          SQL("SELECT 1 as one").map(rs => rs.int("one")).single.apply()
        }
        res should be(Some(1))
        val res2 = NamedDB("bar") readOnly { implicit session =>
          SQL("SELECT 1 as one").map(rs => rs.int("one")).single.apply()
        }
        res2 should be(Some(1))
        DBs.closeAll()
      }
      it("should read application.conf with env (dev)") {
        DBsWithEnv("dev").setupAll()
        val res = DB readOnly { implicit session =>
          SQL("SELECT 1 as one").map(rs => rs.int("one")).single.apply()
        }
        res should be(Some(1))
        DBs.closeAll()
      }
      it("should read application.conf with env (dev2)") {
        DBsWithEnv("dev2").setupAll()
        val res = NamedDB("hocon") readOnly { implicit session =>
          SQL("SELECT 1 as one").map(rs => rs.int("one")).single.apply()
        }
        res should be(Some(1))
        DBs.closeAll()
      }
    }

    describe("#close") {
      describe("When no argument is passed") {
        it("should close default connection pool") {
          DBs.setup()
          DBs.setup("foo")
          DBs.close()
          intercept[IllegalStateException] {
            DB readOnly { implicit session =>
              SQL("SELECT 1 as one").map(rs => rs.int("one")).single.apply()
            }
          }
          val res = NamedDB("foo") readOnly { implicit session =>
            SQL("SELECT 1 as one").map(rs => rs.int("one")).single.apply()
          }
          res should be(Some(1))
          DBs.close("foo")
        }
      }

      it("should close a connection pool") {
        DBs.setup("foo")
        DBs.close("foo")
        intercept[IllegalStateException] {
          NamedDB("foo") readOnly { implicit session =>
            SQL("SELECT 1 as one").map(rs => rs.int("one")).single.apply()
          }
        }
      }
    }

    describe("#closeAll") {
      it("should close all connection pools") {
        DBs.setup("foo")
        DBs.setup("bar")
        DBs.closeAll()
        intercept[IllegalStateException] {
          NamedDB("foo") readOnly { implicit session =>
            SQL("SELECT 1 as one").map(rs => rs.int("one")).single.apply()
          }
        }
        intercept[IllegalStateException] {
          NamedDB("bar") readOnly { implicit session =>
            SQL("SELECT 1 as one").map(rs => rs.int("one")).single.apply()
          }
        }
      }
    }
  }

}
