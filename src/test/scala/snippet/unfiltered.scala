package snippet

import unfiltered.request._
import unfiltered.filter.Plan
import unfiltered.response.ResponseString

import javax.servlet._
import java.sql.DriverManager

import scalikejdbc._

object Settings {
  Class.forName("org.hsqldb.jdbc.JDBCDriver")
  val (url, user, password) = ("jdbc:hsqldb:mem:hsqldb:TxFilter", "", "")
}

import Settings._

class TxFilter extends Filter {

  def init(filterConfig: FilterConfig) {
    ConnectionPool.initialize(url, user, password)
  }

  def doFilter(req: ServletRequest, res: ServletResponse, chain: FilterChain) {

    // simply using DriverManager
    //    val conn = DriverManager.getConnection(url, user, password)

    // using Commons DBCP
    import scalikejdbc.LoanPattern._
    using(ConnectionPool.borrow()) {
      conn => {
        implicit val db = ThreadLocalDB.create(conn)
        db.begin()
        try {
          chain.doFilter(req, res)
          db.commit()
        } catch {
          case e: Exception => {
            db.rollbackIfActive()
            throw e
          }
        }
      }
    }

  }

  def destroy() {
  }

}

class Hello extends Plan {

  def intent = {
    case req@GET(Path("/rollback")) => {
      val db = ThreadLocalDB.load()
      db withinTx {
        session => {
          println(session.asOne("select name from emp where id = ?", 1) {
            rs => Some(rs.getString("name"))
          }.get)
          session.update("update emp set name = ? where id = ?", "rollback?", 1)
        }
      }
      throw new RuntimeException("rollback test")
      // will rollback.
    }
    case req@GET(Path("/commit")) => {
      val db = ThreadLocalDB.load()
      db withinTx {
        session => {
          println(session.asOne("select name from emp where id = ?", 1) {
            rs => Some(rs.getString("name"))
          }.get)
          val count = session.update("update emp set name = ? where id = ?", "commited", 1)
          ResponseString(count.toString)
        }
      }
    }
  }

  val conn = DriverManager.getConnection(url, user, password)
  val ddl = new DB(conn)
  ddl autoCommit {
    session => {
      try {
        session.execute("create table emp (id integer primary key, name varchar(30))")
      } catch {
        case _: Exception =>
      }
      session.execute("insert into emp (id, name) values (?, ?)", 1, "name1")
      session.execute("insert into emp (id, name) values (?, ?)", 2, "name2")
    }
  }

}

object Server extends App {
  unfiltered.jetty.Http.anylocal
    .filter(new TxFilter)
    .plan(new Hello)
    .run {
    s => unfiltered.util.Browser.open(
      "http://127.0.0.1:%d/rollback".format(s.port))
  }
}
