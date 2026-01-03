package scalikejdbc

import util.control.Exception._
import org.scalatest._
import org.scalatest.BeforeAndAfter
import java.sql.{ SQLException, PreparedStatement }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SQLSpec
  extends AnyFlatSpec
  with Matchers
  with BeforeAndAfter
  with Settings
  with LoanPattern {

  val tableNamePrefix = "emp_SQLSpec" + System.currentTimeMillis()

  behavior of "SQL"

  it should "execute insert with nullable values" in {
    val tableName = tableNamePrefix + "_insertWithNullableValues"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(new DB(ConnectionPool.borrow())) { db =>
        implicit val session = db.autoCommitSession()

        SQL("insert into " + tableName + " values (?, ?)")
          .bind(3, Option("Ben"))
          .execute
          .apply()

        val benOpt = SQL("select id,name from " + tableName + " where id = ?")
          .bind(3)
          .map(rs => (rs.int("id"), rs.string("name")))
          .toOption
          .apply()

        benOpt.get._1 should equal(3)
        benOpt.get._2 should equal("Ben")

        SQL("insert into " + tableName + " values (?, ?)")
          .bind(4, Option(null))
          .execute
          .apply()

        val noName = SQL("select id,name from " + tableName + " where id = ?")
          .bind(4)
          .map(rs => (rs.int("id"), rs.string("name")))
          .toOption
          .apply()

        noName.get._1 should equal(4)
        noName.get._2 should equal(null)

        val before = (s: PreparedStatement) => println("before")
        val after = (s: PreparedStatement) => println("before")
        SQL("insert into " + tableName + " values (?, ?)")
          .bind(5, Option(null))
          .executeWithFilters(before, after)
          .apply()
      }
    }
  }

  // --------------------
  // auto commit

  it should "execute single in auto commit mode" in {
    val tableName = tableNamePrefix + "_singleInAutoCommit"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(new DB(ConnectionPool.borrow())) { db =>
        implicit val session = db.autoCommitSession()

        val singleResult = SQL("select id from " + tableName + " where id = ?")
          .bind(1)
          .map(_.string("id"))
          .toOption
          .apply()
        singleResult.get should equal("1")

        val firstResult = SQL("select id from " + tableName)
          .map(_.string("id"))
          .headOption
          .apply()
        firstResult.get should equal("1")
      }
    }
  }

  it should "execute list in auto commit mode" in {
    val tableName = tableNamePrefix + "_listInAutoCommit"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(DB(ConnectionPool.borrow())) { db =>
        implicit val session = db.autoCommitSession()
        val result = SQL("select id from " + tableName)
          .map(_.string("id"))
          .toList
          .apply()
        result.size should equal(2)
      }
    }
  }

  it should "execute collection in auto commit mode" in {
    val tableName = tableNamePrefix + "_listInAutoCommit"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(DB(ConnectionPool.borrow())) { db =>
        implicit val session = db.autoCommitSession()
        val result = SQL("select id from " + tableName)
          .map(_.string("id"))
          .toCollection[Vector]()
        result.size should equal(2)
      }
    }
  }

  it should "execute fold in auto commit mode" in {
    val tableName = tableNamePrefix + "_listInAutoCommit"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(DB(ConnectionPool.borrow())) { db =>
        implicit val session = db.autoCommitSession()
        val result =
          SQL("select id from " + tableName + "").foldLeft[List[String]](Nil) {
            case (r, rs) => rs.string("id") :: r
          }
        result.size should equal(2)
      }
    }
  }

  it should "execute executeUpdate in auto commit mode" in {
    val tableName = tableNamePrefix + "_updateInAutoCommit"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(DB(ConnectionPool.borrow())) { db =>
        implicit val session = db.autoCommitSession()

        val count = SQL("update " + tableName + " set name = ? where id = ?")
          .bind("foo", 1)
          .executeUpdate
          .apply()
        count should equal(1)

        val before = (s: PreparedStatement) => println("before")
        val after = (s: PreparedStatement) => println("after")
        SQL("update " + tableName + " set name = ? where id = ?")
          .bind("foo", 1)
          .executeUpdateWithFilters(before, after)
          .apply()

        db.rollbackIfActive()

        // should be updated
        val name = SQL("select name from " + tableName + " where id = ?")
          .bind(1)
          .map(_.string("name"))
          .toOption
          .apply()
          .getOrElse("---")
        name should equal("foo")
      }
    }

  }

  it should "execute update in auto commit mode" in {
    val tableName = tableNamePrefix + "_updateInAutoCommit"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(DB(ConnectionPool.borrow())) { db =>
        implicit val session = db.autoCommitSession()
        val count = SQL("update " + tableName + " set name = ? where id = ?")
          .bind("foo", 1)
          .update
          .apply()
        count should equal(1)

        val before = (s: PreparedStatement) => println("before")
        val after = (s: PreparedStatement) => println("after")
        SQL("update " + tableName + " set name = ? where id = ?")
          .bind("foo", 1)
          .updateWithFilters(before, after)
          .apply()

        db.rollbackIfActive()

        // should be updated
        val name = SQL("select name from " + tableName + " where id = ?")
          .bind(1)
          .map(_.string("name"))
          .toOption
          .apply()
          .getOrElse("---")
        name should equal("foo")
      }
    }

  }

  // --------------------
  // within tx mode

  it should "execute single in within tx mode" in {
    val tableName = tableNamePrefix + "_singleInWithinTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(DB(ConnectionPool.borrow())) { db =>
        db.begin()
        implicit val session = db.withinTxSession()
        TestUtils.initializeEmpRecords(session, tableName)
        val result = SQL("select id from " + tableName + " where id = ?")
          .bind(1)
          .map(_.string("id"))
          .toOption
          .apply()
        result.get should equal("1")
        db.rollbackIfActive()
      }
    }
  }

  it should "execute list in within tx mode" in {
    val tableName = tableNamePrefix + "_listInWithinTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(DB(ConnectionPool.borrow())) { db =>
        db.begin()
        implicit val session = db.withinTxSession()
        TestUtils.initializeEmpRecords(session, tableName)
        val result = SQL("select id from " + tableName + "")
          .map { _.string("id") }
          .toList
          .apply()
        result.size should equal(2)
        db.rollbackIfActive()
      }
    }
  }

  it should "execute collection in within tx mode" in {
    val tableName = tableNamePrefix + "_listInWithinTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(DB(ConnectionPool.borrow())) { db =>
        db.begin()
        implicit val session = db.withinTxSession()
        TestUtils.initializeEmpRecords(session, tableName)
        val result = SQL("select id from " + tableName + "")
          .map { _.string("id") }
          .toCollection[Vector]()
        result.size should equal(2)
        db.rollbackIfActive()
      }
    }
  }

  it should "execute fold in within tx mode" in {
    val tableName = tableNamePrefix + "_listInWithinTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(DB(ConnectionPool.borrow())) { db =>
        db.begin()
        implicit val session = db.withinTxSession()
        TestUtils.initializeEmpRecords(session, tableName)
        val result =
          SQL("select id from " + tableName + "").foldLeft[List[String]](Nil) {
            case (r, rs) => rs.string("id") :: r
          }
        result.size should equal(2)
        db.rollbackIfActive()
      }
    }
  }

  it should "execute update in within tx mode" in {
    val tableName = tableNamePrefix + "_updateInWithinTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(DB(ConnectionPool.borrow())) { db =>
        db.begin()
        implicit val session = db.withinTxSession()
        TestUtils.initializeEmpRecords(session, tableName)
        val nameBefore = SQL("select name from " + tableName + " where id = ?")
          .bind(1)
          .map { _.string("name") }
          .toOption
          .apply()
        nameBefore.get should equal("name1")
        val count = SQL("update " + tableName + " set name = ? where id = ?")
          .bind("foo", 1)
          .executeUpdate
          .apply()
        count should equal(1)
        db.rollback()
      }
      DB readOnly { implicit session =>
        val name = SQL("select name from " + tableName + " where id = ?")
          .bind(1)
          .map { _.string("name") }
          .toOption
          .apply()
        name.get should equal("name1")
      }
    }
  }

  it should "use GlobalSettings.nameBindingSQLValidator" in {
    val tableName = tableNamePrefix + "_nameBindingSQLValidator"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      GlobalSettings.nameBindingSQLValidator =
        NameBindingSQLValidatorSettings(globalsettings.NoCheckForIgnoredParams)
      DB readOnly { implicit s =>
        SQL("select 1 from " + tableName)
          .bindByName("foo" -> "bar")
          .map(_.toMap())
          .list
          .apply()
      }
      GlobalSettings.nameBindingSQLValidator = NameBindingSQLValidatorSettings(
        globalsettings.InfoLoggingForIgnoredParams
      )
      DB readOnly { implicit s =>
        SQL("select 1 from " + tableName)
          .bindByName("foo" -> "bar")
          .map(_.toMap())
          .list
          .apply()
      }
      GlobalSettings.nameBindingSQLValidator = NameBindingSQLValidatorSettings(
        globalsettings.WarnLoggingForIgnoredParams
      )
      DB readOnly { implicit s =>
        SQL("select 1 from " + tableName)
          .bindByName("foo" -> "bar")
          .map(_.toMap())
          .list
          .apply()
      }
      GlobalSettings.nameBindingSQLValidator = NameBindingSQLValidatorSettings(
        globalsettings.ExceptionForIgnoredParams
      )
      intercept[IllegalStateException] {
        DB readOnly { implicit s =>
          SQL("select 1 from " + tableName)
            .bindByName("foo" -> "bar")
            .map(_.toMap())
            .list
            .apply()
        }
      }
    }
    GlobalSettings.nameBindingSQLValidator = NameBindingSQLValidatorSettings()
  }

  it should "has #toMap" in {
    val tableName = tableNamePrefix + "_nameBindingSQLValidator"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      val results: List[Map[String, Any]] = DB readOnly { implicit s =>
        SQL("select 1 from " + tableName).toMap.list.apply()
      }
      results.size should be > 0
    }
  }

  it should "return statement and parameters" in {
    val sql = SQL("select * from company where id = ?").bind(123)
    sql.statement should equal("select * from company where id = ?")
    sql.parameters should equal(Seq(123))
  }

  it should "return extractor" in {
    val expected = (rs: WrappedResultSet) => rs.long(1)
    val sql = SQL("select * from company where id = ?").bind(123).map(expected)
    sql.extractor should equal(expected)
  }

  it should "work with ReadOnlyAutoSession #189" in {
    val tableName = tableNamePrefix + "_readOnlyAutoSession"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      implicit val session = ReadOnlyAutoSession

      SQL("select id from " + tableName + "").map(_.long(1)).list.apply()

      intercept[SQLException] {
        SQL("delete from " + tableName + "").update.apply()
      }
      intercept[SQLException] {
        SQL("update " + tableName + " set name = 'Anonymous'").update.apply()
      }
      intercept[SQLException] {
        SQL("update " + tableName + " set name = ?")
          .batch(Seq("Anonymous"))
          .apply()
      }
    }
  }

  it should "work with ReadOnlyNamedAutoSession #189" in {
    val tableName = tableNamePrefix + "_readOnlyNamedAutoSession"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      implicit val session = ReadOnlyNamedAutoSession("named")

      SQL("select id from " + tableName + "").map(_.long(1)).list.apply()

      intercept[SQLException] {
        SQL("delete from " + tableName + "").update.apply()
      }
      intercept[SQLException] {
        SQL("update " + tableName + " set name = 'Anonymous'").update.apply()
      }
      intercept[SQLException] {
        SQL("update " + tableName + " set name = ?")
          .batch(Seq("Anonymous"))
          .apply()
      }
    }
  }

  // --------------------
  // Batch

  it should "be able to return generated keys when running batch API" in {
    DB autoCommit { implicit session =>
      ultimately(TestUtils.deleteTable("sqlspec_genkey")) {
        try {
          try {
            SQL(
              "create table sqlspec_genkey (id integer generated always as identity(start with 0), name varchar(10))"
            ).execute.apply()
          } catch {
            case e: Exception =>
              try {
                SQL(
                  "create table sqlspec_genkey (id integer auto_increment, name varchar(10), primary key(id))"
                ).execute.apply()
              } catch {
                case e: Exception =>
                  SQL(
                    "create table sqlspec_genkey (id serial not null, name varchar(10), primary key(id))"
                  ).execute.apply()
              }
          }

          val paramss = Seq(Seq("xxx"), Seq("yyy"), Seq("zzz"))

          val ids1: collection.Seq[Long] = SQL(
            "insert into sqlspec_genkey (name) values (?)"
          ).batchAndReturnGeneratedKey(paramss*).apply[collection.Seq]()
          ids1.size should equal(3)
          ids1.last should be <= 3L

          val ids2: collection.Seq[Long] = SQL(
            "insert into sqlspec_genkey (name) values (?)"
          ).batchAndReturnGeneratedKey(paramss*).apply[collection.Seq]()
          ids2.size should equal(3)
          ids2.last should be <= 6L

          // for Oracle DB
          // just check compilation
          SQL("insert into sqlspec_genkey (name) values (?)")
            .batchAndReturnGeneratedKey("id", paramss*)
          //  val ids3: collection.Seq[Long] = SQL("insert into sqlspec_genkey (name) values (?)").batchAndReturnGeneratedKey("id", paramss: _*).apply()
          //  ids3.size should equal(3)
          //  ids3.last should be <= 9L
        }
      }
    }
  }

}
