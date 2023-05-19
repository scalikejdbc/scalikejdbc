package scalikejdbc

import util.control.Exception._

object TestUtils {

  def initializeEmpRecords(session: DBSession, tableName: String): Unit = {
    session.update("delete from " + tableName)
    session.update(
      "insert into " + tableName + " (id, name) values (?, ?)",
      1,
      "name1"
    )
    session.update(
      "insert into " + tableName + " (id, name) values (?, ?)",
      2,
      "name2"
    )
  }

  def initialize(tableName: String): Unit = {
    DB autoCommit { session =>
      handling(classOf[Throwable]) by { t =>
        try {
          session.execute(
            "create table " + tableName + " (id integer primary key, name varchar(30))"
          )
        } catch {
          case e: Exception =>
            session.execute(
              "create table " + tableName + " (id integer primary key, name varchar(30))"
            )
        }
        initializeEmpRecords(session, tableName)
      } apply {
        session.single("select count(1) from " + tableName)(_.int(1))
        initializeEmpRecords(session, tableName)
      }
    }
  }

  def deleteTable(tableName: String): Unit = {
    ignoring(classOf[Throwable]) {
      DB autoCommit { _.execute("drop table " + tableName) }
    }
  }

}
