package snippet

import javax.servlet._
import java.sql.DriverManager

import unfiltered.request._
import unfiltered.filter._
import unfiltered.response._

import scala.util.control.Exception._
import scalikejdbc._

object Settings {
  Class.forName("org.hsqldb.jdbc.JDBCDriver")
  val (url, user, password) = ("jdbc:hsqldb:mem:hsqldb:TxFilter", "", "")
}

import Settings._

class TxFilter extends Filter {

  def init(filterConfig: FilterConfig) = {
    ConnectionPool.singleton(url, user, password)
  }

  def doFilter(req: ServletRequest, res: ServletResponse, chain: FilterChain) = {
    import scalikejdbc.LoanPattern._
    // using(java.sql.DriverManager.getConnection(url, user, password)) {
    using(ConnectionPool.borrow()) {
      conn =>
        {
          val db = ThreadLocalDB.create(conn)
          handling(classOf[Throwable]) by {
            case e: Exception => {
              db.rollbackIfActive()
              throw e
            }
          } apply {
            db.begin()
            chain.doFilter(req, res)
            db.commit()
          }
        }
    }
  }

  def destroy() = {}

}

class PlanWithTx extends Plan {

  def intent = {
    case req @ GET(Path("/rollback")) => {
      val db = ThreadLocalDB.load()
      db withinTx {
        session =>
          {
            println(session.single("select name from emp where id = ?", 1) {
              rs => rs.string("name")
            }.get)
            session.update("update emp set name = ? where id = ?", "rollback?", 1)
          }
      }
      throw new RuntimeException("rollback test")
      // will rollback.
    }
    case req @ GET(Path("/commit")) => {
      val db = ThreadLocalDB.load()
      db withinTx {
        session =>
          {
            println(session.single("select name from emp where id = ?", 1) {
              rs => rs.string("name")
            })
            val count = session.update("update emp set name = ? where id = ?", "commited", 1)
            ResponseString(count.toString)
          }
      }
    }
  }

  val conn = DriverManager.getConnection(url, user, password)
  val ddl = new DB(conn)
  ddl autoCommit {
    session =>
      {
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
    .plan(new PlanWithTx)
    .run {
      s => unfiltered.util.Browser.open("http://127.0.0.1:%d/rollback".format(s.port))
    }
}
