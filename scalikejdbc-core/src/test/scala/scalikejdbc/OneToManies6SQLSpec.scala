package scalikejdbc

import org.scalatest._
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class OneToManies6SQLSpec
  extends AnyFlatSpec
  with Matchers
  with BeforeAndAfter
  with Settings {

  val tableNamePrefix = "emp_OneToManies6SQLSpec" + System.currentTimeMillis()

  behavior of "OneToManies6SQL"

  it should "execute one-to-manies queries" in {
    val suffix = "_onetoone_" + System.currentTimeMillis()
    try {
      DB autoCommit { implicit s =>
        SQL(
          s"create table groups_${suffix} (id int not null, owner_id int not null)"
        ).execute.apply()
        SQL(s"create table owners_${suffix} (id int not null)").execute.apply()
        SQL(
          s"create table events_${suffix} (id int not null, group_id int not null)"
        ).execute.apply()
        SQL(
          s"create table news_${suffix} (id int not null, group_id int not null)"
        ).execute.apply()
        SQL(
          s"create table members_${suffix} (id int not null, group_id int not null)"
        ).execute.apply()
        SQL(
          s"create table sponsors_${suffix} (id int not null, group_id int not null)"
        ).execute.apply()
        SQL(
          s"create table entity6_${suffix} (id int not null, group_id int not null)"
        ).execute.apply()

        SQL(s"insert into groups_${suffix} values (1, 2)").update.apply()
        SQL(s"insert into groups_${suffix} values (2, 2)").update.apply()
        SQL(s"insert into groups_${suffix} values (3, 1)").update.apply()
        SQL(s"insert into groups_${suffix} values (4, 2)").update.apply()

        SQL(s"insert into owners_${suffix} values (1)").update.apply()
        SQL(s"insert into owners_${suffix} values (2)").update.apply()

        SQL(s"insert into events_${suffix} values (1, 2)").update.apply()
        SQL(s"insert into events_${suffix} values (2, 1)").update.apply()
        SQL(s"insert into events_${suffix} values (3, 1)").update.apply()

        SQL(s"insert into news_${suffix} values (1, 2)").update.apply()
        SQL(s"insert into news_${suffix} values (2, 1)").update.apply()
        SQL(s"insert into news_${suffix} values (3, 2)").update.apply()
        SQL(s"insert into news_${suffix} values (4, 2)").update.apply()
        SQL(s"insert into news_${suffix} values (5, 3)").update.apply()
        SQL(s"insert into news_${suffix} values (6, 2)").update.apply()
        SQL(s"insert into news_${suffix} values (7, 1)").update.apply()
        SQL(s"insert into news_${suffix} values (8, 1)").update.apply()

        SQL(s"insert into members_${suffix} values (1, 2)").update.apply()
        SQL(s"insert into members_${suffix} values (2, 1)").update.apply()
        SQL(s"insert into members_${suffix} values (3, 1)").update.apply()
        SQL(s"insert into members_${suffix} values (4, 2)").update.apply()
        SQL(s"insert into members_${suffix} values (5, 1)").update.apply()

        SQL(s"insert into sponsors_${suffix} values (1, 1)").update.apply()
        SQL(s"insert into sponsors_${suffix} values (2, 2)").update.apply()
        SQL(s"insert into sponsors_${suffix} values (3, 3)").update.apply()
        SQL(s"insert into sponsors_${suffix} values (4, 2)").update.apply()
        SQL(s"insert into sponsors_${suffix} values (5, 3)").update.apply()

        SQL(s"insert into entity6_${suffix} values (1, 1)").update.apply()
        SQL(s"insert into entity6_${suffix} values (2, 2)").update.apply()
        SQL(s"insert into entity6_${suffix} values (3, 1)").update.apply()

        case class GroupEntity(id: Int, ownerId: Int)
        case class Group(
          id: Int,
          ownerId: Int,
          owner: Owner,
          events: collection.Seq[Event] = Nil,
          news: collection.Seq[News] = Nil,
          members: collection.Seq[Member] = Nil,
          sponsors: collection.Seq[Sponsor] = Nil,
          entity6: collection.Seq[Entity6] = Nil
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
        case class Entity6(id: Int, groupId: Int)

        {
          val groups: List[Group] = SQL(s"""
              select g.id as g_id, g.owner_id as g_owner_id,
              o.id as o_id, e.id as e_id, n.id as n_id, m.id as m_id, s.id as s_id,
              e6.id as e6_id
              from groups_${suffix} g
              inner join owners_${suffix} o on g.owner_id = o.id
              left join events_${suffix} e on g.id = e.group_id
              left join news_${suffix} n on g.id = n.group_id
              left join members_${suffix} m on g.id = m.group_id
              left join sponsors_${suffix} s on g.id = s.group_id
              left join entity6_${suffix} e6 on g.id = e6.group_id
              order by g.id, n.id, e.id desc, m.id desc""")
            .one(rs => GroupEntity(rs.int("g_id"), rs.int("g_owner_id")))
            .toManies(
              rs => rs.intOpt("o_id").map(id => new Owner(id)),
              rs => rs.intOpt("e_id").map(id => new Event(id, rs.int("g_id"))),
              rs => rs.intOpt("n_id").map(id => new News(id, rs.int("g_id"))),
              rs => rs.intOpt("m_id").map(id => Member(id, rs.int("g_id"))),
              rs => rs.intOpt("s_id").map(id => Sponsor(id, rs.int("g_id"))),
              rs => rs.intOpt("e6_id").map(id => Entity6(id, rs.int("g_id")))
            )
            .map { (g, os, es, ns, ms, ss, e6) =>
              Group(
                id = g.id,
                ownerId = g.ownerId,
                owner = os.head,
                events = es,
                news = ns,
                members = ms,
                sponsors = ss,
                entity6 = e6
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

          groups(0).entity6.size should equal(2)
        }

        {
          val groups: Vector[Group] = SQL(s"""
              select g.id as g_id, g.owner_id as g_owner_id,
              o.id as o_id, e.id as e_id, n.id as n_id, m.id as m_id, s.id as s_id,
              e6.id as e6_id
              from groups_${suffix} g
              inner join owners_${suffix} o on g.owner_id = o.id
              left join events_${suffix} e on g.id = e.group_id
              left join news_${suffix} n on g.id = n.group_id
              left join members_${suffix} m on g.id = m.group_id
              left join sponsors_${suffix} s on g.id = s.group_id
              left join entity6_${suffix} e6 on g.id = e6.group_id
              order by g.id, n.id, e.id desc, m.id desc""")
            .one(rs => GroupEntity(rs.int("g_id"), rs.int("g_owner_id")))
            .toManies(
              rs => rs.intOpt("o_id").map(id => new Owner(id)),
              rs => rs.intOpt("e_id").map(id => new Event(id, rs.int("g_id"))),
              rs => rs.intOpt("n_id").map(id => new News(id, rs.int("g_id"))),
              rs => rs.intOpt("m_id").map(id => Member(id, rs.int("g_id"))),
              rs => rs.intOpt("s_id").map(id => Sponsor(id, rs.int("g_id"))),
              rs => rs.intOpt("e6_id").map(id => Entity6(id, rs.int("g_id")))
            )
            .map { (g, os, es, ns, ms, ss, e6) =>
              Group(
                id = g.id,
                ownerId = g.ownerId,
                owner = os.head,
                events = es,
                news = ns,
                members = ms,
                sponsors = ss,
                entity6 = e6
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

          groups(0).entity6.size should equal(2)
        }

        {
          val group: Group = SQL(s"""
              select g.id as g_id, g.owner_id as g_owner_id,
              o.id as o_id, e.id as e_id, n.id as n_id, m.id as m_id, s.id as s_id,
              e6.id as e6_id
              from groups_${suffix} g
              inner join owners_${suffix} o on g.owner_id = o.id
              left join events_${suffix} e on g.id = e.group_id
              left join news_${suffix} n on g.id = n.group_id
              left join members_${suffix} m on g.id = m.group_id
              left join sponsors_${suffix} s on g.id = s.group_id
              left join entity6_${suffix} e6 on g.id = e6.group_id
              where g.id = 1
              order by g.id, n.id, e.id desc, m.id desc""")
            .one(rs => GroupEntity(rs.int("g_id"), rs.int("g_owner_id")))
            .toManies(
              rs => rs.intOpt("o_id").map(id => new Owner(id)),
              rs => rs.intOpt("e_id").map(id => new Event(id, rs.int("g_id"))),
              rs => rs.intOpt("n_id").map(id => new News(id, rs.int("g_id"))),
              rs => rs.intOpt("m_id").map(id => Member(id, rs.int("g_id"))),
              rs => rs.intOpt("s_id").map(id => Sponsor(id, rs.int("g_id"))),
              rs => rs.intOpt("e6_id").map(id => Entity6(id, rs.int("g_id")))
            )
            .map { (g, os, es, ns, ms, ss, e6) =>
              Group(
                id = g.id,
                ownerId = g.ownerId,
                owner = os.head,
                events = es,
                news = ns,
                members = ms,
                sponsors = ss,
                entity6 = e6
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

          group.entity6.size should equal(2)
        }
      }
    } finally {
      DB autoCommit { implicit s =>
        SQL(s"drop table groups_${suffix}").execute.apply()
        SQL(s"drop table owners_${suffix}").execute.apply()
        SQL(s"drop table events_${suffix}").execute.apply()
        SQL(s"drop table members_${suffix}").execute.apply()
        SQL(s"drop table sponsors_${suffix}").execute.apply()
        SQL(s"drop table entity6_${suffix}").execute.apply()
      }
    }
  }
}
