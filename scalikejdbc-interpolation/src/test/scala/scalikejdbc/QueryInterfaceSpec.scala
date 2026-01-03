package scalikejdbc

import java.time.format.DateTimeFormatter

import java.time.{ ZoneId, ZonedDateTime }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class QueryInterfaceSpec
  extends AnyFlatSpec
  with Matchers
  with DBSettings
  with SQLInterpolation {

  def isH2: Boolean = driverClassName == "org.h2.Driver" || sys.env
    .get("SCALIKEJDBC_DATABASE")
    .exists(_ == "h2")

  def isMySQL: Boolean =
    Set("com.mysql.jdbc.Driver", "com.mysql.cj.jdbc.Driver").apply(
      driverClassName
    ) || sys.env
      .get("SCALIKEJDBC_DATABASE")
      .exists(_ == "mysql")

  behavior of "QueryInterface"

  case class Price(value: Int)
  object Price {
    implicit val binder: TypeBinder[Price] = TypeBinder.int.map(Price.apply)
    implicit val unbinder: ParameterBinderFactory[Price] =
      ParameterBinderFactory.intParameterBinderFactory.contramap(_.value)
  }
  case class Order(
    id: Int,
    productId: Int,
    accountId: Option[Int],
    createdAt: ZonedDateTime,
    product: Option[Product] = None,
    account: Option[Account] = None
  )
  case class LegacyProduct(id: Option[Int], name: Option[String], price: Int)
  case class Product(id: Int, name: Option[String], price: Price)
  case class Account(id: Int, name: Option[String])
  case class SchemaExample(id: Int)
  case class TimeHolder(id: Int, time: ZonedDateTime)

  object Order extends SQLSyntaxSupport[Order] {
    override val tableName = "qi_orders"
    def apply(o: SyntaxProvider[Order])(rs: WrappedResultSet): Order =
      apply(o.resultName)(rs)
    def apply(o: ResultName[Order])(rs: WrappedResultSet): Order = {
      new Order(
        rs.int(o.id),
        rs.int(o.productId),
        rs.intOpt(o.accountId),
        rs.zonedDateTime(o.createdAt)
      )
    }
    def apply(o: SyntaxProvider[Order], p: SyntaxProvider[Product])(
      rs: WrappedResultSet
    ): Order = {
      apply(o)(rs).copy(product = Some(Product(p)(rs)))
    }
    def apply(
      o: SyntaxProvider[Order],
      p: SyntaxProvider[Product],
      a: SyntaxProvider[Account]
    )(rs: WrappedResultSet): Order = {
      apply(o)(rs)
        .copy(product = Some(Product(p)(rs)), account = Account.opt(a)(rs))
    }
  }
  object LegacyProduct extends SQLSyntaxSupport[LegacyProduct] {
    override val tableName = "qi_legacy_products"
  }
  object Product extends SQLSyntaxSupport[Product] {
    override val tableName = "qi_products"
    def apply(p: SyntaxProvider[Product])(rs: WrappedResultSet): Product =
      apply(p.resultName)(rs)
    def apply(p: ResultName[Product])(rs: WrappedResultSet): Product =
      new Product(rs.int(p.id), rs.stringOpt(p.name), rs.get(p.price))
  }
  object Account extends SQLSyntaxSupport[Account] {
    override val tableName = "qi_accounts"
    def apply(a: SyntaxProvider[Account])(rs: WrappedResultSet): Account =
      apply(a.resultName)(rs)
    def apply(a: ResultName[Account])(rs: WrappedResultSet): Account =
      new Account(rs.int(a.id), rs.stringOpt(a.name))
    def opt(a: SyntaxProvider[Account])(rs: WrappedResultSet): Option[Account] =
      rs.intOpt(a.resultName.id).map(_ => apply(a)(rs))
  }
  object SchemaExample extends SQLSyntaxSupport[SchemaExample] {
    override val schemaName: Option[String] = Some("public")
    override val tableName = "qi_schema_example"
  }
  object TimeHolder extends SQLSyntaxSupport[TimeHolder] {
    override val tableName = "qi_time_holder"
    def apply(a: SyntaxProvider[TimeHolder])(rs: WrappedResultSet): TimeHolder =
      apply(a.resultName)(rs)
    def apply(p: ResultName[TimeHolder])(rs: WrappedResultSet): TimeHolder =
      new TimeHolder(rs.int(p.id), rs.zonedDateTime(p.time))
  }

  it should "support schemaName" in {
    if (!isMySQL) {
      try {
        DB autoCommit { implicit s =>
          try sql"drop table ${SchemaExample.table}".execute.apply()
          catch { case e: Exception => }
          sql"create table ${SchemaExample.table} (id int not null)".execute
            .apply()
          withSQL { insert.into(SchemaExample).values(1) }.update.apply()
          val se = SchemaExample.syntax("se")
          select(sqls.count)
            .from(SchemaExample as se)
            .toSQL
            .statement should equal(
            "select count(1) from public.qi_schema_example se"
          )

          val count = withSQL { select(sqls.count).from(SchemaExample as se) }
            .map(_.long(1))
            .single
            .apply()
            .get
          count should equal(1L)
        }
      } finally {
        DB autoCommit { implicit s =>
          sql"drop table ${SchemaExample.table}".execute.apply()
        }
      }
    }
  }

  it should "be available with Query Interface" in {
    try {
      DB autoCommit { implicit s =>
        try sql"drop table ${Order.table}".execute.apply()
        catch { case e: Exception => }
        sql"create table ${Order.table} (id int not null, product_id int not null, account_id int, created_at timestamp not null)".execute
          .apply()

        try sql"drop table ${Product.table}".execute.apply()
        catch { case e: Exception => }
        sql"create table ${Product.table} (id int not null, name varchar(256), price int not null)".execute
          .apply()

        try sql"drop table ${LegacyProduct.table}".execute.apply()
        catch { case e: Exception => }
        sql"create table ${LegacyProduct.table} (id int, name varchar(256), price int not null)".execute
          .apply()

        try sql"drop table ${Account.table}".execute.apply()
        catch { case e: Exception => }
        sql"create table ${Account.table} (id int not null, name varchar(256))".execute
          .apply()
      }

      DB localTx { implicit s =>
        // insert test data
        val (oc, pc, ac) = (Order.column, Product.column, Account.column)
        val lp = LegacyProduct.syntax("lp")
        Seq(
          insert.into(Account).columns(ac.id, ac.name).values(1, "Alice"),
          insert.into(Account).columns(ac.id, ac.name).values(2, "Bob"),
          insert.into(Account).columns(ac.id, ac.name).values(3, "Chris"),
          insert.into(Account).namedValues(ac.id -> 4, ac.name -> "Debian"),
          insert.into(LegacyProduct).values(None, "tmp", 777),
          insert.into(LegacyProduct).values(Some(100), "Old Cookie", 40),
          insert.into(LegacyProduct).values(Some(200), "Green Tea", 20),
          insert.into(Product).values(1, "Cookie", 120),
          insert
            .into(Product)
            .namedValues(pc.id -> 2, pc.name -> "Tea", pc.price -> Price(80)),
          insert
            .into(Product)
            .select(_.from(LegacyProduct as lp).where.isNotNull(lp.id)),
          insert
            .into(Product)
            .select(lp.id, lp.name, lp.price)(
              _.from(LegacyProduct as lp).where.isNotNull(lp.id)
            ),
          insert
            .into(Product)
            .selectAll(lp)(_.from(LegacyProduct as lp).where.isNotNull(lp.id)),
          delete.from(Product).where.in(pc.id, Seq(100, 200)),
          insert.into(Order).values(11, 1, Some(1), ZonedDateTime.now),
          insert.into(Order).values(12, 1, Some(2), ZonedDateTime.now),
          insert.into(Order).values(13, 1, Some(3), ZonedDateTime.now),
          insert.into(Order).values(14, 1, Some(1), ZonedDateTime.now),
          insert.into(Order).values(15, 1, Some(1), ZonedDateTime.now),
          insert.into(Order).values(21, 2, Some(2), ZonedDateTime.now),
          insert.into(Order).values(22, 2, Some(2), ZonedDateTime.now),
          insert.into(Order).values(23, 2, Some(2), ZonedDateTime.now),
          insert.into(Order).values(24, 2, Some(1), ZonedDateTime.now),
          insert.into(Order).values(25, 2, Some(3), ZonedDateTime.now),
          insert.into(Order).values(26, 2, None, ZonedDateTime.now)
        ).foreach(sql => applyUpdate(sql))

        {
          val p = Product.syntax("p")
          val products = withSQL {
            select.from(Product as p).orderBy(p.id)
          }.map(Product(p)).list.apply()
          assert(
            products === List(
              Product(1, Some("Cookie"), Price(120)),
              Product(2, Some("Tea"), Price(80))
            )
          )
        }

        // batch insert
        val batchInsertQuery = withSQL {
          insert
            .into(Product)
            .columns(pc.id, pc.name, pc.price)
            .values(sqls.?, sqls.?, sqls.?)
        }
        batchInsertQuery
          .batch(Seq(3, "Coffee", 90), Seq(4, "Chocolate", 200))
          .apply()

        withSQL { delete.from(Product).where.in(pc.id, Seq(3, 4)) }.update
          .apply()

        // batch insert with BatchParamsBuilder
        {
          val products: Seq[Product] = Seq(
            Product(3, Some("Coffee"), Price(90)),
            Product(4, Some("Coffee"), Price(200))
          )
          val params = BatchParamsBuilder {
            products.map { product =>
              Seq(
                pc.id -> product.id,
                pc.name -> product.name,
                pc.price -> product.price
              )
            }
          }
          withSQL {
            insert.into(Product).namedValues(params.columnsAndPlaceholders*)
          }.batch(params.batchParams*).apply()

          withSQL { delete.from(Product).where.in(pc.id, Seq(3, 4)) }.update
            .apply()
        }

        val (o, p, a) =
          (Order.syntax("o"), Product.syntax("p"), Account.syntax("a"))

        // simple query
        val alice: Account = withSQL(
          select.from(Account as a).where.eq(a.name, "Alice")
        ).map(Account(a)).single.apply().get
        val ordersByAlice = withSQL {
          select.from(Order as o).where.eq(o.accountId, alice.id)
        }.map(Order(o)).list.apply()

        ordersByAlice.size should equal(4)

        val allAccounts = withSQL { select.from(Account as a).orderBy(a.id) }
          .map(Account(a))
          .list
          .apply()
        allAccounts.size should equal(4)

        // join query
        val cookieOrders = withSQL {
          select
            .from(Order as o)
            .innerJoin(Product as p)
            .on(o.productId, p.id)
            .leftJoin(Account as a)
            .on(o.accountId, a.id)
            .where
            .eq(o.productId, 2)
            .orderBy(o.id)
            .desc
            .limit(4)
            .offset(0)
        }.map(Order(o, p, a)).list.apply()

        cookieOrders.size should equal(4)
        cookieOrders(0).product.isEmpty should be(false)
        cookieOrders(0).account.isEmpty should be(true)
        cookieOrders(1).product.isEmpty should be(false)
        cookieOrders(1).account.isEmpty should be(false)

        // cross join query
        val ordersAndProducts = withSQL {
          select
            .from(Order as o)
            .crossJoin(Product as p)
        }.map(Order(o, p)).list.apply()

        val productNum =
          withSQL(select.from(Product as p)).map(Product(p)).list.apply().size
        val orderNum =
          withSQL(select.from(Order as o)).map(Order(o)).list.apply().size
        ordersAndProducts.size should equal(productNum * orderNum)
        ordersAndProducts.head.id should equal(11)
        ordersAndProducts.head.productId should equal(1)

        // dynamic query

        def findCookieOrder(accountRequired: Boolean) = withSQL {
          select
            .from[Order](Order as o)
            .innerJoin(Product as p)
            .on(o.productId, p.id)
            .map { sql =>
              if (accountRequired)
                sql.leftJoin(Account as a).on(o.accountId, a.id)
              else sql
            }
            .where
            .eq(o.id, 13)
        }.map { rs =>
          if (accountRequired) Order(o, p, a)(rs) else Order(o, p)(rs)
        }.single
          .apply()

        findCookieOrder(true).get.account.isEmpty should be(false)
        findCookieOrder(false).get.account.isEmpty should be(true)

        def findByOptionalAccountName(accountName: Option[String]) = withSQL {
          select
            .from[Order](Order as o)
            .innerJoin(Product as p)
            .on(o.productId, p.id)
            .innerJoin(accountName.map(_ => Account as a))
            .on(o.accountId, a.id)
            .where(sqls.toAndConditionOpt(accountName.map(sqls.eq(a.name, _))))
        }.map { rs => Order(o, p)(rs) }.list.apply()

        findByOptionalAccountName(Some("Alice")).size should be(4)
        findByOptionalAccountName(Option.empty).size should be(11)

        {
          val (productId, accountId): (Option[Int], Option[Int]) =
            (Some(1), None)
          val ids = withSQL {
            select(o.result.id)
              .from(Order as o)
              .where(
                sqls.toAndConditionOpt(
                  productId.map(id => sqls.eq(o.productId, id)),
                  accountId.map(id => sqls.eq(o.accountId, id))
                )
              )
              .orderBy(o.id)
          }.map(_.int(1)).list.apply()
          ids should equal(Seq(11, 12, 13, 14, 15))
        }
        {
          val (productId, accountId) = (Some(1), Some(2))
          val ids = withSQL {
            select(o.result.id)
              .from(Order as o)
              .where(
                sqls.toAndConditionOpt(
                  productId.map(id => sqls.eq(o.productId, id)),
                  accountId.map(id => sqls.eq(o.accountId, id))
                )
              )
              .orderBy(o.id)
          }.map(_.int(1)).list.apply()
          ids should equal(Seq(12))
        }

        {
          val (id1, id2): (Option[Int], Option[Int]) = (Some(1), None)
          val ids = withSQL {
            select(o.result.id)
              .from(Order as o)
              .where
              .isNotNull(o.accountId)
              .and(
                sqls.toOrConditionOpt(
                  id1.map(id => sqls.eq(o.productId, id)),
                  id2.map(id => sqls.eq(o.productId, id))
                )
              )
              .orderBy(o.id)
          }.map(_.int(1)).list.apply()
          ids should equal(Seq(11, 12, 13, 14, 15))
        }
        {
          val (id1, id2) = (Some(1), Some(2))
          val ids = withSQL {
            select(o.result.id)
              .from(Order as o)
              .where
              .isNotNull(o.accountId)
              .and(
                sqls.toOrConditionOpt(
                  id1.map(id => sqls.eq(o.productId, id)),
                  id2.map(id => sqls.eq(o.productId, id))
                )
              )
              .orderBy(o.id)
          }.map(_.int(1)).list.apply()
          ids should equal(Seq(11, 12, 13, 14, 15, 21, 22, 23, 24, 25))
        }

        // sub-query
        val sp = SubQuery.syntax("sp").include(p)
        val productId: Option[Int] = withSQL {
          select(sqls"${sp(p).id} id")
            .from(select.from(Product as p).where.eq(p.price, Price(80)).as(sp))
        }.map(_.int("id")).single.apply()

        productId should equal(Option(2))

        // sub-query, group by, having
        import sqls.{ sum, gt }
        val x = SubQuery.syntax("x").include(o, p)
        val preferredClients: List[(Int, Int)] = withSQL {
          select(sqls"${x(o).accountId} id", sqls"${sum(x(p).price)} amount")
            .from(
              select
                .from(Order as o)
                .innerJoin(Product as p)
                .on(o.productId, p.id)
                .as(x)
            )
            .groupBy(x(o).accountId)
            .having(gt(sum(x(p).price), 300))
            .orderBy(sqls"amount")
        }.map(rs => (rs.int("id"), rs.int("amount"))).list.apply()

        preferredClients.size should equal(2)
        preferredClients should equal(List((2, 360), (1, 440)))

        // test withRoundBracket(ConditionSQLBuilder => ConditionSQLBuilder)
        val bracketTestResults = withSQL {
          select(o.result.id)
            .from(Order as o)
            .where
            .withRoundBracket {
              _.eq(o.productId, 1).and.isNotNull(o.accountId)
            }
            .or
            .isNull(o.accountId)
            .orderBy(o.id)
        }.map(_.int(o.resultName.id)).list.apply()

        bracketTestResults should equal(List(11, 12, 13, 14, 15, 26))

        // test roundBracket(SQLSyntax)
        val bracketTestResults2 = withSQL {
          select(o.result.id)
            .from(Order as o)
            .where
            .roundBracket(sqls.eq(o.productId, 1).and.isNotNull(o.accountId))
            .or
            .isNull(o.accountId)
            .orderBy(o.id)
        }.map(_.int(o.resultName.id)).list.apply()

        bracketTestResults2 should equal(List(11, 12, 13, 14, 15, 26))

        {
          val productId = Some(1)
          val withConditionsTestResults = withSQL {
            select(o.result.id)
              .from(Order as o)
              .where
              .withRoundBracket(sql =>
                sqls
                  .toAndConditionOpt(
                    productId.map(i => sqls.eq(o.productId, i)),
                    Some(sqls.isNotNull(o.accountId))
                  )
                  .map(s => sql.append(s))
                  .getOrElse(sql)
              )
              .or
              .isNull(o.accountId)
              .orderBy(o.id)
              .append(sqls"desc")
          }.map(_.int(o.resultName.id)).list.apply()

          withConditionsTestResults should equal(List(26, 15, 14, 13, 12, 11))
        }

        {
          val productId = Some(1)
          val withConditionsTestResults = withSQL {
            select(o.result.id)
              .from(Order as o)
              .where(
                sqls.toOrConditionOpt(
                  productId.map(i => sqls.eq(o.productId, i)),
                  Some(sqls.isNull(o.accountId))
                )
              )
              .orderBy(o.id)
          }.map(_.int(o.resultName.id)).list.apply()

          withConditionsTestResults should equal(List(11, 12, 13, 14, 15, 26))
        }

        // in clause
        {
          val inClauseResults = withSQL {
            select
              .from(Order as o)
              .where
              .in(o.id, Seq(1, 2, 14, 15, 16, 20, 21, 22))
              .orderBy(o.id)
          }.map(Order(o)).list.apply()
          inClauseResults.map(_.id) should equal(List(14, 15, 21, 22))
        }
        {
          val inClauseResults = withSQL {
            select
              .from(Order as o)
              .where
              .in(o.id, Seq[Int]())
              .orderBy(o.id)
          }.map(Order(o)).list.apply()
          inClauseResults.map(_.id) should equal(Nil)
        }
        {
          val notInClauseResults = withSQL {
            select
              .from(Order as o)
              .where
              .notIn(o.id, Seq(14, 15, 22, 23, 24, 25, 26))
              .orderBy(o.id)
          }.map(Order(o)).list.apply()
          notInClauseResults.map(_.id) should equal(List(11, 12, 13, 21))
        }
        {
          val notInClauseResults = withSQL {
            select
              .from(Order as o)
              .where
              .notIn(o.id, Seq[Int]())
              .orderBy(o.id)
          }.map(Order(o)).list.apply()
          notInClauseResults.map(_.id) should equal(
            List(11, 12, 13, 14, 15, 21, 22, 23, 24, 25, 26)
          )
        }
        {
          val notInClauseResults = withSQL {
            select
              .from(Order as o)
              .where
              .not
              .in(o.id, Seq[Int]())
              .orderBy(o.id)
          }.map(Order(o)).list.apply()
          notInClauseResults.map(_.id) should equal(
            List(11, 12, 13, 14, 15, 21, 22, 23, 24, 25, 26)
          )
        }
        {
          val inClauseResults = withSQL {
            select
              .from(Order as o)
              .where
              .in(
                o.id,
                select(o.id).from(Order as o).where.between(o.id, 14, 16)
              )
              .orderBy(o.id)
          }.map(Order(o)).list.apply()
          inClauseResults.map(_.id) should equal(List(14, 15))
        }
        {
          val inClauseResults = withSQL {
            select
              .from(Order as o)
              .where
              .notIn(
                o.id,
                select(o.id).from(Order as o).where.between(o.id, 13, 30)
              )
              .orderBy(o.id)
          }.map(Order(o)).list.apply()
          inClauseResults.map(_.id) should equal(List(11, 12))
        }
        {
          val inClauseResults = withSQL {
            select
              .from(Order as o)
              .where
              .in(
                o.id,
                select(o.id).from(Order as o).where.notBetween(o.id, 14, 16)
              )
              .orderBy(o.id)
          }.map(Order(o)).list.apply()
          inClauseResults.map(_.id) should equal(
            List(11, 12, 13, 21, 22, 23, 24, 25, 26)
          )
        }
        {
          val inClauseResults = withSQL {
            select
              .from(Order as o)
              .where
              .notIn(
                o.id,
                select(o.id).from(Order as o).where.notBetween(o.id, 13, 30)
              )
              .orderBy(o.id)
          }.map(Order(o)).list.apply()
          inClauseResults.map(_.id) should equal(
            List(13, 14, 15, 21, 22, 23, 24, 25, 26)
          )
        }

        {
          val inClauseResults = withSQL {
            select
              .from(Order as o)
              .where
              .in((o.id, o.productId), Seq((11, 1), (12, 2), (21, 2)))
              .orderBy(o.id)
          }.map(Order(o)).list.apply()
          inClauseResults.map(_.id) should equal(List(11, 21))
        }
        {
          val inClauseResults = withSQL {
            select
              .from(Order as o)
              .where
              .in((o.id, o.productId), Seq[(Int, Int)]())
              .orderBy(o.id)
          }.map(Order(o)).list.apply()
          inClauseResults.map(_.id) should equal(Nil)
        }
        {
          val notInClauseResults = withSQL {
            select
              .from(Order as o)
              .where
              .notIn(
                (o.id, o.productId),
                Seq((11, 1), (12, 2), (13, 1), (14, 1), (15, 1), (21, 2))
              )
              .orderBy(o.id)
          }.map(Order(o)).list.apply()
          notInClauseResults.map(_.id) should equal(
            List(12, 22, 23, 24, 25, 26)
          )
        }
        {
          val notInClauseResults = withSQL {
            select
              .from(Order as o)
              .where
              .notIn((o.id, o.productId), Seq[(Int, Int)]())
              .orderBy(o.id)
          }.map(Order(o)).list.apply()
          notInClauseResults.map(_.id) should equal(
            List(11, 12, 13, 14, 15, 21, 22, 23, 24, 25, 26)
          )
        }
        {
          val notInClauseResults = withSQL {
            select
              .from(Order as o)
              .where
              .not
              .in((o.id, o.productId), Seq[(Int, Int)]())
              .orderBy(o.id)
          }.map(Order(o)).list.apply()
          notInClauseResults.map(_.id) should equal(
            List(11, 12, 13, 14, 15, 21, 22, 23, 24, 25, 26)
          )
        }

        // like search
        {
          val results = withSQL {
            select
              .from(Account as a)
              .where
              .like(a.name, "%e%")
              .orderBy(a.id)
          }.map(Account(a)).list.apply()
          results.map(_.id) should equal(List(1, 4))
        }
        {
          val results = withSQL {
            select
              .from(Account as a)
              .where
              .notLike(a.name, "%e%")
              .orderBy(a.id)
          }.map(Account(a)).list.apply()
          results.map(_.id) should equal(List(2, 3))
        }

        // exists clause
        val existsClauseResults = withSQL {
          select(a.id)
            .from(Account as a)
            .where
            .exists(select.from(Order as o).where.eq(o.accountId, a.id))
            .orderBy(a.id)
        }.map(_.int(1)).list.apply()
        existsClauseResults should equal(List(1, 2, 3))

        // not exists clause
        {
          val notExistsClauseResults = withSQL {
            select(a.id)
              .from(Account as a)
              .where
              .not
              .exists(select.from(Order as o).where.eq(o.accountId, a.id))
              .orderBy(a.id)
          }.map(_.int(1)).list.apply()
          notExistsClauseResults should equal(List(4))
        }
        {
          val notExistsClauseResults = withSQL {
            select(a.id)
              .from(Account as a)
              .where
              .notExists(
                sqls"select ${o.id} from ${Order as o} where ${o.accountId} = ${a.id}"
              )
              .orderBy(a.id)
          }.map(_.int(1)).list.apply()
          notExistsClauseResults should equal(List(4))
        }

        // distinct count
        import sqls.{ distinct, count }
        val productCount = withSQL {
          select(count(distinct(o.productId))).from(Order as o)
        }.map(_.int(1)).single.apply().get

        productCount should equal(2)

        // enabled wildcard count but it doesn't work with all the RDBMS
        // HSQLDB: syntax error, H2: always treated as *, MySQL: syntax error
        val wildCardCountSyntax = select(o.productId, count(p), count(a))

        val wildcardCounts = withSQL {
          // select(o.productId, count(p), count(a))
          select(o.productId, count(p.id), count(a.id))
            .from(Order as o)
            .innerJoin(Product as p)
            .on(o.productId, p.id)
            .leftJoin(Account as a)
            .on(o.accountId, a.id)
            .groupBy(o.productId)
        }.map(rs => (rs.int(1), rs.int(2), rs.int(3))).list.apply()

        wildcardCounts.toSet should equal(Set((1, 5, 5), (2, 6, 5)))

        // group by after where clause
        val groupByAfterWhereClauseResults = withSQL {
          select(o.accountId, count)
            .from(Order as o)
            .where
            .isNotNull(o.accountId)
            .groupBy(o.accountId)
            .orderBy(o.accountId)
            .desc
            .limit(2)
        }.map(rs => (rs.int(1), rs.int(2))).list.apply()

        groupByAfterWhereClauseResults should equal(List((3, 2), (2, 4)))

        // union
        val unionResults = withSQL {
          select(sqls"${a.id} as id")
            .from(Account as a)
            .union(select(sqls"${p.id} as id").from(Product as p))
            .orderBy(sqls"id")
            .desc
            .limit(3)
            .offset(0)
        }.map(_.int("id")).list.apply()
        unionResults should equal(List(4, 3, 2))

        // union with limit within a subexpression (fix #1391)
        val unionResultsWithLimit = withSQL {
          select(sqls"${a.id} as id")
            .from(Account as a)
            .union(
              select(sqls"${p.id} as id")
                .from(Product as p)
                .limit(1)
            )
            .orderBy(sqls"id")
            .desc
            .limit(3)
            .offset(0)
        }.map(_.int("id")).list.apply()
        unionResultsWithLimit should equal(List(4, 3, 2))

        // union with order by within a subexpression (fix #1391)
        val unionResultsWithOrderBy = withSQL {
          select(sqls"${a.id} as id")
            .from(Account as a)
            .union(
              select(sqls"${p.id} as id")
                .from(Product as p)
                .orderBy(p.id)
            )
            .orderBy(sqls"id")
            .desc
            .limit(3)
            .offset(0)
        }.map(_.int("id")).list.apply()
        unionResultsWithOrderBy should equal(List(4, 3, 2))

        // union all
        val unionAllResults = withSQL {
          select(a.id)
            .from(Account as a)
            .unionAll(select(p.id).from(Product as p))
            .unionAll(select(p.id).from(Product as p))
        }.map(_.int(1)).list.apply()
        unionAllResults should equal(List(1, 2, 3, 4, 1, 2, 1, 2))

        // union all with limit within a subexpression (fix #1391)
        val unionAllResultsWithLimit = withSQL {
          select(a.id)
            .from(Account as a)
            .unionAll(select(p.id).from(Product as p).limit(1))
            .unionAll(select(p.id).from(Product as p))
        }.map(_.int(1)).list.apply()
        unionAllResultsWithLimit should equal(List(1, 2, 3, 4, 1, 1, 2))

        // union all with limit within a subexpression (fix #1391)
        val unionAllResultsWithOrderBy = withSQL {
          select(a.id)
            .from(Account as a)
            .orderBy(a.id)
            .unionAll(select(p.id).from(Product as p).orderBy(sqls"id"))
            .unionAll(select(p.id).from(Product as p))
        }.map(_.int(1)).list.apply()
        unionAllResultsWithOrderBy should equal(List(1, 2, 3, 4, 1, 2, 1, 2))

        // except
        // MySQL doesn't support except
        if (!isMySQL) {
          val exceptResults = withSQL {
            select(sqls"${a.id} as id")
              .from(Account as a)
              .where
              .in(a.id, Seq(1, 2, 3))
              .unionAll(
                select(sqls"${a.id} as id")
                  .from(Account as a)
                  .where
                  .in(a.id, Seq(1))
              )
              .except(
                select(sqls"${p.id} as id")
                  .from(Product as p)
                  .where
                  .in(p.id, Seq(2))
              )
          }.map(_.int("id")).list.apply()
          exceptResults should equal(List(1, 3))
        }

        // except all
        // H2 Database doesn't support except all
        // MySQL doesn't support except all
        if (!isH2 && !isMySQL) {
          val exceptAllResults = withSQL {
            select(sqls"${a.id} as id")
              .from(Account as a)
              .where
              .in(a.id, Seq(1, 2, 3))
              .unionAll(
                select(sqls"${a.id} as id")
                  .from(Account as a)
                  .where
                  .in(a.id, Seq(1))
              )
              .exceptAll(
                select(sqls"${p.id} as id")
                  .from(Product as p)
                  .where
                  .in(p.id, Seq(2))
              )
          }.map(_.int("id")).list.apply()
          exceptAllResults should equal(List(1, 1, 3))
        }

        // intersect
        // MySQL doesn't support intersect
        if (!isMySQL) {
          val intersectResults = withSQL {
            select(sqls"${a.id} as id")
              .from(Account as a)
              .where
              .in(a.id, Seq(1, 2, 3))
              .intersect(
                select(sqls"${p.id} as id")
                  .from(Product as p)
                  .where
                  .in(p.id, Seq(1, 2))
              )
          }.map(_.int("id")).list.apply()
          intersectResults should equal(List(1, 2))
        }

        // intersect all
        // H2 and MySQL don't support intersect all
        if (!isH2 && !isMySQL) {
          val intersectAllResults = withSQL {
            select(sqls"${p.id} as id")
              .from(Product as p)
              .where
              .in(p.id, Seq(1, 2))
              .intersectAll {
                select(sqls"${a.id} as id")
                  .from(Account as a)
                  .where
                  .in(a.id, Seq(1, 2, 3))
                  .unionAll(
                    select(sqls"${a.id} as id")
                      .from(Account as a)
                      .where
                      .in(a.id, Seq(1))
                  )
              }
              .orderBy(sqls"id")
          }.map(_.int("id")).list.apply()
          intersectAllResults should equal(List(1, 1, 2))
        }

        // between
        val betweenResults = withSQL {
          select(o.result.id).from(Order as o).where.between(o.id, 13, 22)
        }.map(_.int(1)).list.apply()

        betweenResults should equal(List(13, 14, 15, 21, 22))

        val notBetweenResults = withSQL {
          select(o.result.id).from(Order as o).where.notBetween(o.id, 13, 22)
        }.map(_.int(1)).list.apply()

        notBetweenResults should equal(List(11, 12, 23, 24, 25, 26))

        // update,delete
        // applyUpdate = withSQL { ... }.update.apply()

        // TODO the following code becomes compilation error on Scala 2.10.1.
        // applyUpdate(update(Account as a).set(a.name -> "Bob Marley").where.eq(a.id, 2))
        /*
         [error]   scalikejdbc-interpolation/src/test/scala/scalikejdbc/QueryInterfaceSpec.scala:162: erroneous or inaccessible type
         [error]         applyExecute(update(Account as a).set(a.name -> "Bob Marley").where.eq(a.id, 2))
         [error]                                                                                  ^
         */
        // the following code works fine. Don't know why.
        val updateQuery =
          update(Account).set(ac.name -> "Bob Marley").where.eq(ac.id, 2)
        applyUpdate(updateQuery)
        // of course, this code also works fine.
        withSQL(
          update(Account).set(ac.name -> "Bob Marley").where.eq(ac.id, 2)
        ).update.apply()

        val newName = withSQL { select.from(Account as a).where.eq(a.id, 2) }
          .map(Account(a))
          .single
          .apply()
          .get
          .name
        newName should equal(Some("Bob Marley"))

        // TODO compilation error since 2.10.1
        // applyUpdate { delete.from(Order).where.isNull(Order.column.accountId) }
        withSQL {
          delete.from(Order).where.isNull(Order.column.accountId)
        }.update.apply()

        val noAccountIdOrderCount = withSQL {
          select(count).from(Order as o).where.isNull(o.accountId)
        }.map(_.long(1)).single.apply().get
        noAccountIdOrderCount should equal(0)

        val stmt1 = select(count)
          .from(Order as o)
          .where
          .isNull(o.accountId)
          .toSQL
          .statement
        val stmt2 = select(count)
          .from(Order as o)
          .where
          .eq(o.accountId, None)
          .toSQL
          .statement
        val stmt3 = select(count)
          .from(Order as o)
          .where
          .eq(o.accountId, null)
          .toSQL
          .statement
        stmt1 should equal(stmt2)
        stmt2 should equal(stmt3)

        val orders = withSQL {
          QueryDSL.select.from(Order as o).where.isNotNull(o.accountId)
        }.map(Order(o)).list.apply()
        orders.size should be > 0

        QueryDSL.insert.into(Account).columns(ac.id, ac.name).values(1, "Alice")

        // TODO compilation error since 2.10.1
        // QueryDSL.update(Account as a).set(a.name -> "Bob Marley").where.eq(a.id, 2)
        // QueryDSL.delete.from(Order).where.isNull(Order.column.accountId)
        QueryDSL
          .update(Account)
          .set(ac.name -> "Bob Marley")
          .where
          .eq(ac.c("id"), 2)
        QueryDSL.delete
          .from(Order)
          .where
          .isNull(Order.column.field("accountId"))

        // insert returning id for PostgreSQL
        val returningIdQuery =
          insert.into(Account).namedValues(ac.name -> "Alice").returningId
        returningIdQuery.toSQL.statement should equal(
          "insert into qi_accounts (name) values (?) returning id"
        )

        // insert returning columns* for PostgreSQL
        val returningColumnsQuery = insert
          .into(Account)
          .namedValues(ac.name -> "Alice")
          .returning(ac.name, ac.id)
        returningColumnsQuery.toSQL.statement should equal(
          "insert into qi_accounts (name) values (?) returning name, id"
        )

        // update returning columns* for PostgreSQL
        val updateReturningColumnsQuery = QueryDSL
          .update(Account)
          .set(ac.name -> "Alice")
          .returning(ac.name, ac.id, ac.name)
        updateReturningColumnsQuery.toSQL.statement should equal(
          "update qi_accounts set name = ? returning name, id, name"
        )
      }

      // for update query
      val o = Order.syntax("o")
      DB localTx { implicit s =>
        withSQL { select.from(Order as o).where.eq(o.id, 1).forUpdate }
          .map(Order(o))
          .single
          .apply()
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

  "insert.namedValues" should "accepts not only var args" in {
    val ac = Account.column
    val s = insert.into(Account).namedValues(Map(ac.name -> "Bob Marley")).toSQL
    s.statement should equal("insert into qi_accounts (name) values (?)")
    s.parameters should equal(Seq("Bob Marley"))
  }

  "update.set" should "accepts not only var args" in {
    val ac = Account.column
    val s = update(Account).set(Map(ac.name -> "Bob Marley")).toSQL
    s.statement should equal("update qi_accounts set name = ?")
    s.parameters should equal(Seq("Bob Marley"))
  }

  "insert.namedValues" should "accept None under nested AsIsParameterBinder" in {
    DB autoCommit { implicit s =>
      try sql"drop table ${Account.table}".execute.apply()
      catch { case e: Exception => }
      sql"create table ${Account.table} (id int not null, name varchar(256))".execute
        .apply()
    }

    try {
      val ac = Account.column
      val params: Seq[(SQLSyntax, Any)] =
        Seq(ac.id -> 123, ac.name -> AsIsParameterBinder(None))
      val query = insert
        .into(Account)
        .namedValues(params.map { case (k, v) =>
          k -> AsIsParameterBinder(v)
        }*)
        .toSQL
      query.statement should equal(
        "insert into qi_accounts (id, name) values (?, ?)"
      )
      query.parameters should equal(Seq(123, None))

      DB autoCommit { implicit s => query.update.apply() }
    } finally {
      DB autoCommit { implicit s =>
        try sql"drop table ${Account.table}".execute.apply()
        catch { case e: Exception => }
      }
    }
  }

  it should "convert timeZone" in {
    import java.util.TimeZone

    val t = TimeHolder.syntax("t")

    val time =
      ZonedDateTime.of(2016, 1, 9, 2, 43, 42, 0, ZoneId.of("Asia/Tokyo"))

    val castToString: String = if (isMySQL) {
      "date_format(time, '%Y-%m-%d %H:%i:%S')"
    } else {
      "to_char(time, 'YYYY-MM-DD HH24:MI:SS')"
    }

    DB autoCommit { implicit session =>
      try sql"drop table ${TimeHolder.table}".execute.apply()
      catch { case e: Exception => }
      sql"create table ${TimeHolder.table} (id int, time timestamp)".execute
        .apply()
    }

    try {
      // execute with Asia/Tokyo timezone
      DB autoCommit { session =>
        implicit val jstSession = DBSession(
          conn = session.conn,
          connectionAttributes =
            session.connectionAttributes.copy(timeZoneSettings =
              TimeZoneSettings(true, TimeZone.getTimeZone("Asia/Tokyo"))
            )
        )

        applyUpdate(
          insertInto(TimeHolder).namedValues(
            TimeHolder.column.id -> 1,
            TimeHolder.column.time -> time
          )
        )
        val jstString = SQL(
          s"select $castToString as s from ${TimeHolder.tableName} where id = 1"
        ).map(_.string("s")).single.apply().get
        val expected =
          DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(time)
        jstString should equal(expected)

        val expectedTime1 = withSQL(
          selectFrom(TimeHolder as t).where.eq(t.id, 1)
        ).map(TimeHolder(t)(_)).single.apply().get.time
        expectedTime1.isEqual(time) should equal(true)
      }

      // execute with UTC timezone
      DB autoCommit { session =>
        implicit val utcSession = DBSession(
          conn = session.conn,
          connectionAttributes =
            session.connectionAttributes.copy(timeZoneSettings =
              TimeZoneSettings(true, TimeZone.getTimeZone("UTC"))
            )
        )

        applyUpdate(
          insertInto(TimeHolder).namedValues(
            TimeHolder.column.id -> 2,
            TimeHolder.column.time -> time
          )
        )
        val utcString = SQL(
          s"select $castToString as s from ${TimeHolder.tableName} where id = 2"
        ).map(_.string("s")).single.apply().get
        val expected = DateTimeFormatter
          .ofPattern("yyyy-MM-dd HH:mm:ss")
          .format(time.withZoneSameInstant(ZoneId.of("UTC")))
        utcString should equal(expected)

        val expectedTime2 = withSQL(
          selectFrom(TimeHolder as t).where.eq(t.id, 2)
        ).map(TimeHolder(t)(_)).single.apply().get.time
        expectedTime2.isEqual(time) should equal(true)

        val map = withSQL(
          select(sqls"time").from(TimeHolder as t).where.eq(t.id, 2)
        ).map(_.toMap()).single.apply().get
        if (map.get("time").isDefined) {
          map.get("time") should equal(
            Some(java.sql.Timestamp.from(time.toInstant))
          )
        } else {
          map.get("TIME") should equal(
            Some(java.sql.Timestamp.from(time.toInstant))
          )
        }
      }
    } finally {
      DB autoCommit { implicit session =>
        try sql"drop table ${TimeHolder.table}".execute.apply()
        catch { case e: Exception => }
      }
    }
  }

  it should "have result.name" in {
    select(Product.syntax("p").result.name)
    select(Product.column.name)
  }
}
