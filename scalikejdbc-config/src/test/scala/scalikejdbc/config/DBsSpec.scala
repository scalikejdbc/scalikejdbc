package scalikejdbc.config

import org.scalatest._
import scalikejdbc._

class DBsSpec extends FunSpec with Matchers {

  def fixture = new {

  }

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
        DBs.setup(Symbol("foo"))
        val res = NamedDB(Symbol("foo")) readOnly { implicit session =>
          SQL("SELECT 1 as one").map(rs => rs.int("one")).single.apply()
        }
        res should be(Some(1))
        DBs.close(Symbol("foo"))
      }
      it("should setup env & top level config") {
        DBs.setup(Symbol("topLevelDefaults"))
        val res = NamedDB(Symbol("topLevelDefaults")) readOnly { implicit session =>
          SQL("SELECT 1 as one").map(rs => rs.int("one")).single.apply()
        }
        res should be(Some(1))
        DBs.close(Symbol("topLevelDefaults"))
      }
      describe("When an unknown database name is passed") {
        it("throws Configuration Exception") {
          intercept[ConfigurationException] {
            DBs.setup(Symbol("unknown"))
          }
        }
      }
    }

    describe("#setupAll") {
      it("should read application.conf and setup all connection pool") {
        DBs.setupAll()
        val res = NamedDB(Symbol("foo")) readOnly { implicit session =>
          SQL("SELECT 1 as one").map(rs => rs.int("one")).single.apply()
        }
        res should be(Some(1))
        val res2 = NamedDB(Symbol("bar")) readOnly { implicit session =>
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
        val res = NamedDB(Symbol("hocon")) readOnly { implicit session =>
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
          DBs.setup(Symbol("foo"))
          DBs.close()
          intercept[IllegalStateException] {
            DB readOnly { implicit session =>
              SQL("SELECT 1 as one").map(rs => rs.int("one")).single.apply()
            }
          }
          val res = NamedDB(Symbol("foo")) readOnly { implicit session =>
            SQL("SELECT 1 as one").map(rs => rs.int("one")).single.apply()
          }
          res should be(Some(1))
          DBs.close(Symbol("foo"))
        }
      }

      it("should close a connection pool") {
        DBs.setup(Symbol("foo"))
        DBs.close(Symbol("foo"))
        intercept[IllegalStateException] {
          NamedDB(Symbol("foo")) readOnly { implicit session =>
            SQL("SELECT 1 as one").map(rs => rs.int("one")).single.apply()
          }
        }
      }
    }

    describe("#closeAll") {
      it("should close all connection pools") {
        DBs.setup(Symbol("foo"))
        DBs.setup(Symbol("bar"))
        DBs.closeAll()
        intercept[IllegalStateException] {
          NamedDB(Symbol("foo")) readOnly { implicit session =>
            SQL("SELECT 1 as one").map(rs => rs.int("one")).single.apply()
          }
        }
        intercept[IllegalStateException] {
          NamedDB(Symbol("bar")) readOnly { implicit session =>
            SQL("SELECT 1 as one").map(rs => rs.int("one")).single.apply()
          }
        }
      }
    }
  }

}
