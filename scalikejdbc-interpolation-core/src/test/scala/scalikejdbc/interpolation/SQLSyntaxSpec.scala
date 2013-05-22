package scalikejdbc.interpolation

import org.scalatest._
import org.scalatest.matchers._

class SQLSyntaxSpec extends FlatSpec with Matchers {

  import Implicits._

  behavior of "SQLSyntax"

  it should "be available" in {
    SQLSyntax("where") should not be (null)
  }

  it should "have #join" in {
    val s = SQLSyntax.join(Seq(sqls"id", sqls"name"), sqls"and")
    s.value should equal("id and name")
    s.parameters should equal(Nil)
  }

  it should "have #csv" in {
    val (id, name) = (123, "Alice")
    val s = SQLSyntax.csv(sqls"id = ${id}", sqls"name = ${name}")
    s.value should equal("id = ? , name = ?")
    s.parameters should equal(Seq(123, "Alice"))
  }

  it should "have #eq" in {
    val s = SQLSyntax.eq(sqls"id", 123)
    s.value should equal(" id = ?")
    s.parameters should equal(Seq(123))
  }

  it should "have #eq and #ne" in {
    val s = SQLSyntax.eq(sqls"id", 123).and.ne(sqls"name", "Alice")
    s.value should equal(" id = ? and name <> ?")
    s.parameters should equal(Seq(123, "Alice"))
  }

  it should "have #gt" in {
    val s = SQLSyntax.gt(sqls"amount", 200)
    s.value should equal(" amount > ?")
    s.parameters should equal(Seq(200))
  }

  it should "have #ge" in {
    val s = SQLSyntax.ge(sqls"amount", 200)
    s.value should equal(" amount >= ?")
    s.parameters should equal(Seq(200))
  }

  it should "have #lt" in {
    val s = SQLSyntax.lt(sqls"amount", 200)
    s.value should equal(" amount < ?")
    s.parameters should equal(Seq(200))
  }

  it should "have #le" in {
    val s = SQLSyntax.le(sqls"amount", 200)
    s.value should equal(" amount <= ?")
    s.parameters should equal(Seq(200))
  }

  it should "have #isNull" in {
    val s = SQLSyntax.isNull(sqls"amount")
    s.value should equal(" amount is null")
    s.parameters should equal(Nil)
  }

  it should "have #isNotNull" in {
    val s = SQLSyntax.isNotNull(sqls"amount")
    s.value should equal(" amount is not null")
    s.parameters should equal(Nil)
  }

  it should "have #between" in {
    val s = SQLSyntax.between(1, 2)
    s.value should equal(" between ? and ?")
    s.parameters should equal(Seq(1, 2))
  }

  it should "have #in" in {
    val s = SQLSyntax.in(sqls"id", Seq(1, 2, 3))
    s.value should equal(" id in (?, ?, ?)")
    s.parameters should equal(Seq(1, 2, 3))
  }

  it should "have #groupBy and #having" in {
    val groupId = 123
    val s = SQLSyntax.groupBy(sqls"name").having(sqls"group_id = ${groupId}")
    s.value should equal(" group by name having group_id = ?")
    s.parameters should equal(Seq(123))
  }

  it should "have #orderBy" in {
    val s = SQLSyntax.orderBy(sqls"id")
    s.value should equal(" order by id")
    s.parameters should equal(Nil)
  }

  it should "have #orderBy and #asc" in {
    val s = SQLSyntax.orderBy(sqls"id").asc
    s.value should equal(" order by id asc")
    s.parameters should equal(Nil)
  }

  it should "have #orderBy and #desc" in {
    val s = SQLSyntax.orderBy(sqls"id").desc
    s.value should equal(" order by id desc")
    s.parameters should equal(Nil)
  }

  it should "have #limit and #offset" in {
    val s = SQLSyntax.limit(10).offset(20)
    s.value should equal(" limit 10 offset 20")
    s.parameters should equal(Nil)
  }

  it should "have #where" in {
    val s = SQLSyntax.where
    s.value should equal(" where")
    s.parameters should equal(Nil)
  }

  it should "have #where(SQLSyntax)" in {
    val id = 123
    val s = SQLSyntax.where(sqls"id = ${id}")
    s.value should equal(" where id = ?")
    s.parameters should equal(Seq(123))
  }

  it should "have #and" in {
    val (id, name) = (123, "Alice")
    val s = SQLSyntax.eq(sqls"id", id).and.eq(sqls"name", name)
    s.value should equal(" id = ? and name = ?")
    s.parameters should equal(Seq(123, "Alice"))
  }

  it should "have #or" in {
    val (id, name) = (123, "Alice")
    val s = SQLSyntax.eq(sqls"id", id).or.eq(sqls"name", name)
    s.value should equal(" id = ? or name = ?")
    s.parameters should equal(Seq(123, "Alice"))
  }

}
