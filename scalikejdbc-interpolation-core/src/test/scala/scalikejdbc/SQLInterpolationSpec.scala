package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._
import org.joda.time._

class SQLInterpolationSpec extends FlatSpec with ShouldMatchers with LogSupport {

  import scalikejdbc.interpolation._
  import scalikejdbc.interpolation.Implicits._

  behavior of "SQLInterpolation"

  val props = new java.util.Properties
  using(new java.io.FileInputStream("scalikejdbc-library/src/test/resources/jdbc.properties")) { in => props.load(in) }
  val driverClassName = props.getProperty("driverClassName")
  val url = props.getProperty("url")
  val user = props.getProperty("user")
  val password = props.getProperty("password")

  Class.forName(driverClassName)
  val poolSettings = new ConnectionPoolSettings(initialSize = 50, maxSize = 50)
  ConnectionPool.singleton(url, user, password, poolSettings)

  case class Group(id: Int, websiteUrl: Option[String], members: Seq[User] = Nil)
  case class User(id: Int, name: Option[String], groupId: Option[Int] = None, group: Option[Group] = None)

  it should "be available with here document values" in {
    DB localTx {
      implicit s =>
        try {
          sql"""create table interpolation_users (id int, name varchar(256))""".execute.apply()

          Seq((1, "foo"), (2, "bar"), (3, "baz")) foreach {
            case (id, name) =>
              sql"""insert into interpolation_users values (${id}, ${name})""".update.apply()
          }

          val id = 3
          val user = sql"""select * from interpolation_users where id = ${id}""".map {
            rs => User(id = rs.int("id"), name = rs.stringOpt("name"))
          }.single.apply()
          user.isDefined should equal(true)
        } finally {
          sql"""drop table interpolation_users""".execute.apply()
        }
    }
  }

  it should "be available with option values" in {
    DB localTx {
      implicit s =>
        try {
          sql"create table interpolation_users (id int not null, name varchar(256))".execute.apply()

          Seq((1, Some("foo")), (2, None)) foreach {
            case (id, name) =>
              sql"insert into interpolation_users values (${id}, ${name})".update.apply()
          }

          val id = 2
          val user: User = sql"select * from interpolation_users where id = ${id}".map {
            rs => User(id = rs.int("id"), name = rs.stringOpt("name"))
          }.single.apply().get
          user.id should equal(2)
          user.name should be(None)
        } finally {
          sql"drop table interpolation_users".execute.apply()
        }
    }
  }

  it should "be available with the IN statement" in {
    DB localTx {
      implicit s =>
        try {
          sql"create table interpolation_users (id int not null, name varchar(256))".execute.apply()
          Seq((1, "foo"), (2, "bar"), (3, "baz")) foreach {
            case (id, name) =>
              sql"insert into interpolation_users values (${id}, ${name})".update.apply()
          }

          val ids = List(1, 2, 4) ::: (100 until 200).toList
          val interpolation_users = sql"select * from interpolation_users where id in (${ids})".map {
            rs => User(id = rs.int("id"), name = rs.stringOpt("name"))
          }.list.apply()
          interpolation_users.size should equal(2)
          interpolation_users.map(_.name) should equal(Seq(Some("foo"), Some("bar")))
        } finally {
          sql"drop table interpolation_users".execute.apply()
        }
    }
  }

  it should "be available with sql syntax" in {
    DB localTx {
      implicit s =>
        try {
          sql"create table interpolation_users (id int not null, name varchar(256))".execute.apply()
          Seq((1, "foo"), (2, "bar"), (3, "baz")) foreach {
            case (id, name) =>
              sql"insert into interpolation_users values (${id}, ${name})".update.apply()
          }

          val ids = List(1, 2, 4) ::: (100 until 200).toList
          val sorting = sqls"desc"
          val interpolation_users = sql"select * from interpolation_users where id in (${ids}) order by id ${sorting}".map {
            rs => User(id = rs.int("id"), name = rs.stringOpt("name"))
          }.list.apply()
          interpolation_users.size should equal(2)
          interpolation_users.map(_.name) should equal(Seq(Some("bar"), Some("foo")))
        } finally {
          sql"drop table interpolation_users".execute.apply()
        }
    }
  }

  it should "support some syntax" in {
    import SQLSyntax._
    try {
      DB autoCommit { implicit s =>
        sql"create table sqlsyntax_spec (id int not null, name varchar(256))".execute.apply()
        sql"insert into sqlsyntax_spec values (1, ${"Alice"})".execute.apply()
      }
      DB readOnly { implicit s =>
        // abs
        {
          val v = sqls"${123}"
          val doubleResult = sql"select ${abs(v)} from sqlsyntax_spec limit 1".map(_.double(1)).single.apply().get
          doubleResult should equal(123.0d)
          val floatResult = sql"select ${abs(v)} from sqlsyntax_spec limit 1".map(_.float(1)).single.apply().get
          floatResult should equal(123.0f)
          val intResult = sql"select ${abs(v)} from sqlsyntax_spec limit 1".map(_.int(1)).single.apply().get
          intResult should equal(123)
          val longResult = sql"select ${abs(v)} from sqlsyntax_spec limit 1".map(_.long(1)).single.apply().get
          longResult should equal(123L)
        }
        // floor
        {
          val v = sqls"${123.4}"
          val doubleResult = sql"select ${floor(v)} from sqlsyntax_spec limit 1".map(_.double(1)).single.apply().get
          doubleResult should equal(123.0d)
          val floatResult = sql"select ${floor(v)} from sqlsyntax_spec limit 1".map(_.float(1)).single.apply().get
          floatResult should equal(123.0d)
          val intResult = sql"select ${floor(v)} from sqlsyntax_spec limit 1".map(_.int(1)).single.apply().get
          intResult should equal(123)
          val longResult = sql"select ${floor(v)} from sqlsyntax_spec limit 1".map(_.long(1)).single.apply().get
          longResult should equal(123L)
        }
        // ceiling
        {
          val v = sqls"${123.4}"
          val doubleResult = sql"select ${ceiling(v)} from sqlsyntax_spec limit 1".map(_.double(1)).single.apply().get
          doubleResult should equal(124.0d)
          val floatResult = sql"select ${ceiling(v)} from sqlsyntax_spec limit 1".map(_.float(1)).single.apply().get
          floatResult should equal(124.0d)
          val intResult = sql"select ${ceiling(v)} from sqlsyntax_spec limit 1".map(_.int(1)).single.apply().get
          intResult should equal(124)
          val longResult = sql"select ${ceiling(v)} from sqlsyntax_spec limit 1".map(_.long(1)).single.apply().get
          longResult should equal(124L)
        }
        // current_date
        {
          val t = sql"select ${currentDate} from sqlsyntax_spec limit 1".map(_.date(1)).single.apply().get
          log.warn("current_date: " + t + "," + t.getTime)
          t.toLocalDate should equal(LocalDate.now)
        }
        // current_timestamp
        {
          val t = sql"select ${currentTimestamp} from sqlsyntax_spec limit 1".map(_.timestamp(1)).single.apply().get
          log.warn("current_timestamp: " + t + "," + t.getTime)
          t.toDateTime.getMillis should be < (DateTime.now.plusDays(1).getMillis)
        }
      }
    } finally {
      DB autoCommit { implicit s =>
        sql"drop table sqlsyntax_spec".execute.apply()
      }
    }
  }

}
