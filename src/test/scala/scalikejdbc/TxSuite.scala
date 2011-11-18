package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import java.sql._

@RunWith(classOf[JUnitRunner])
class TxSuite extends FunSuite with ShouldMatchers {

  type ? = this.type // for IntelliJ IDEA

  Class.forName("org.hsqldb.jdbc.JDBCDriver")
  val url = "jdbc:hsqldb:mem:hsqldb:LoanPatternSuite"
  val user = ""
  val password = ""

  test("available") {
    val conn = DriverManager.getConnection(url, user, password)
    val tx = new Tx(conn)
    tx should not be null
  }

  test("begin") {
    val conn = DriverManager.getConnection(url, user, password)
    val tx = new Tx(conn)
    tx.begin()
    tx.rollbackIfActive()
  }

  test("commit") {
    val conn = DriverManager.getConnection(url, user, password)
    val tx = new Tx(conn)
    tx.begin()
    tx.commit()
  }

  test("rollback") {
    val conn = DriverManager.getConnection(url, user, password)
    val tx = new Tx(conn)
    tx.begin()
    tx.rollback()
  }

}
