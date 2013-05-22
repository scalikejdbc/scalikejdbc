package scalikejdbc

import org.scalatest._
import org.joda.time._
import scalikejdbc.SQLInterpolation._

class QueryInterfaceSpec extends FlatSpec with Matchers with DBSettings {

  behavior of "QueryInterface"

  case class Order(id: Int, productId: Int, accountId: Option[Int], createdAt: DateTime, product: Option[Product] = None, account: Option[Account] = None)
  case class LegacyProduct(id: Option[Int], name: Option[String], price: Int)
  case class Product(id: Int, name: Option[String], price: Int)
  case class Account(id: Int, name: Option[String])

  object Order extends SQLSyntaxSupport[Order] {
    override val tableName = "qi_orders"
    def apply(o: SyntaxProvider[Order])(rs: WrappedResultSet): Order = apply(o.resultName)(rs)
    def apply(o: ResultName[Order])(rs: WrappedResultSet): Order = {
      new Order(rs.int(o.id), rs.int(o.productId), rs.intOpt(o.accountId), rs.timestamp(o.createdAt).toDateTime)
    }
    def apply(o: SyntaxProvider[Order], p: SyntaxProvider[Product])(rs: WrappedResultSet): Order = {
      (apply(o)(rs)).copy(product = Some(Product(p)(rs)))
    }
    def apply(o: SyntaxProvider[Order], p: SyntaxProvider[Product], a: SyntaxProvider[Account])(rs: WrappedResultSet): Order = {
      (apply(o)(rs)).copy(product = Some(Product(p)(rs)), account = Account.opt(a)(rs))
    }
  }
  object LegacyProduct extends SQLSyntaxSupport[LegacyProduct] {
    override val tableName = "qi_legacy_products"
  }
  object Product extends SQLSyntaxSupport[Product] {
    override val tableName = "qi_products"
    def apply(p: SyntaxProvider[Product])(rs: WrappedResultSet): Product = apply(p.resultName)(rs)
    def apply(p: ResultName[Product])(rs: WrappedResultSet): Product = new Product(rs.int(p.id), rs.stringOpt(p.name), rs.int(p.price))
  }
  object Account extends SQLSyntaxSupport[Account] {
    override val tableName = "qi_accounts"
    def apply(a: SyntaxProvider[Account])(rs: WrappedResultSet): Account = apply(a.resultName)(rs)
    def apply(a: ResultName[Account])(rs: WrappedResultSet): Account = new Account(rs.int(a.id), rs.stringOpt(a.name))
    def opt(a: SyntaxProvider[Account])(rs: WrappedResultSet): Option[Account] = rs.intOpt(a.resultName.id).map(_ => apply(a)(rs))
  }

  it should "be available with Query Interface" in {
    try {
      DB localTx { implicit s =>
        sql"create table ${Order.table} (id int not null, product_id int not null, account_id int, created_at timestamp not null)".execute.apply()
        sql"create table ${Product.table} (id int not null, name varchar(256), price int not null)".execute.apply()
        sql"create table ${LegacyProduct.table} (id int, name varchar(256), price int not null)".execute.apply()
        sql"create table ${Account.table} (id int not null, name varchar(256))".execute.apply()
      }
      DB localTx { implicit s =>

        // insert test data
        val (oc, pc, ac) = (Order.column, Product.column, Account.column)
        val lp = LegacyProduct.syntax("lp")
        Seq(
          insert.into(Account).columns(ac.id, ac.name).values(1, "Alice"),
          insert.into(Account).columns(ac.id, ac.name).values(2, "Bob"),
          insert.into(Account).columns(ac.id, ac.name).values(3, "Chris"),
          insert.into(LegacyProduct).values(None, "tmp", 777),
          insert.into(LegacyProduct).values(Some(100), "Old Cookie", 40),
          insert.into(LegacyProduct).values(Some(200), "Green Tea", 20),
          insert.into(Product).values(1, "Cookie", 120),
          insert.into(Product).values(2, "Tea", 80),
          insert.into(Product).select(_.from(LegacyProduct as lp).where.isNotNull(lp.id)),
          insert.into(Product).select(lp.id, lp.name, lp.price)(_.from(LegacyProduct as lp).where.isNotNull(lp.id)),
          insert.into(Product).selectAll(lp)(_.from(LegacyProduct as lp).where.isNotNull(lp.id)),
          delete.from(Product).where.in(pc.id, Seq(100, 200)),
          insert.into(Order).values(11, 1, Some(1), DateTime.now),
          insert.into(Order).values(12, 1, Some(2), DateTime.now),
          insert.into(Order).values(13, 1, Some(3), DateTime.now),
          insert.into(Order).values(14, 1, Some(1), DateTime.now),
          insert.into(Order).values(15, 1, Some(1), DateTime.now),
          insert.into(Order).values(21, 2, Some(2), DateTime.now),
          insert.into(Order).values(22, 2, Some(2), DateTime.now),
          insert.into(Order).values(23, 2, Some(2), DateTime.now),
          insert.into(Order).values(24, 2, Some(1), DateTime.now),
          insert.into(Order).values(25, 2, Some(3), DateTime.now),
          insert.into(Order).values(26, 2, None, DateTime.now)
        ).foreach(sql => applyUpdate(sql))

        val (o, p, a) = (Order.syntax("o"), Product.syntax("p"), Account.syntax("a"))

        // simple query
        val alice: Account = withSQL(select.from(Account as a).where.eq(a.name, "Alice")).map(Account(a)).single.apply().get
        val ordersByAlice = withSQL {
          select.from(Order as o).where.eq(o.accountId, alice.id)
        }.map(Order(o)).list.apply()

        ordersByAlice.size should equal(4)

        // join query
        val cookieOrders = withSQL {
          select
            .from(Order as o)
            .innerJoin(Product as p).on(o.productId, p.id)
            .leftJoin(Account as a).on(o.accountId, a.id)
            .where.eq(o.productId, 2)
            .orderBy(o.id).desc
            .limit(4)
            .offset(0)
        }.map(Order(o, p, a)).list.apply()

        cookieOrders.size should equal(4)
        cookieOrders(0).product.isEmpty should be(false)
        cookieOrders(0).account.isEmpty should be(true)
        cookieOrders(1).product.isEmpty should be(false)
        cookieOrders(1).account.isEmpty should be(false)

        // dynamic query

        def findCookieOrder(accountRequired: Boolean) = withSQL {
          select
            .from[Order](Order as o)
            .innerJoin(Product as p).on(o.productId, p.id)
            .map { sql => if (accountRequired) sql.leftJoin(Account as a).on(o.accountId, a.id) else sql }
            .where.eq(o.id, 13)
        }.map {
          rs => if (accountRequired) Order(o, p, a)(rs) else Order(o, p)(rs)
        }.single.apply()

        findCookieOrder(true).get.account.isEmpty should be(false)
        findCookieOrder(false).get.account.isEmpty should be(true)

        // sub-query, group by, having
        import sqls.{ sum, gt }
        val x = SubQuery.syntax("x").include(o, p)
        val preferredClients: List[(Int, Int)] = withSQL {
          select(sqls"${x(o).accountId} id", sqls"${sum(x(p).price)} amount")
            .from(select.from(Order as o).innerJoin(Product as p).on(o.productId, p.id).as(x))
            .groupBy(x(o).accountId)
            .having(gt(sum(x(p).price), 300))
            .orderBy(sqls"amount")
        }.map(rs => (rs.int("id"), rs.int("amount"))).list.apply()

        preferredClients.size should equal(2)
        preferredClients should equal(List((2, 360), (1, 440)))

        val bracketTestResults = withSQL {
          select(o.result.id)
            .from(Order as o)
            .where.withRoundBracket {
              _.eq(o.productId, 1).and.isNotNull(o.accountId)
            }.or.isNull(o.accountId)
            .orderBy(o.id)
        }.map(_.int(o.resultName.id)).list.apply()

        bracketTestResults should equal(List(11, 12, 13, 14, 15, 26))

        // in clause
        val inClauseResults = withSQL {
          select
            .from(Order as o)
            .where.in(o.id, Seq(1, 2, 14, 15, 16, 20, 21, 22))
            .orderBy(o.id)

        }.map(Order(o)).list.apply()

        inClauseResults.map(_.id) should equal(List(14, 15, 21, 22))

        // distinct count
        import sqls.{ distinct, count }
        val productCount = withSQL {
          select(count(distinct(o.productId))).from(Order as o)
        }.map(_.int(1)).single.apply().get

        productCount should equal(2)

        // update,delete
        // applyUpdate = withSQL { ... }.update.apply()

        // the folloing code becomes compilation error on Scala 2.10.1.
        // applyUpdate(update(Account as a).set(a.name -> "Bob Marley").where.eq(a.id, 2))
        /*
         [error]   scalikejdbc-interpolation/src/test/scala/scalikejdbc/QueryInterfaceSpec.scala:162: erroneous or inaccessible type
         [error]         applyExecute(update(Account as a).set(a.name -> "Bob Marley").where.eq(a.id, 2))
         [error]                                                                                  ^
         */
        // the following code works fine. Don't know why.
        val updateQuery = update(Account as a).set(a.name -> "Bob Marley").where.eq(a.id, 2)
        applyUpdate(updateQuery)
        // of course, this code also works fine.
        withSQL(update(Account as a).set(a.name -> "Bob Marley").where.eq(a.id, 2)).update.apply()

        val newName = withSQL { select.from(Account as a).where.eq(a.id, 2) }.map(Account(a)).single.apply().get.name
        newName should equal(Some("Bob Marley"))

        // compilation error since 2.10.1
        // applyUpdate { delete.from(Order).where.isNull(Order.column.accountId) } 
        withSQL { delete.from(Order).where.isNull(Order.column.accountId) }.update.apply()

        val noAccountIdOrderCount = withSQL {
          select(count).from(Order as o).where.isNull(o.accountId)
        }.map(_.long(1)).single.apply().get
        noAccountIdOrderCount should equal(0)

      }
    } catch {
      case e: Exception =>
        e.printStackTrace
        throw e
    } finally {
      DB localTx { implicit s =>
        try {
          sql"drop table ${Order.table}".execute.apply()
          sql"drop table ${LegacyProduct.table}".execute.apply()
          sql"drop table ${Product.table}".execute.apply()
          sql"drop table ${Account.table}".execute.apply()
        } catch { case e: Exception => }
      }
    }
  }

}

