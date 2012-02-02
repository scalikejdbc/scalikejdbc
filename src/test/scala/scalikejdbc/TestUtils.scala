package scalikejdbc

import java.sql.{Connection, DriverManager}
import util.control.Exception._
import scalikejdbc.LoanPattern._

object TestUtils {

  def initializeEmpRecords(session: DBSession, tableName: String) {
    session.update("delete from " + tableName)
    session.update("insert into " + tableName + " (id, name) values (?, ?)", 1, "name1")
    session.update("insert into " + tableName + " (id, name) values (?, ?)", 2, "name2")
  }

  def initialize(conn: Connection, tableName: String) {
    new DB(conn) autoCommit {
      session => {
        def createTableAndInitialize() = {
          session.execute("create table " + tableName + " (id integer primary key, name varchar(30))")
          initializeEmpRecords(session, tableName)
        }
        val createTableAndInitializeIfNotExist = handling(classOf[Throwable]) by {
          t => createTableAndInitialize()
        }
        createTableAndInitializeIfNotExist {
          session.asOne("select count(1) from " + tableName) {
            rs => Some(rs.getInt(1))
          } match {
            case Some(2) =>
            case _ => createTableAndInitialize()
          }
        }
      }
    }
  }

  def deleteTable(conn: Connection, tableName: String): Unit = {
    using(conn) {
      conn => {
        new DB(conn) autoCommit {
          session =>
            ignoring(classOf[Throwable]) {
              session.execute("drop table " + tableName)
            }
        }
      }
    }
  }

}