package scalikejdbc

import org.scalatest._
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class RelationalSQLSpec
  extends AnyFlatSpec
  with Matchers
  with BeforeAndAfter
  with Settings {

  val tableNamePrefix = "emp_RelationalSQLSpec" + System.currentTimeMillis()

  behavior of "RelationalSQL"

  it should "execute one-to-one queries" in {
    val suffix = "_onetoone_" + System.currentTimeMillis()
    try {
      DB autoCommit { implicit s =>
        SQL(
          "create table users_" + suffix + " (id int not null, group_id int)"
        ).execute.apply()
        SQL(
          "create table groups_" + suffix + " (id int not null, name varchar(30))"
        ).execute.apply()
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
          val sqlString = "select u.id as u_id, u.group_id as u_group_id, g.id as g_id, g.name as g_name " +
            " from users_" + suffix + " u inner join groups_" + suffix + " g " +
            " on u.group_id = g.id order by u.id"
          val sql = SQL[User](sqlString)
          val users: List[User] = sql
            .one(rs => User(rs.int("u_id"), rs.int("u_group_id"), None))
            .toOne(rs => Group(rs.int("g_id"), rs.string("g_name")))
            .map((u: User, g: Group) => u.copy(group = Some(g)))
            .list
            .apply()

          users.size should equal(6)
          users.foreach { user =>
            user.group should not be (Some)
          }
          users(0).id should equal(1)
          users(1).id should equal(2)
          users(2).id should equal(3)
          users(3).id should equal(4)
          users(4).id should equal(5)
          users(5).id should equal(6)
        }

        {
          val users = SQL(
            "select u.id as u_id, u.group_id as u_group_id, g.id as g_id, g.name as g_name " +
              " from users_" + suffix + " u inner join groups_" + suffix + " g " +
              " on u.group_id = g.id order by u.id"
          )
            .one(rs => User(rs.int("u_id"), rs.int("u_group_id"), None))
            .toOne(rs => Group(rs.int("g_id"), rs.string("g_name")))
            .map((u: User, g: Group) => u.copy(group = Some(g)))
            .iterable
            .apply()

          users.size should equal(6)
          users.foreach { user =>
            user.group should not be (Some)
          }
        }

        {
          val user = SQL(
            "select u.id as u_id, u.group_id as u_group_id, g.id as g_id, g.name as g_name " +
              " from users_" + suffix + " u inner join groups_" + suffix + " g " +
              " on u.group_id = g.id where u.id = 1"
          )
            .one(rs => User(rs.int("u_id"), rs.int("u_group_id"), None))
            .toOne(rs => Group(rs.int("g_id"), rs.string("g_name")))
            .map((u: User, g: Group) => u.copy(group = Some(g)))
            .single
            .apply()

          user.get.id should equal(1)
        }

        {
          intercept[TooManyRowsException] {
            SQL(
              "select u.id as u_id, u.group_id as u_group_id, g.id as g_id, g.name as g_name " +
                " from users_" + suffix + " u inner join groups_" + suffix + " g " +
                " on u.group_id = g.id order by u.id"
            )
              .one(rs => User(rs.int("u_id"), rs.int("u_group_id"), None))
              .toOne(rs => Group(rs.int("g_id"), rs.string("g_name")))
              .map((u: User, g: Group) => u.copy(group = Some(g)))
              .single
              .apply()
          }
        }

        {
          val users = SQL(
            "select u.id as u_id, u.group_id as u_group_id, g.id as g_id, g.name as g_name " +
              " from users_" + suffix + " u left join groups_" + suffix + " g " +
              " on u.group_id = g.id where u.id = 7"
          )
            .one(rs =>
              User(
                rs.intOpt("u_id").getOrElse(0),
                rs.intOpt("u_group_id").getOrElse(0),
                None
              )
            )
            .toOptionalOne(rs =>
              rs.intOpt("g_id").map(id => Group(id, rs.string("g_name")))
            )
            .map((u: User, g: Option[Group]) => u.copy(group = g))
            .list
            .apply()

          users.size should equal(1)
          users.foreach { _.group should be(None) }
        }

        {
          val users = SQL(
            "select u.id as u_id, u.group_id as u_group_id, g.id as g_id, g.name as g_name " +
              " from users_" + suffix + " u left join groups_" + suffix + " g " +
              " on u.group_id = g.id where u.id = 7"
          )
            .one(rs =>
              User(
                rs.intOpt("u_id").getOrElse(0),
                rs.intOpt("u_group_id").getOrElse(0),
                None
              )
            )
            .toOptionalOne(rs =>
              rs.intOpt("g_id").map(id => Group(id, rs.string("g_name")))
            )
            .map((u: User, g: Option[Group]) => u.copy(group = g))
            .collection[Vector]()

          users.size should equal(1)
          users.foreach { _.group should be(None) }
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
        SQL("create table users_" + suffix + " (id int not null)").execute
          .apply()
        SQL(
          "create table groups_" + suffix + " (id int not null, name varchar(30))"
        ).execute.apply()
        SQL(
          "create table group_members_" + suffix + " (user_id int not null, group_id int not null)"
        ).execute.apply()
        SQL("insert into users_" + suffix + " values (1)").update.apply()
        SQL("insert into users_" + suffix + " values (2)").update.apply()
        SQL("insert into users_" + suffix + " values (3)").update.apply()
        SQL("insert into users_" + suffix + " values (4)").update.apply()
        SQL("insert into users_" + suffix + " values (5)").update.apply()
        SQL("insert into users_" + suffix + " values (6)").update.apply()
        SQL("insert into groups_" + suffix + " values (1, 'A')").update.apply()
        SQL("insert into groups_" + suffix + " values (2, 'B')").update.apply()
        SQL("insert into groups_" + suffix + " values (3, 'C')").update.apply()
        SQL("insert into group_members_" + suffix + " values (1,1)").update
          .apply()
        SQL("insert into group_members_" + suffix + " values (2,1)").update
          .apply()
        SQL("insert into group_members_" + suffix + " values (3,1)").update
          .apply()
        SQL("insert into group_members_" + suffix + " values (4,1)").update
          .apply()
        SQL("insert into group_members_" + suffix + " values (5,1)").update
          .apply()
        SQL("insert into group_members_" + suffix + " values (6,1)").update
          .apply()
        SQL("insert into group_members_" + suffix + " values (1,2)").update
          .apply()
        SQL("insert into group_members_" + suffix + " values (2,2)").update
          .apply()
        SQL("insert into group_members_" + suffix + " values (3,2)").update
          .apply()
        SQL("insert into group_members_" + suffix + " values (4,2)").update
          .apply()

        case class User(id: Int)
        case class Group(
          id: Int,
          name: String,
          members: collection.Seq[User] = Nil
        )

        {
          val groups = SQL(
            "select u.id as u_id, g.id as g_id, g.name as g_name " +
              " from group_members_" + suffix + " gm" +
              " inner join users_" + suffix + " u on u.id = gm.user_id" +
              " inner join groups_" + suffix + " g on g.id = gm.group_id" +
              " order by g.id, u.id desc"
          )
            .one(rs => Group(rs.int("g_id"), rs.string("g_name")))
            .toMany(rs => Some(User(rs.int("u_id"))))
            .map((g: Group, ms: collection.Seq[User]) => g.copy(members = ms))
            .list
            .apply()

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
          val groups = SQL(
            "select u.id as u_id, g.id as g_id, g.name as g_name " +
              " from group_members_" + suffix + " gm" +
              " inner join users_" + suffix + " u on u.id = gm.user_id" +
              " inner join groups_" + suffix + " g on g.id = gm.group_id" +
              " order by g.id, u.id desc"
          )
            .one(rs => Group(rs.int("g_id"), rs.string("g_name")))
            .toMany(rs => Some(User(rs.int("u_id"))))
            .map((g: Group, ms: collection.Seq[User]) => g.copy(members = ms))
            .collection[Vector]()

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
          val groups = SQL(
            "select u.id as u_id, g.id as g_id, g.name as g_name " +
              " from group_members_" + suffix + " gm" +
              " inner join users_" + suffix + " u on u.id = gm.user_id" +
              " inner join groups_" + suffix + " g on g.id = gm.group_id" +
              " order by g.id, u.id desc"
          )
            .one(rs => Group(rs.int("g_id"), rs.string("g_name")))
            .toMany(rs => Some(User(rs.int("u_id"))))
            .map((g: Group, ms: collection.Seq[User]) => g.copy(members = ms))
            .iterable
            .apply()

          groups.size should equal(2)
        }

        {
          val group = SQL(
            "select u.id as u_id, g.id as g_id, g.name as g_name " +
              " from group_members_" + suffix + " gm" +
              " inner join users_" + suffix + " u on u.id = gm.user_id" +
              " inner join groups_" + suffix + " g on g.id = gm.group_id" +
              " where g.id = 1"
          )
            .one(rs => Group(rs.int("g_id"), rs.string("g_name")))
            .toMany(rs => Some(User(rs.int("u_id"))))
            .map((g: Group, ms: collection.Seq[User]) => g.copy(members = ms))
            .single
            .apply()

          group.get.id should equal(1)
        }

        {
          intercept[TooManyRowsException] {
            SQL(
              "select u.id as u_id, g.id as g_id, g.name as g_name " +
                " from group_members_" + suffix + " gm" +
                " inner join users_" + suffix + " u on u.id = gm.user_id" +
                " inner join groups_" + suffix + " g on g.id = gm.group_id" +
                " order by g.id, u.id desc"
            )
              .one(rs => Group(rs.int("g_id"), rs.string("g_name")))
              .toMany(rs => Some(User(rs.int("u_id"))))
              .map((g: Group, ms: collection.Seq[User]) => g.copy(members = ms))
              .single
              .apply()
          }
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

  it should "execute one-to-manies2 queries" in {
    val suffix = "_onetomanies2_" + System.currentTimeMillis()
    try {
      DB autoCommit { implicit s =>
        SQL("create table groups_" + suffix + " (id int not null)").execute
          .apply()
        SQL(
          "create table members_" + suffix + " (id int not null, group_id int not null)"
        ).execute.apply()
        SQL(
          "create table sponsors_" + suffix + " (id int not null, group_id int not null)"
        ).execute.apply()

        SQL("insert into groups_" + suffix + " values (1)").update.apply()
        SQL("insert into groups_" + suffix + " values (2)").update.apply()
        SQL("insert into groups_" + suffix + " values (3)").update.apply()
        SQL("insert into groups_" + suffix + " values (4)").update.apply()

        SQL("insert into members_" + suffix + " values (1, 2)").update.apply()
        SQL("insert into members_" + suffix + " values (2, 1)").update.apply()
        SQL("insert into members_" + suffix + " values (3, 1)").update.apply()
        SQL("insert into members_" + suffix + " values (4, 2)").update.apply()
        SQL("insert into members_" + suffix + " values (5, 1)").update.apply()

        SQL("insert into sponsors_" + suffix + " values (1, 1)").update.apply()
        SQL("insert into sponsors_" + suffix + " values (2, 2)").update.apply()
        SQL("insert into sponsors_" + suffix + " values (3, 3)").update.apply()
        SQL("insert into sponsors_" + suffix + " values (4, 2)").update.apply()
        SQL("insert into sponsors_" + suffix + " values (5, 3)").update.apply()

        case class Group(
          id: Int,
          members: collection.Seq[Member] = Nil,
          sponsors: collection.Seq[Sponsor] = Nil
        )
        case class Member(id: Int, groupId: Int)
        case class Sponsor(id: Int, groupId: Int)

        {
          val groups: List[Group] = SQL(
            "select g.id as g_id, m.id as m_id, s.id as s_id " +
              " from groups_" + suffix + " g " +
              " left join members_" + suffix + " m on g.id = m.group_id" +
              " left join sponsors_" + suffix + " s on g.id = s.group_id" +
              " order by g.id, m.id desc"
          )
            .one(rs => Group(rs.int("g_id")))
            .toManies(
              rs => rs.intOpt("m_id").map(id => Member(id, rs.int("g_id"))),
              rs => rs.intOpt("s_id").map(id => Sponsor(id, rs.int("g_id")))
            )
            .map(
              (
                g: Group,
                ms: collection.Seq[Member],
                ss: collection.Seq[Sponsor]
              ) => g.copy(members = ms, sponsors = ss)
            )
            .list
            .apply()

          groups.size should equal(4)
          groups(0).id should equal(1)

          groups(0).members.size should equal(3)
          groups(0).members(0).id should equal(5)
          groups(0).members(1).id should equal(3)
          groups(0).members(2).id should equal(2)

          groups(0).sponsors.size should equal(1)
          groups(0).sponsors(0).id should equal(1)
        }

        {
          val groups: Vector[Group] = SQL(
            "select g.id as g_id, m.id as m_id, s.id as s_id " +
              " from groups_" + suffix + " g " +
              " left join members_" + suffix + " m on g.id = m.group_id" +
              " left join sponsors_" + suffix + " s on g.id = s.group_id" +
              " order by g.id, m.id desc"
          )
            .one(rs => Group(rs.int("g_id")))
            .toManies(
              rs => rs.intOpt("m_id").map(id => Member(id, rs.int("g_id"))),
              rs => rs.intOpt("s_id").map(id => Sponsor(id, rs.int("g_id")))
            )
            .map(
              (
                g: Group,
                ms: collection.Seq[Member],
                ss: collection.Seq[Sponsor]
              ) => g.copy(members = ms, sponsors = ss)
            )
            .collection[Vector]()

          groups.size should equal(4)
          groups(0).id should equal(1)

          groups(0).members.size should equal(3)
          groups(0).members(0).id should equal(5)
          groups(0).members(1).id should equal(3)
          groups(0).members(2).id should equal(2)

          groups(0).sponsors.size should equal(1)
          groups(0).sponsors(0).id should equal(1)
        }

        {
          val group: Group = SQL(
            "select g.id as g_id, m.id as m_id, s.id as s_id " +
              " from groups_" + suffix + " g " +
              " left join members_" + suffix + " m on g.id = m.group_id" +
              " left join sponsors_" + suffix + " s on g.id = s.group_id" +
              " where g.id = 1 order by g.id, m.id desc"
          )
            .one(rs => Group(rs.int("g_id")))
            .toManies(
              rs => rs.intOpt("m_id").map(id => Member(id, rs.int("g_id"))),
              rs => rs.intOpt("s_id").map(id => Sponsor(id, rs.int("g_id")))
            )
            .map(
              (
                g: Group,
                ms: collection.Seq[Member],
                ss: collection.Seq[Sponsor]
              ) => g.copy(members = ms, sponsors = ss)
            )
            .single
            .apply()
            .get

          group.id should equal(1)

          group.members.size should equal(3)
          group.members(0).id should equal(5)
          group.members(1).id should equal(3)
          group.members(2).id should equal(2)

          group.sponsors.size should equal(1)
          group.sponsors(0).id should equal(1)
        }
      }
    } finally {
      DB autoCommit { implicit s =>
        SQL("drop table groups_" + suffix)
        SQL("drop table members_" + suffix)
        SQL("drop table sponsors_" + suffix)
      }
    }
  }

  it should "execute one-to-manies3 queries" in {
    val suffix = "_onetomanies3_" + System.currentTimeMillis()
    try {
      DB autoCommit { implicit s =>
        SQL(
          "create table groups_" + suffix + " (id int not null, owner_id int not null)"
        ).execute.apply()
        SQL("create table owners_" + suffix + " (id int not null)").execute
          .apply()
        SQL(
          "create table members_" + suffix + " (id int not null, group_id int not null)"
        ).execute.apply()
        SQL(
          "create table sponsors_" + suffix + " (id int not null, group_id int not null)"
        ).execute.apply()

        SQL("insert into groups_" + suffix + " values (1, 2)").update.apply()
        SQL("insert into groups_" + suffix + " values (2, 2)").update.apply()
        SQL("insert into groups_" + suffix + " values (3, 1)").update.apply()
        SQL("insert into groups_" + suffix + " values (4, 2)").update.apply()

        SQL("insert into owners_" + suffix + " values (1)").update.apply()
        SQL("insert into owners_" + suffix + " values (2)").update.apply()

        SQL("insert into members_" + suffix + " values (1, 2)").update.apply()
        SQL("insert into members_" + suffix + " values (2, 1)").update.apply()
        SQL("insert into members_" + suffix + " values (3, 1)").update.apply()
        SQL("insert into members_" + suffix + " values (4, 2)").update.apply()
        SQL("insert into members_" + suffix + " values (5, 1)").update.apply()

        SQL("insert into sponsors_" + suffix + " values (1, 1)").update.apply()
        SQL("insert into sponsors_" + suffix + " values (2, 2)").update.apply()
        SQL("insert into sponsors_" + suffix + " values (3, 3)").update.apply()
        SQL("insert into sponsors_" + suffix + " values (4, 2)").update.apply()
        SQL("insert into sponsors_" + suffix + " values (5, 3)").update.apply()

        case class GroupEntity(id: Int, ownerId: Int)
        case class Group(
          id: Int,
          ownerId: Int,
          owner: Owner,
          members: collection.Seq[Member] = Nil,
          sponsors: collection.Seq[Sponsor] = Nil
        )
        case class Owner(id: Int)
        case class Member(id: Int, groupId: Int)
        case class Sponsor(id: Int, groupId: Int)

        {
          val groups: List[Group] = SQL(
            "select g.id as g_id, g.owner_id as g_owner_id, o.id as o_id, m.id as m_id, s.id as s_id " +
              " from groups_" + suffix + " g " +
              " inner join owners_" + suffix + " o on g.owner_id = o.id " +
              " left join members_" + suffix + " m on g.id = m.group_id " +
              " left join sponsors_" + suffix + " s on g.id = s.group_id " +
              " order by g.id, m.id desc"
          )
            .one(rs => GroupEntity(rs.int("g_id"), rs.int("g_owner_id")))
            .toManies(
              rs => rs.intOpt("o_id").map(id => Owner(id)),
              rs => rs.intOpt("m_id").map(id => Member(id, rs.int("g_id"))),
              rs => rs.intOpt("s_id").map(id => Sponsor(id, rs.int("g_id")))
            )
            .map { (g, os, ms, ss) =>
              Group(
                id = g.id,
                ownerId = g.ownerId,
                owner = os.head,
                members = ms,
                sponsors = ss
              )
            }
            .list
            .apply()

          groups.size should equal(4)
          groups(0).id should equal(1)

          groups(0).owner.id should equal(2)

          groups(0).members.size should equal(3)
          groups(0).members(0).id should equal(5)
          groups(0).members(1).id should equal(3)
          groups(0).members(2).id should equal(2)

          groups(0).sponsors.size should equal(1)
          groups(0).sponsors(0).id should equal(1)
        }

        {
          val groups: Vector[Group] = SQL(
            "select g.id as g_id, g.owner_id as g_owner_id, o.id as o_id, m.id as m_id, s.id as s_id " +
              " from groups_" + suffix + " g " +
              " inner join owners_" + suffix + " o on g.owner_id = o.id " +
              " left join members_" + suffix + " m on g.id = m.group_id " +
              " left join sponsors_" + suffix + " s on g.id = s.group_id " +
              " order by g.id, m.id desc"
          )
            .one(rs => GroupEntity(rs.int("g_id"), rs.int("g_owner_id")))
            .toManies(
              rs => rs.intOpt("o_id").map(id => Owner(id)),
              rs => rs.intOpt("m_id").map(id => Member(id, rs.int("g_id"))),
              rs => rs.intOpt("s_id").map(id => Sponsor(id, rs.int("g_id")))
            )
            .map { (g, os, ms, ss) =>
              Group(
                id = g.id,
                ownerId = g.ownerId,
                owner = os.head,
                members = ms,
                sponsors = ss
              )
            }
            .collection[Vector]()

          groups.size should equal(4)
          groups(0).id should equal(1)

          groups(0).owner.id should equal(2)

          groups(0).members.size should equal(3)
          groups(0).members(0).id should equal(5)
          groups(0).members(1).id should equal(3)
          groups(0).members(2).id should equal(2)

          groups(0).sponsors.size should equal(1)
          groups(0).sponsors(0).id should equal(1)
        }

        {
          case class GroupId(
            id: Int,
            members: collection.Seq[Member] = Nil,
            sponsors: collection.Seq[Sponsor] = Nil
          )

          val group: Group = SQL(
            "select g.id as g_id, o.id as o_id, m.id as m_id, s.id as s_id " +
              " from groups_" + suffix + " g " +
              " inner join owners_" + suffix + " o on g.owner_id = o.id " +
              " left join members_" + suffix + " m on g.id = m.group_id " +
              " left join sponsors_" + suffix + " s on g.id = s.group_id " +
              " where g.id = 1 order by g.id, m.id desc"
          )
            .one(rs => GroupId(rs.int("g_id")))
            .toManies(
              rs => rs.intOpt("o_id").map(id => Owner(id)),
              rs => rs.intOpt("m_id").map(id => Member(id, rs.int("g_id"))),
              rs => rs.intOpt("s_id").map(id => Sponsor(id, rs.int("g_id")))
            )
            .map { (g, os, ms, ss) =>
              Group(
                id = g.id,
                ownerId = os.head.id,
                owner = os.head,
                members = ms,
                sponsors = ss
              )
            }
            .single
            .apply()
            .get

          group.id should equal(1)

          group.members.size should equal(3)
          group.members(0).id should equal(5)
          group.members(1).id should equal(3)
          group.members(2).id should equal(2)

          group.owner.id should equal(2)

          group.sponsors.size should equal(1)
          group.sponsors(0).id should equal(1)
        }
      }
    } finally {
      DB autoCommit { implicit s =>
        SQL("drop table groups_" + suffix)
        SQL("drop table owners_" + suffix)
        SQL("drop table members_" + suffix)
        SQL("drop table sponsors_" + suffix)
      }
    }
  }

  it should "execute one-to-manies4 queries" in {
    val suffix = "_onetomanies4_" + System.currentTimeMillis()
    try {
      DB autoCommit { implicit s =>
        SQL(
          "create table groups_" + suffix + " (id int not null, owner_id int not null)"
        ).execute.apply()
        SQL("create table owners_" + suffix + " (id int not null)").execute
          .apply()
        SQL(
          "create table events_" + suffix + " (id int not null, group_id int not null)"
        ).execute.apply()
        SQL(
          "create table members_" + suffix + " (id int not null, group_id int not null)"
        ).execute.apply()
        SQL(
          "create table sponsors_" + suffix + " (id int not null, group_id int not null)"
        ).execute.apply()

        SQL("insert into groups_" + suffix + " values (1, 2)").update.apply()
        SQL("insert into groups_" + suffix + " values (2, 2)").update.apply()
        SQL("insert into groups_" + suffix + " values (3, 1)").update.apply()
        SQL("insert into groups_" + suffix + " values (4, 2)").update.apply()

        SQL("insert into owners_" + suffix + " values (1)").update.apply()
        SQL("insert into owners_" + suffix + " values (2)").update.apply()

        SQL("insert into events_" + suffix + " values (1, 2)").update.apply()
        SQL("insert into events_" + suffix + " values (2, 1)").update.apply()
        SQL("insert into events_" + suffix + " values (3, 1)").update.apply()

        SQL("insert into members_" + suffix + " values (1, 2)").update.apply()
        SQL("insert into members_" + suffix + " values (2, 1)").update.apply()
        SQL("insert into members_" + suffix + " values (3, 1)").update.apply()
        SQL("insert into members_" + suffix + " values (4, 2)").update.apply()
        SQL("insert into members_" + suffix + " values (5, 1)").update.apply()

        SQL("insert into sponsors_" + suffix + " values (1, 1)").update.apply()
        SQL("insert into sponsors_" + suffix + " values (2, 2)").update.apply()
        SQL("insert into sponsors_" + suffix + " values (3, 3)").update.apply()
        SQL("insert into sponsors_" + suffix + " values (4, 2)").update.apply()
        SQL("insert into sponsors_" + suffix + " values (5, 3)").update.apply()

        case class GroupEntity(id: Int, ownerId: Int)
        case class Group(
          id: Int,
          ownerId: Int,
          owner: Owner,
          events: collection.Seq[Event] = Nil,
          members: collection.Seq[Member] = Nil,
          sponsors: collection.Seq[Sponsor] = Nil
        )
        case class Owner(id: Int)
        case class Event(id: Int, groupId: Int)
        case class Member(id: Int, groupId: Int)
        case class Sponsor(id: Int, groupId: Int)

        {
          val groups: List[Group] = SQL(
            "select g.id as g_id, g.owner_id as g_owner_id, " +
              " o.id as o_id, e.id as e_id, m.id as m_id, s.id as s_id " +
              " from groups_" + suffix + " g " +
              " inner join owners_" + suffix + " o on g.owner_id = o.id " +
              " left join events_" + suffix + " e on g.id = e.group_id " +
              " left join members_" + suffix + " m on g.id = m.group_id " +
              " left join sponsors_" + suffix + " s on g.id = s.group_id " +
              " order by g.id, e.id desc, m.id desc"
          )
            .one(rs => GroupEntity(rs.int("g_id"), rs.int("g_owner_id")))
            .toManies(
              rs => rs.intOpt("o_id").map(id => Owner(id)),
              rs => rs.intOpt("e_id").map(id => Event(id, rs.int("g_id"))),
              rs => rs.intOpt("m_id").map(id => Member(id, rs.int("g_id"))),
              rs => rs.intOpt("s_id").map(id => Sponsor(id, rs.int("g_id")))
            )
            .map { (g, os, es, ms, ss) =>
              Group(
                id = g.id,
                ownerId = g.ownerId,
                owner = os.head,
                events = es,
                members = ms,
                sponsors = ss
              )
            }
            .list
            .apply()

          groups.size should equal(4)
          groups(0).id should equal(1)

          groups(0).owner.id should equal(2)

          groups(0).events.size should equal(2)
          groups(0).events(0).id should equal(3)
          groups(0).events(1).id should equal(2)

          groups(0).members.size should equal(3)
          groups(0).members(0).id should equal(5)
          groups(0).members(1).id should equal(3)
          groups(0).members(2).id should equal(2)

          groups(0).sponsors.size should equal(1)
          groups(0).sponsors(0).id should equal(1)
        }

        {
          val groups: Vector[Group] = SQL(
            "select g.id as g_id, g.owner_id as g_owner_id, " +
              " o.id as o_id, e.id as e_id, m.id as m_id, s.id as s_id " +
              " from groups_" + suffix + " g " +
              " inner join owners_" + suffix + " o on g.owner_id = o.id " +
              " left join events_" + suffix + " e on g.id = e.group_id " +
              " left join members_" + suffix + " m on g.id = m.group_id " +
              " left join sponsors_" + suffix + " s on g.id = s.group_id " +
              " order by g.id, e.id desc, m.id desc"
          )
            .one(rs => GroupEntity(rs.int("g_id"), rs.int("g_owner_id")))
            .toManies(
              rs => rs.intOpt("o_id").map(id => Owner(id)),
              rs => rs.intOpt("e_id").map(id => Event(id, rs.int("g_id"))),
              rs => rs.intOpt("m_id").map(id => Member(id, rs.int("g_id"))),
              rs => rs.intOpt("s_id").map(id => Sponsor(id, rs.int("g_id")))
            )
            .map { (g, os, es, ms, ss) =>
              Group(
                id = g.id,
                ownerId = g.ownerId,
                owner = os.head,
                events = es,
                members = ms,
                sponsors = ss
              )
            }
            .collection[Vector]()

          groups.size should equal(4)
          groups(0).id should equal(1)

          groups(0).owner.id should equal(2)

          groups(0).events.size should equal(2)
          groups(0).events(0).id should equal(3)
          groups(0).events(1).id should equal(2)

          groups(0).members.size should equal(3)
          groups(0).members(0).id should equal(5)
          groups(0).members(1).id should equal(3)
          groups(0).members(2).id should equal(2)

          groups(0).sponsors.size should equal(1)
          groups(0).sponsors(0).id should equal(1)
        }

        {
          val group: Group = SQL(
            "select g.id as g_id, g.owner_id as g_owner_id, " +
              " o.id as o_id, e.id as e_id, m.id as m_id, s.id as s_id " +
              " from groups_" + suffix + " g " +
              " inner join owners_" + suffix + " o on g.owner_id = o.id " +
              " left join events_" + suffix + " e on g.id = e.group_id " +
              " left join members_" + suffix + " m on g.id = m.group_id " +
              " left join sponsors_" + suffix + " s on g.id = s.group_id " +
              " where g.id = 1 order by g.id, e.id desc, m.id desc"
          )
            .one(rs => GroupEntity(rs.int("g_id"), rs.int("g_owner_id")))
            .toManies(
              rs => rs.intOpt("o_id").map(id => Owner(id)),
              rs => rs.intOpt("e_id").map(id => Event(id, rs.int("g_id"))),
              rs => rs.intOpt("m_id").map(id => Member(id, rs.int("g_id"))),
              rs => rs.intOpt("s_id").map(id => Sponsor(id, rs.int("g_id")))
            )
            .map { (g, os, es, ms, ss) =>
              Group(
                id = g.id,
                ownerId = g.ownerId,
                owner = os.head,
                events = es,
                members = ms,
                sponsors = ss
              )
            }
            .single
            .apply()
            .get

          group.id should equal(1)

          group.owner.id should equal(2)

          group.events.size should equal(2)
          group.events(0).id should equal(3)
          group.events(1).id should equal(2)

          group.members.size should equal(3)
          group.members(0).id should equal(5)
          group.members(1).id should equal(3)
          group.members(2).id should equal(2)

          group.sponsors.size should equal(1)
          group.sponsors(0).id should equal(1)
        }
      }
    } finally {
      DB autoCommit { implicit s =>
        SQL("drop table groups_" + suffix)
        SQL("drop table owners_" + suffix)
        SQL("drop table events_" + suffix)
        SQL("drop table members_" + suffix)
        SQL("drop table sponsors_" + suffix)
      }
    }
  }

  it should "execute one-to-manies5 queries" in {
    val suffix = "_onetomanies5_" + System.currentTimeMillis()
    try {
      DB autoCommit { implicit s =>
        SQL(
          "create table groups_" + suffix + " (id int not null, owner_id int not null)"
        ).execute.apply()
        SQL("create table owners_" + suffix + " (id int not null)").execute
          .apply()
        SQL(
          "create table events_" + suffix + " (id int not null, group_id int not null)"
        ).execute.apply()
        SQL(
          "create table news_" + suffix + " (id int not null, group_id int not null)"
        ).execute.apply()
        SQL(
          "create table members_" + suffix + " (id int not null, group_id int not null)"
        ).execute.apply()
        SQL(
          "create table sponsors_" + suffix + " (id int not null, group_id int not null)"
        ).execute.apply()

        SQL("insert into groups_" + suffix + " values (1, 2)").update.apply()
        SQL("insert into groups_" + suffix + " values (2, 2)").update.apply()
        SQL("insert into groups_" + suffix + " values (3, 1)").update.apply()
        SQL("insert into groups_" + suffix + " values (4, 2)").update.apply()

        SQL("insert into owners_" + suffix + " values (1)").update.apply()
        SQL("insert into owners_" + suffix + " values (2)").update.apply()

        SQL("insert into events_" + suffix + " values (1, 2)").update.apply()
        SQL("insert into events_" + suffix + " values (2, 1)").update.apply()
        SQL("insert into events_" + suffix + " values (3, 1)").update.apply()

        SQL("insert into news_" + suffix + " values (1, 2)").update.apply()
        SQL("insert into news_" + suffix + " values (2, 1)").update.apply()
        SQL("insert into news_" + suffix + " values (3, 2)").update.apply()
        SQL("insert into news_" + suffix + " values (4, 2)").update.apply()
        SQL("insert into news_" + suffix + " values (5, 3)").update.apply()
        SQL("insert into news_" + suffix + " values (6, 2)").update.apply()
        SQL("insert into news_" + suffix + " values (7, 1)").update.apply()
        SQL("insert into news_" + suffix + " values (8, 1)").update.apply()

        SQL("insert into members_" + suffix + " values (1, 2)").update.apply()
        SQL("insert into members_" + suffix + " values (2, 1)").update.apply()
        SQL("insert into members_" + suffix + " values (3, 1)").update.apply()
        SQL("insert into members_" + suffix + " values (4, 2)").update.apply()
        SQL("insert into members_" + suffix + " values (5, 1)").update.apply()

        SQL("insert into sponsors_" + suffix + " values (1, 1)").update.apply()
        SQL("insert into sponsors_" + suffix + " values (2, 2)").update.apply()
        SQL("insert into sponsors_" + suffix + " values (3, 3)").update.apply()
        SQL("insert into sponsors_" + suffix + " values (4, 2)").update.apply()
        SQL("insert into sponsors_" + suffix + " values (5, 3)").update.apply()
      }

      case class GroupEntity(id: Int, ownerId: Int)
      case class Group(
        id: Int,
        ownerId: Int,
        owner: Owner,
        events: collection.Seq[Event] = Nil,
        news: collection.Seq[News] = Nil,
        members: collection.Seq[Member] = Nil,
        sponsors: collection.Seq[Sponsor] = Nil
      )

      class Owner(val id: Int) extends EntityEquality {
        override val entityIdentity = id
      }
      class News(val id: Int, val groupId: Int) extends EntityEquality {
        override val entityIdentity = (id, groupId)
      }
      class Event(val id: Int, val groupId: Int) extends EntityEquality {
        override val entityIdentity = id
      }
      case class Member(id: Int, groupId: Int)
      case class Sponsor(id: Int, groupId: Int)

      {
        implicit val session = ReadOnlyAutoSession

        val groups: List[Group] = SQL(
          "select g.id as g_id, g.owner_id as g_owner_id, " +
            " o.id as o_id, e.id as e_id, n.id as n_id, m.id as m_id, s.id as s_id " +
            " from groups_" + suffix + " g " +
            " inner join owners_" + suffix + " o on g.owner_id = o.id " +
            " left join events_" + suffix + " e on g.id = e.group_id " +
            " left join news_" + suffix + " n on g.id = n.group_id " +
            " left join members_" + suffix + " m on g.id = m.group_id " +
            " left join sponsors_" + suffix + " s on g.id = s.group_id " +
            " order by g.id, n.id, e.id desc, m.id desc"
        )
          .one(rs => GroupEntity(rs.int("g_id"), rs.int("g_owner_id")))
          .toManies(
            rs => rs.intOpt("o_id").map(id => new Owner(id)),
            rs => rs.intOpt("e_id").map(id => new Event(id, rs.int("g_id"))),
            rs => rs.intOpt("n_id").map(id => new News(id, rs.int("g_id"))),
            rs => rs.intOpt("m_id").map(id => Member(id, rs.int("g_id"))),
            rs => rs.intOpt("s_id").map(id => Sponsor(id, rs.int("g_id")))
          )
          .map { (g, os, es, ns, ms, ss) =>
            Group(
              id = g.id,
              ownerId = g.ownerId,
              owner = os.head,
              events = es,
              news = ns,
              members = ms,
              sponsors = ss
            )
          }
          .list
          .apply()

        groups.size should equal(4)
        groups(0).id should equal(1)

        groups(0).owner.id should equal(2)

        groups(0).news.size should equal(3)
        groups(0).news(0).id should equal(2)
        groups(0).news(1).id should equal(7)
        groups(0).news(2).id should equal(8)

        groups(0).events.size should equal(2)
        groups(0).events(0).id should equal(3)
        groups(0).events(1).id should equal(2)

        groups(0).members.size should equal(3)
        groups(0).members(0).id should equal(5)
        groups(0).members(1).id should equal(3)
        groups(0).members(2).id should equal(2)

        groups(0).sponsors.size should equal(1)
        groups(0).sponsors(0).id should equal(1)
      }

      {
        implicit val session = ReadOnlyAutoSession

        val groups: Vector[Group] = SQL(
          "select g.id as g_id, g.owner_id as g_owner_id, " +
            " o.id as o_id, e.id as e_id, n.id as n_id, m.id as m_id, s.id as s_id " +
            " from groups_" + suffix + " g " +
            " inner join owners_" + suffix + " o on g.owner_id = o.id " +
            " left join events_" + suffix + " e on g.id = e.group_id " +
            " left join news_" + suffix + " n on g.id = n.group_id " +
            " left join members_" + suffix + " m on g.id = m.group_id " +
            " left join sponsors_" + suffix + " s on g.id = s.group_id " +
            " order by g.id, n.id, e.id desc, m.id desc"
        )
          .one(rs => GroupEntity(rs.int("g_id"), rs.int("g_owner_id")))
          .toManies(
            rs => rs.intOpt("o_id").map(id => new Owner(id)),
            rs => rs.intOpt("e_id").map(id => new Event(id, rs.int("g_id"))),
            rs => rs.intOpt("n_id").map(id => new News(id, rs.int("g_id"))),
            rs => rs.intOpt("m_id").map(id => Member(id, rs.int("g_id"))),
            rs => rs.intOpt("s_id").map(id => Sponsor(id, rs.int("g_id")))
          )
          .map { (g, os, es, ns, ms, ss) =>
            Group(
              id = g.id,
              ownerId = g.ownerId,
              owner = os.head,
              events = es,
              news = ns,
              members = ms,
              sponsors = ss
            )
          }
          .collection[Vector]()

        groups.size should equal(4)
        groups(0).id should equal(1)

        groups(0).owner.id should equal(2)

        groups(0).news.size should equal(3)
        groups(0).news(0).id should equal(2)
        groups(0).news(1).id should equal(7)
        groups(0).news(2).id should equal(8)

        groups(0).events.size should equal(2)
        groups(0).events(0).id should equal(3)
        groups(0).events(1).id should equal(2)

        groups(0).members.size should equal(3)
        groups(0).members(0).id should equal(5)
        groups(0).members(1).id should equal(3)
        groups(0).members(2).id should equal(2)

        groups(0).sponsors.size should equal(1)
        groups(0).sponsors(0).id should equal(1)
      }

      {
        implicit val session = ReadOnlyAutoSession

        val group: Group = SQL(
          "select g.id as g_id, g.owner_id as g_owner_id, " +
            " o.id as o_id, e.id as e_id, n.id as n_id, m.id as m_id, s.id as s_id " +
            " from groups_" + suffix + " g " +
            " inner join owners_" + suffix + " o on g.owner_id = o.id " +
            " left join events_" + suffix + " e on g.id = e.group_id " +
            " left join news_" + suffix + " n on g.id = n.group_id " +
            " left join members_" + suffix + " m on g.id = m.group_id " +
            " left join sponsors_" + suffix + " s on g.id = s.group_id " +
            " where g.id = 1 order by g.id, n.id, e.id desc, m.id desc"
        )
          .one(rs => GroupEntity(rs.int("g_id"), rs.int("g_owner_id")))
          .toManies(
            rs => rs.intOpt("o_id").map(id => new Owner(id)),
            rs => rs.intOpt("e_id").map(id => new Event(id, rs.int("g_id"))),
            rs => rs.intOpt("n_id").map(id => new News(id, rs.int("g_id"))),
            rs => rs.intOpt("m_id").map(id => Member(id, rs.int("g_id"))),
            rs => rs.intOpt("s_id").map(id => Sponsor(id, rs.int("g_id")))
          )
          .map { (g, os, es, ns, ms, ss) =>
            Group(
              id = g.id,
              ownerId = g.ownerId,
              owner = os.head,
              events = es,
              news = ns,
              members = ms,
              sponsors = ss
            )
          }
          .single
          .apply()
          .get

        group.id should equal(1)

        group.members.size should equal(3)
        group.members(0).id should equal(5)
        group.members(1).id should equal(3)
        group.members(2).id should equal(2)

        group.sponsors.size should equal(1)
        group.sponsors(0).id should equal(1)
      }

    } finally {
      DB autoCommit { implicit s =>
        SQL("drop table groups_" + suffix).execute.apply()
        SQL("drop table owners_" + suffix).execute.apply()
        SQL("drop table events_" + suffix).execute.apply()
        SQL("drop table members_" + suffix).execute.apply()
        SQL("drop table sponsors_" + suffix).execute.apply()
      }
    }
  }
}
