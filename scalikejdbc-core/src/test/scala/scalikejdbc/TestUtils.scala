package scalikejdbc

import util.control.Exception._

object TestUtils {

  def initializeEmpRecords(session: DBSession, tableName: String): Unit = {
    session.update("delete from " + tableName)
    session.update("insert into " + tableName + " (id, name, x_timestamp) values (?, ?, ?)", 1, "name1", java.time.Instant.now)
    session.update("insert into " + tableName + " (id, name, x_timestamp) values (?, ?, ?)", 2, "name2", java.time.Instant.now)
  }

  def initialize(tableName: String): Unit = {
    DB autoCommit {
      session =>
        handling(classOf[Throwable]) by {
          t =>
            try {
              session.execute("create table " + tableName + " (id integer primary key, name varchar(30), x_timestamp timestamp with time zone)")
            } catch {
              case e: Exception =>
                session.execute("create table " + tableName + " (id integer primary key, name varchar(30), x_timestamp timestamp with time zone)")
            }
            initializeEmpRecords(session, tableName)
        } apply {
          session.single("select count(1) from " + tableName)(rs => rs.int(1))
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
