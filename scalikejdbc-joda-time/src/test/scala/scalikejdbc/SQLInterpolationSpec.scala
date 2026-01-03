package scalikejdbc

import org.joda.time._
import scalikejdbc.jodatime.JodaUnixTimeInMillisConverterImplicits._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SQLInterpolationSpec
  extends AnyFlatSpec
  with Matchers
  with LogSupport
  with LoanPattern
  with JavaUtilDateConverterImplicits {

  behavior of "SQLInterpolation"

  val props = new java.util.Properties
  using(
    new java.io.FileInputStream(
      "scalikejdbc-core/src/test/resources/jdbc.properties"
    )
  ) { in => props.load(in) }
  val driverClassName = props.getProperty("driverClassName")
  val url = props.getProperty("url")
  val user = props.getProperty("user")
  val password = props.getProperty("password")

  Class.forName(driverClassName)
  val poolSettings = new ConnectionPoolSettings(initialSize = 50, maxSize = 50)
  ConnectionPool.singleton(url, user, password, poolSettings)

  case class Group(
    id: Int,
    websiteUrl: Option[String],
    members: collection.Seq[User] = Nil
  )
  case class User(
    id: Int,
    name: Option[String],
    groupId: Option[Int] = None,
    group: Option[Group] = None
  )

  it should "be available with here document values" in {
    DB localTx { implicit s =>
      try {
        sql"""create table interpolation_users (id int, name varchar(256))""".execute
          .apply()

        Seq((1, "foo"), (2, "bar"), (3, "baz")) foreach { case (id, name) =>
          sql"""insert into interpolation_users values (${id}, ${name})""".update
            .apply()
        }

        val id = 3
        val user = sql"""select * from interpolation_users where id = ${id}"""
          .map { rs =>
            User(id = rs.int("id"), name = rs.stringOpt("name"))
          }
          .single
          .apply()
        user.isDefined should equal(true)
      } finally {
        sql"""drop table interpolation_users""".execute.apply()
      }
    }
  }

  it should "be available with option values" in {
    DB localTx { implicit s =>
      try {
        sql"create table interpolation_users (id int not null, name varchar(256))".execute
          .apply()

        Seq((1, Some("foo")), (2, None)) foreach { case (id, name) =>
          sql"insert into interpolation_users values (${id}, ${name})".update
            .apply()
        }

        val id = 2
        val user: User = sql"select * from interpolation_users where id = ${id}"
          .map { rs =>
            User(id = rs.int("id"), name = rs.stringOpt("name"))
          }
          .single
          .apply()
          .get
        user.id should equal(2)
        user.name should be(None)
      } finally {
        sql"drop table interpolation_users".execute.apply()
      }
    }
  }

  it should "be available with the IN statement" in {
    DB localTx { implicit s =>
      try {
        sql"create table interpolation_users (id int not null, name varchar(256))".execute
          .apply()
        Seq((1, "foo"), (2, "bar"), (3, "baz")) foreach { case (id, name) =>
          sql"insert into interpolation_users values (${id}, ${name})".update
            .apply()
        }

        val ids = List(1, 2, 4) ::: (100 until 200).toList
        val interpolation_users =
          sql"select * from interpolation_users where id in (${ids})"
            .map { rs =>
              User(id = rs.int("id"), name = rs.stringOpt("name"))
            }
            .list
            .apply()
        interpolation_users.size should equal(2)
        interpolation_users.map(_.name) should equal(
          Seq(Some("foo"), Some("bar"))
        )
      } finally {
        sql"drop table interpolation_users".execute.apply()
      }
    }
  }

  it should "be available with sql syntax" in {
    DB localTx { implicit s =>
      try {
        sql"create table interpolation_users (id int not null, name varchar(256))".execute
          .apply()
        Seq((1, "foo"), (2, "bar"), (3, "baz")) foreach { case (id, name) =>
          sql"insert into interpolation_users values (${id}, ${name})".update
            .apply()
        }

        val ids = List(1, 2, 4) ::: (100 until 200).toList
        val sorting = sqls"desc"
        val interpolation_users =
          sql"select * from interpolation_users where id in (${ids}) order by id ${sorting}"
            .map { rs =>
              User(id = rs.int("id"), name = rs.stringOpt("name"))
            }
            .list
            .apply()
        interpolation_users.size should equal(2)
        interpolation_users.map(_.name) should equal(
          Seq(Some("bar"), Some("foo"))
        )
      } finally {
        sql"drop table interpolation_users".execute.apply()
      }
    }
  }

  it should "support some syntax" in {
    import scalikejdbc.interpolation.SQLSyntax._
    try {
      DB autoCommit { implicit s =>
        sql"create table sqlsyntax_spec (id int not null, name varchar(256))".execute
          .apply()
        sql"insert into sqlsyntax_spec values (1, ${"Alice"})".execute.apply()
      }
      DB readOnly { implicit s =>
        // abs
        {
          val v = sqls"${123}"
          val doubleResult = sql"select ${abs(v)} from sqlsyntax_spec limit 1"
            .map(_.double(1))
            .single
            .apply()
            .get
          doubleResult should equal(123.0d)
          val floatResult = sql"select ${abs(v)} from sqlsyntax_spec limit 1"
            .map(_.float(1))
            .single
            .apply()
            .get
          floatResult should equal(123.0f)
          val intResult = sql"select ${abs(v)} from sqlsyntax_spec limit 1"
            .map(_.int(1))
            .single
            .apply()
            .get
          intResult should equal(123)
          val longResult = sql"select ${abs(v)} from sqlsyntax_spec limit 1"
            .map(_.long(1))
            .single
            .apply()
            .get
          longResult should equal(123L)
        }
        // floor
        {
          val v = sqls"${123.4}"
          val doubleResult = sql"select ${floor(v)} from sqlsyntax_spec limit 1"
            .map(_.double(1))
            .single
            .apply()
            .get
          doubleResult should equal(123.0d)
          val floatResult = sql"select ${floor(v)} from sqlsyntax_spec limit 1"
            .map(_.float(1))
            .single
            .apply()
            .get
          floatResult should equal(123.0d)
          val intResult = sql"select ${floor(v)} from sqlsyntax_spec limit 1"
            .map(_.int(1))
            .single
            .apply()
            .get
          intResult should equal(123)
          val longResult = sql"select ${floor(v)} from sqlsyntax_spec limit 1"
            .map(_.long(1))
            .single
            .apply()
            .get
          longResult should equal(123L)
        }
        // ceiling
        {
          val v = sqls"${123.4}"
          val doubleResult =
            sql"select ${ceiling(v)} from sqlsyntax_spec limit 1"
              .map(_.double(1))
              .single
              .apply()
              .get
          doubleResult should equal(124.0d)
          val floatResult =
            sql"select ${ceiling(v)} from sqlsyntax_spec limit 1"
              .map(_.float(1))
              .single
              .apply()
              .get
          floatResult should equal(124.0d)
          val intResult = sql"select ${ceiling(v)} from sqlsyntax_spec limit 1"
            .map(_.int(1))
            .single
            .apply()
            .get
          intResult should equal(124)
          val longResult = sql"select ${ceiling(v)} from sqlsyntax_spec limit 1"
            .map(_.long(1))
            .single
            .apply()
            .get
          longResult should equal(124L)
        }
        // current_date
        {
          val t = sql"select ${currentDate} from sqlsyntax_spec limit 1"
            .map(_.date(1))
            .single
            .apply()
            .get
          log.warn("current_date: " + t + "," + t.getTime)
          // Timezone issue
          // t.toLocalDate should equal(LocalDate.now)
        }
        // current_timestamp
        {
          val t = sql"select ${currentTimestamp} from sqlsyntax_spec limit 1"
            .map(_.timestamp(1))
            .single
            .apply()
            .get
          log.warn("current_timestamp: " + t + "," + t.getTime)
          t.toJodaDateTime.getMillis should be < (DateTime.now
            .plusDays(1)
            .getMillis)
        }
      }
    } finally {
      DB autoCommit { implicit s =>
        sql"drop table sqlsyntax_spec".execute.apply()
      }
    }
  }

  // issue #215 https://github.com/scalikejdbc/scalikejdbc/issues/215
  it should "work with toSeq (#215)" in {
    DB localTx { implicit s =>
      try {
        sql"""create table interpolation_users (id int, name varchar(256))""".execute
          .apply()

        Seq((1, "foo"), (2, "bar"), (3, "baz")) foreach { case (id, name) =>
          sql"""insert into interpolation_users values (${id}, ${name})""".update
            .apply()
        }

        val names = """.*?""".r.findAllIn("""a&""").toSeq
        val users =
          sql"""select * from interpolation_users where name in (${names})"""
            .map { rs =>
              User(id = rs.int("id"), name = rs.stringOpt("name"))
            }
            .list
            .apply()
        users should have size 0
      } finally {
        sql"""drop table interpolation_users""".execute.apply()
      }
    }
  }

  it should "work with toList (#215)" in {
    DB localTx { implicit s =>
      try {
        sql"""create table interpolation_users (id int, name varchar(256))""".execute
          .apply()

        Seq((1, "foo"), (2, "bar"), (3, "baz")) foreach { case (id, name) =>
          sql"""insert into interpolation_users values (${id}, ${name})""".update
            .apply()
        }

        // val names = """.*?""".r.findAllIn("""a&""").toSeq
        val names = """.*?""".r.findAllIn("""a&""").toList
        val users =
          sql"""select * from interpolation_users where name in (${names})"""
            .map { rs =>
              User(id = rs.int("id"), name = rs.stringOpt("name"))
            }
            .list
            .apply()
        users should have size 0
      } finally {
        sql"""drop table interpolation_users""".execute.apply()
      }
    }
  }

  it should "accept Traversable[SQLSyntax] (#216)" in {
    DB localTx { implicit s =>
      try {
        sql"""create table interpolation_users_216 (id int, name varchar(256))""".execute
          .apply()
        Seq((1, "foo"), (2, "bar"), (3, "baz")) foreach { case (id, name) =>
          sql"""insert into interpolation_users_216 values (${id}, ${name})""".update
            .apply()
        }
        val columns: collection.Seq[SQLSyntax] =
          Seq("id", "name").map(SQLSyntax.createUnsafely(_, Nil))
        val values: collection.Seq[SQLSyntax] =
          Seq(Seq(1, "foo"), Seq(2, "bar"), Seq(3, "bazzzz")).map { xs =>
            sqls"($xs)"
          }
        val sql =
          sql"select count(1) from interpolation_users_216 where ${columns} in (${values})"

        sql.statement should equal(
          "select count(1) from interpolation_users_216 where id, name in ((?, ?), (?, ?), (?, ?))"
        )
        sql.parameters should equal(Seq(1, "foo", 2, "bar", 3, "bazzzz"))
        // fails with h2/hsqldb
        // sql.map(_.long(1)).single.apply() should equal(Some(2))

      } finally {
        sql"""drop table interpolation_users_216""".execute.apply()
      }
    }
  }

  it should "interpolate a Set using the correct number of placeholders" in {
    DB localTx { implicit s =>
      try {
        sql"""create table interpolation_users_set (id int, name varchar(256))""".execute
          .apply()
        Seq((1, "foo"), (2, "bar"), (3, "baz")) foreach { case (id, name) =>
          sql"""insert into interpolation_users_set values (${id}, ${name})""".update
            .apply()
        }
        val sql =
          sql"select count(1) from interpolation_users_set where id in (${Set(1, 3)})"

        sql.statement should equal(
          "select count(1) from interpolation_users_set where id in (?, ?)"
        )
        sql.parameters should equal(Seq(1, 3))
        sql.map(_.long(1)).single.apply() should equal(Some(2))

      } finally {
        sql"""drop table interpolation_users_set""".execute.apply()
      }
    }
  }

}
