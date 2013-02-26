package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._
import org.scalatest.BeforeAndAfter

class RelationalSQLSpec extends FlatSpec with ShouldMatchers with BeforeAndAfter with Settings {

  val tableNamePrefix = "emp_RelationalSQLSpec" + System.currentTimeMillis()

  behavior of "RelationalSQL"

  it should "execute one-to-one queries" in {
    val suffix = "_onetoone_" + System.currentTimeMillis()
    try {
      DB autoCommit {
        implicit s =>

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
              " on u.group_id = g.id order by u.id")
              .one(rs => User(rs.int("u_id"), rs.int("u_group_id"), None))
              .toOne(rs => rs.intOpt("g_id").map(id => Group(id, rs.string("g_name"))))
              .map((u: User, g: Group) => u.copy(group = Option(g)))
              .list.apply()

            users.size should equal(6)
            users.foreach {
              user => user.group should not be (Some)
            }
            users(0).id should equal(1)
            users(1).id should equal(2)
            users(2).id should equal(3)
            users(3).id should equal(4)
            users(4).id should equal(5)
            users(5).id should equal(6)
          }

          {
            val users = SQL("select u.id as u_id, u.group_id as u_group_id, g.id as g_id, g.name as g_name " +
              " from users_" + suffix + " u inner join groups_" + suffix + " g " +
              " on u.group_id = g.id order by u.id")
              .one(rs => User(rs.int("u_id"), rs.int("u_group_id"), None))
              .toOne(rs => rs.intOpt("g_id").map(id => Group(id, rs.string("g_name"))))
              .map((u: User, g: Group) => u.copy(group = Option(g)))
              .traversable.apply()

            users.size should equal(6)
            users.foreach {
              user => user.group should not be (Some)
            }
          }

          {
            val user = SQL("select u.id as u_id, u.group_id as u_group_id, g.id as g_id, g.name as g_name " +
              " from users_" + suffix + " u inner join groups_" + suffix + " g " +
              " on u.group_id = g.id where u.id = 1")
              .one(rs => User(rs.int("u_id"), rs.int("u_group_id"), None))
              .toOne(rs => rs.intOpt("g_id").map(id => Group(id, rs.string("g_name"))))
              .map((u: User, g: Group) => u.copy(group = Option(g)))
              .single.apply()

            user.get.id should equal(1)
          }

          {
            intercept[TooManyRowsException] {
              SQL("select u.id as u_id, u.group_id as u_group_id, g.id as g_id, g.name as g_name " +
                " from users_" + suffix + " u inner join groups_" + suffix + " g " +
                " on u.group_id = g.id order by u.id")
                .one(rs => User(rs.int("u_id"), rs.int("u_group_id"), None))
                .toOne(rs => rs.intOpt("g_id").map(id => Group(id, rs.string("g_name"))))
                .map((u: User, g: Group) => u.copy(group = Option(g)))
                .single.apply()
            }
          }

          {
            val users = SQL("select u.id as u_id, u.group_id as u_group_id, g.id as g_id, g.name as g_name " +
              " from users_" + suffix + " u left join groups_" + suffix + " g " +
              " on u.group_id = g.id where u.id = 7")
              .one(rs => User(rs.int("u_id"), rs.int("u_group_id"), None))
              .toOne(rs => rs.intOpt("g_id").map(id => Group(id, rs.string("g_name"))))
              .map((u: User, g: Group) => u.copy(group = Option(g)))
              .list.apply()

            users.size should equal(1)
            users.foreach {
              user => user.group should be(None)
            }
          }

      }
    } finally {
      DB autoCommit {
        implicit s =>
          SQL("drop table users_" + suffix)
          SQL("drop table groups_" + suffix)
      }
    }
  }

  it should "execute one-to-many queries" in {
    val suffix = "_onetomany_" + System.currentTimeMillis()
    try {
      DB autoCommit {
        implicit s =>

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
          case class Group(id: Int, name: String, members: Seq[User] = Nil)

          {
            val groups = SQL("select u.id as u_id, g.id as g_id, g.name as g_name " +
              " from group_members_" + suffix + " gm" +
              " inner join users_" + suffix + " u on u.id = gm.user_id" +
              " inner join groups_" + suffix + " g on g.id = gm.group_id" +
              " order by g.id, u.id desc")
              .one(rs => Group(rs.int("g_id"), rs.string("g_name")))
              .toMany(rs => rs.intOpt("u_id").map(id => User(id)))
              .map((g: Group, ms: Seq[User]) => g.copy(members = ms))
              .list.apply()

            groups.size should equal(2)
            groups(0).members.size should equal(6)
            groups(0).members(0).id should equal(6)
            groups(0).members(1).id should equal(5)
            groups(0).members(2).id should equal(4)
            groups(0).members(3).id should equal(3)
            groups(0).members(4).id should equal(2)
            groups(0).members(5).id should equal(1)
            groups(1).members.size should equal(4)
            groups(1).members(0).id should equal(4)
            groups(1).members(1).id should equal(3)
            groups(1).members(2).id should equal(2)
            groups(1).members(3).id should equal(1)
          }

          {
            val groups = SQL("select u.id as u_id, g.id as g_id, g.name as g_name " +
              " from group_members_" + suffix + " gm" +
              " inner join users_" + suffix + " u on u.id = gm.user_id" +
              " inner join groups_" + suffix + " g on g.id = gm.group_id" +
              " order by g.id, u.id desc")
              .one(rs => Group(rs.int("g_id"), rs.string("g_name")))
              .toMany(rs => rs.intOpt("u_id").map(id => User(id)))
              .map((g: Group, ms: Seq[User]) => g.copy(members = ms))
              .traversable.apply()

            groups.size should equal(2)
          }

          {
            val group = SQL("select u.id as u_id, g.id as g_id, g.name as g_name " +
              " from group_members_" + suffix + " gm" +
              " inner join users_" + suffix + " u on u.id = gm.user_id" +
              " inner join groups_" + suffix + " g on g.id = gm.group_id" +
              " where g.id = 1")
              .one(rs => Group(rs.int("g_id"), rs.string("g_name")))
              .toMany(rs => rs.intOpt("u_id").map(id => User(id)))
              .map((g: Group, ms: Seq[User]) => g.copy(members = ms))
              .single.apply()

            group.get.id should equal(1)
          }

          {
            intercept[TooManyRowsException] {
              SQL("select u.id as u_id, g.id as g_id, g.name as g_name " +
                " from group_members_" + suffix + " gm" +
                " inner join users_" + suffix + " u on u.id = gm.user_id" +
                " inner join groups_" + suffix + " g on g.id = gm.group_id" +
                " order by g.id, u.id desc")
                .one(rs => Group(rs.int("g_id"), rs.string("g_name")))
                .toMany(rs => rs.intOpt("u_id").map(id => User(id)))
                .map((g: Group, ms: Seq[User]) => g.copy(members = ms))
                .single.apply()
            }
          }
      }
    } finally {
      DB autoCommit {
        implicit s =>
          SQL("drop table users_" + suffix)
          SQL("drop table groups_" + suffix)
          SQL("drop table group_membergs_" + suffix)
      }
    }
  }

}
