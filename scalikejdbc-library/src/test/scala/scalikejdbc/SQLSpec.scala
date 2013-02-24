package scalikejdbc

import util.control.Exception._
import org.scalatest._
import org.scalatest.matchers._
import org.scalatest.BeforeAndAfter
import java.sql.PreparedStatement

class SQLSpec extends FlatSpec with ShouldMatchers with BeforeAndAfter with Settings {

  val tableNamePrefix = "emp_SQLSpec" + System.currentTimeMillis()

  behavior of "SQL"

  it should "execute one-to-one queries" in {
    val suffix = "_onetoone_" + System.currentTimeMillis()
    try {
      DB autoCommit { implicit s =>

        SQL("create table users_" + suffix + " (id int not null, group_id int)").execute.apply()
        SQL("create table groups_" + suffix + " (id int not null, name varchar(30))").execute.apply()
        SQL("insert into users_" + suffix + " values (1,1)").update.apply()
        SQL("insert into users_" + suffix + " values (2,1)").update.apply()
        SQL("insert into users_" + suffix + " values (3,1)").update.apply()
        SQL("insert into users_" + suffix + " values (4,1)").update.apply()
        SQL("insert into users_" + suffix + " values (5,2)").update.apply()
        SQL("insert into users_" + suffix + " values (6,2)").update.apply()
        SQL("insert into users_" + suffix + " values (7,null)").update.apply()
        SQL("insert into groups_" + suffix + " values (1, 'A')").update.apply()
        SQL("insert into groups_" + suffix + " values (2, 'B')").update.apply()
        SQL("insert into groups_" + suffix + " values (3, 'C')").update.apply()

        case class User(id: Int, groupId: Int, group: Option[Group] = None)
        case class Group(id: Int, name: String)

        {
          val users = SQL("select u.id as u_id, u.group_id as u_group_id, g.id as g_id, g.name as g_name " +
            " from users_" + suffix + " u inner join groups_" + suffix + " g " +
            " on u.group_id = g.id")
            .one(rs => User(rs.int("u_id"), rs.int("u_group_id"), None))
            .toOne[Group](rs => rs.intOpt("g_id").map(id => Group(id, rs.string("g_name"))))
            .map((u: User, g: Group) => u.copy(group = Option(g)))
            .list.apply()

          users.size should equal(6)
          users.foreach { user => user.group should not be (Some) }
        }

        {
          val users = SQL("select u.id as u_id, u.group_id as u_group_id, g.id as g_id, g.name as g_name " +
            " from users_" + suffix + " u left join groups_" + suffix + " g " +
            " on u.group_id = g.id where u.id = 7")
            .one(rs => User(rs.int("u_id"), rs.int("u_group_id"), None))
            .toOne[Group](rs => rs.intOpt("g_id").map(id => Group(id, rs.string("g_name"))))
            .map((u: User, g: Group) => u.copy(group = Option(g)))
            .list.apply()

          users.size should equal(1)
          users.foreach { user => user.group should be(None) }
        }

      }
    } finally {
      DB autoCommit { implicit s =>
        SQL("drop table users_" + suffix)
        SQL("drop table groups_" + suffix)
      }
    }
  }

  it should "execute one-to-many queries" in {
    val suffix = "_onetomany_" + System.currentTimeMillis()
    try {
      DB autoCommit { implicit s =>

        SQL("create table users_" + suffix + " (id int not null)").execute.apply()
        SQL("create table groups_" + suffix + " (id int not null, name varchar(30))").execute.apply()
        SQL("create table group_members_" + suffix + " (user_id int not null, group_id int not null)").execute.apply()
        SQL("insert into users_" + suffix + " values (1)").update.apply()
        SQL("insert into users_" + suffix + " values (2)").update.apply()
        SQL("insert into users_" + suffix + " values (3)").update.apply()
        SQL("insert into users_" + suffix + " values (4)").update.apply()
        SQL("insert into users_" + suffix + " values (5)").update.apply()
        SQL("insert into users_" + suffix + " values (6)").update.apply()
        SQL("insert into groups_" + suffix + " values (1, 'A')").update.apply()
        SQL("insert into groups_" + suffix + " values (2, 'B')").update.apply()
        SQL("insert into groups_" + suffix + " values (3, 'C')").update.apply()
        SQL("insert into group_members_" + suffix + " values (1,1)").update.apply()
        SQL("insert into group_members_" + suffix + " values (2,1)").update.apply()
        SQL("insert into group_members_" + suffix + " values (3,1)").update.apply()
        SQL("insert into group_members_" + suffix + " values (4,1)").update.apply()
        SQL("insert into group_members_" + suffix + " values (5,1)").update.apply()
        SQL("insert into group_members_" + suffix + " values (6,1)").update.apply()
        SQL("insert into group_members_" + suffix + " values (1,2)").update.apply()
        SQL("insert into group_members_" + suffix + " values (2,2)").update.apply()
        SQL("insert into group_members_" + suffix + " values (3,2)").update.apply()
        SQL("insert into group_members_" + suffix + " values (4,2)").update.apply()

        case class User(id: Int)
        case class Group(id: Int, name: String, members: List[User] = Nil)

        {
          val groups = SQL("select u.id as u_id, g.id as g_id, g.name as g_name " +
            " from group_members_" + suffix + " gm" +
            " inner join users_" + suffix + " u on u.id = gm.user_id" +
            " inner join groups_" + suffix + " g on g.id = gm.group_id" +
            " order by g.id")
            .one(rs => Group(rs.int("g_id"), rs.string("g_name")))
            .toMany[User](rs => rs.intOpt("u_id").map(id => User(id)))
            .map((g: Group, ms: List[User]) => g.copy(members = ms))
            .list.apply()

          groups.size should equal(2)
          groups(0).members.size should equal(6)
          groups(1).members.size should equal(4)
        }
      }
    } finally {
      DB autoCommit { implicit s =>
        SQL("drop table users_" + suffix)
        SQL("drop table groups_" + suffix)
        SQL("drop table group_membergs_" + suffix)
      }
    }
  }

  it should "execute insert with nullable values" in {
    val tableName = tableNamePrefix + "_insertWithNullableValues"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(new DB(ConnectionPool.borrow())) { db =>
        implicit val session = db.autoCommitSession()

        SQL("insert into " + tableName + " values (?, ?)").bind(3, Option("Ben")).execute().apply()

        val benOpt = SQL("select id,name from " + tableName + " where id = ?").bind(3)
          .map(rs => (rs.int("id"), rs.string("name"))).toOption()
          .apply()

        benOpt.get._1 should equal(3)
        benOpt.get._2 should equal("Ben")

        SQL("insert into " + tableName + " values (?, ?)").bind(4, Option(null)).execute().apply()

        val noName = SQL("select id,name from " + tableName + " where id = ?").bind(4)
          .map(rs => (rs.int("id"), rs.string("name"))).toOption
          .apply()

        noName.get._1 should equal(4)
        noName.get._2 should equal(null)

        val before = (s: PreparedStatement) => println("before")
        val after = (s: PreparedStatement) => println("before")
        SQL("insert into " + tableName + " values (?, ?)").bind(5, Option(null)).executeWithFilters(before, after).apply()
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

        val singleResult = SQL("select id from " + tableName + " where id = ?").bind(1)
          .map(rs => rs.string("id")).toOption().apply()
        singleResult.get should equal("1")

        val firstResult = SQL("select id from " + tableName).map(rs => rs.string("id")).headOption().apply()
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
        val result = SQL("select id from " + tableName).map(rs => rs.string("id")).toList().apply()
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
        val result = SQL("select id from " + tableName + "").foldLeft[List[String]](Nil) {
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
        val count = SQL("update " + tableName + " set name = ? where id = ?").bind("foo", 1).executeUpdate.apply()

        val before = (s: PreparedStatement) => println("before")
        val after = (s: PreparedStatement) => println("before")
        SQL("update " + tableName + " set name = ? where id = ?").bind("foo", 1).executeUpdateWithFilters(before, after).apply()

        db.rollbackIfActive()
        count should equal(1)
        val name = SQL("select name from " + tableName + " where id = ?").bind(1)
          .map(rs => rs.string("name")).toOption().apply().getOrElse("---")
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
        val count = SQL("update " + tableName + " set name = ? where id = ?").bind("foo", 1).update.apply()

        val before = (s: PreparedStatement) => println("before")
        val after = (s: PreparedStatement) => println("before")
        SQL("update " + tableName + " set name = ? where id = ?").bind("foo", 1).updateWithFilters(before, after).apply()

        db.rollbackIfActive()
        count should equal(1)
        val name = SQL("select name from " + tableName + " where id = ?").bind(1)
          .map(rs => rs.string("name")).toOption().apply().getOrElse("---")
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
        val result = SQL("select id from " + tableName + " where id = ?").bind(1).map(rs => rs.string("id")).toOption().apply()
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
        val result = SQL("select id from " + tableName + "").map {
          rs => rs.string("id")
        }.toList().apply()
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
        val result = SQL("select id from " + tableName + "").foldLeft[List[String]](Nil) {
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
        val nameBefore = SQL("select name from " + tableName + " where id = ?").bind(1).map {
          rs => rs.string("name")
        }.toOption().apply()
        nameBefore.get should equal("name1")
        val count = SQL("update " + tableName + " set name = ? where id = ?").bind("foo", 1).executeUpdate().apply()
        count should equal(1)
        db.rollback()
      }
      DB readOnly { implicit session =>
        val name = SQL("select name from " + tableName + " where id = ?").bind(1).map {
          rs => rs.string("name")
        }.toOption().apply()
        name.get should equal("name1")
      }
    }
  }

}
