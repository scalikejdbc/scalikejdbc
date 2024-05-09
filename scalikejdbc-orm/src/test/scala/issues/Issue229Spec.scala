package issues

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import scalikejdbc._
import scalikejdbc.orm.CRUDMapper
import util.DBSeeds

class Issue229Spec extends AnyFunSpec with Matchers with DBSeeds {

  Class.forName("org.h2.Driver")
  ConnectionPool.add(
    "issue229",
    "jdbc:h2:mem:issue229;MODE=PostgreSQL",
    "sa",
    "sa"
  )

  override val dbSeedsAutoSession: DBSession = NamedAutoSession("issue229")

  addSeedSQL(
    sql"""
create table user (
  id bigserial not null,
  name varchar(100) not null)
"""
  )
  addSeedSQL(
    sql"""
create table article (
  id bigserial not null,
  title varchar(100) not null,
  user_id bigint references user(id))
"""
  )
  runIfFailed(sql"select count(1) from article")

  case class User(id: Long, name: String)
  case class Article(
    id: Long,
    title: String,
    userId: Option[Long],
    user: Option[User] = None
  )

  object User extends CRUDMapper[User] {
    override val connectionPoolName: Any = "issue229"
    override def defaultAlias = createAlias("u")

    override def extract(rs: WrappedResultSet, rn: ResultName[User]) =
      autoConstruct(rs, rn)
  }
  object Article extends CRUDMapper[Article] {
    override val connectionPoolName: Any = "issue229"
    override def defaultAlias = createAlias("a")
    override def extract(rs: WrappedResultSet, rn: ResultName[Article]) =
      autoConstruct(rs, rn, "user")

    lazy val userRef = {
      belongsTo[User](
        right = User,
        merge = (a, u) => a.copy(user = u)
      ).includes[User]((as, us) =>
        as.map { a =>
          us.find(u => a.user.exists(_.id == u.id))
            .map(u => a.copy(user = Some(u)))
            .getOrElse(a)
        }
      )
    }
  }

  import Article._

  def db = NamedDB("issue229").toDB()

  def fixture(implicit session: DBSession): Unit = {
    val aliceId = User.createWithAttributes("name" -> "Alice")
    val bobId = User.createWithAttributes("name" -> "Bob")
    Seq(
      ("Hello World", Some(aliceId)),
      ("Getting Started with Scala", Some(bobId)),
      ("Functional Programming", None),
      ("How to user sbt", Some(aliceId))
    ).foreach { case (title, userId) =>
      Article.createWithAttributes("title" -> title, "userId" -> userId)
    }
  }

  def id(implicit session: DBSession): Long = {
    Article.where("title" -> "Functional Programming").apply().head.id
  }

  describe("find empty with empty ids") {
    it("should return no results") {
      db.localTx { implicit s =>
        fixture(s)
        Article.where("id" -> Nil).apply().size should equal(0)
      }
    }
  }
}
