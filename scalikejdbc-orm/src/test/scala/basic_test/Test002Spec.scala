package basic_test

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{ Tag => _ }
import scalikejdbc._
import scalikejdbc.orm.{ Alias, NoIdCRUDMapper }
import util.DBSeeds

class Test002Spec extends AnyFunSpec with Matchers with DBSeeds {

  Class.forName("org.h2.Driver")
  ConnectionPool.add(
    "test002",
    "jdbc:h2:mem:test002;MODE=PostgreSQL",
    "sa",
    "sa"
  )

  override val dbSeedsAutoSession: DBSession = NamedAutoSession("test002")

  addSeedSQL(
    sql"""
create table account (
  name varchar(128) not null)
"""
  )
  runIfFailed(sql"select count(1) from account")

  case class Account(name: String)

  object Account extends NoIdCRUDMapper[Account] {
    override def connectionPoolName: Any = "test002"

    override def tableName = "account"
    override def defaultAlias: Alias[Account] = createAlias("a")
    override def extract(
      rs: WrappedResultSet,
      n: ResultName[Account]
    ): Account = new Account(rs.get(n.name))
  }

  def fixture(implicit session: DBSession): Unit = {
    Account.createWithAttributes("name" -> "Alice")
    Account.createWithAttributes("name" -> "Bob")
  }

  describe("The test") {
    it("should work as expected") {
      NamedDB("test002").localTx { implicit s =>
        fixture(s)

        val a = Account.defaultAlias

        {
          val accounts = Account.findAll()
          accounts.size should equal(2)
        }
        {
          val accounts = Account.findAll(Seq(a.name))
          accounts.size should equal(2)
          accounts.map(_.name) should equal(Seq("Alice", "Bob"))
        }
        {
          val accounts = Account.findAll(Seq(a.name.desc))
          accounts.size should equal(2)
          accounts.map(_.name) should equal(Seq("Bob", "Alice"))
        }

        {
          val accounts = Account.findAllBy(sqls.eq(a.name, "Bob"))
          accounts.size should equal(1)
        }
        {
          val accounts = Account.findAllBy(sqls.eq(a.name, "Bob"), Seq(a.name))
          accounts.size should equal(1)
          accounts.map(_.name) should equal(Seq("Bob"))
        }
        {
          val accounts =
            Account.findAllBy(sqls.eq(a.name, "Bob"), Seq(a.name.desc))
          accounts.size should equal(1)
          accounts.map(_.name) should equal(Seq("Bob"))
        }
      }
    }
  }
}
