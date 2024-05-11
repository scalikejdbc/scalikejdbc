package basic_test

import org.joda.time.DateTime
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{ Tag => _ }
import scalikejdbc._
import scalikejdbc.orm.NoIdCRUDMapper
import util.DBSeeds

class Test008Spec extends AnyFunSpec with Matchers with DBSeeds {

  Class.forName("org.h2.Driver")
  ConnectionPool.add(
    "test008",
    "jdbc:h2:mem:test008;MODE=PostgreSQL",
    "sa",
    "sa"
  )

  override val dbSeedsAutoSession: DBSession = NamedAutoSession("test008")

  addSeedSQL(sql"create table blog (name varchar(100) not null)")
  addSeedSQL(sql"""
   create table article (
     blog_name varchar(100) not null references blog(name),
     title varchar(1000) not null,
     body text not null,
     created_at timestamp not null default current_timestamp
   )""")
  runIfFailed(sql"select count(1) from blog")

  // entities
  case class Blog(name: String)
  object Blog extends NoIdCRUDMapper[Blog] {
    override val connectionPoolName: Any = "test008"
    override def defaultAlias = createAlias("b")
    override def extract(rs: WrappedResultSet, rn: ResultName[Blog]) =
      autoConstruct(rs, rn)
  }

  case class Article(
    blogName: String,
    title: String,
    body: String,
    createdAt: DateTime,
    blog: Option[Blog] = None
  )
  object Article extends NoIdCRUDMapper[Article] {
    override val connectionPoolName: Any = "test008"
    override def defaultAlias = createAlias("a")
    override def extract(rs: WrappedResultSet, rn: ResultName[Article]) =
      autoConstruct(rs, rn, "blog")

    lazy val blogRef = belongsToWithAliasAndFkAndJoinCondition[Blog](
      right = Blog -> Blog.defaultAlias,
      fk = "blogName",
      on = sqls.eq(defaultAlias.blogName, Blog.defaultAlias.name),
      merge = (a, b) => a.copy(blog = b)
    )
  }

  def fixture(implicit session: DBSession): Unit = {}

  describe("The test") {
    it("should work as expected") {
      NamedDB("test008").localTx { implicit session =>
        fixture(session)

        Blog.createWithAttributes("name" -> "Apply in Tokyo")
        Blog.createWithAttributes("name" -> "Apply in NY")
        Blog.createWithAttributes("name" -> "Apply in Paris")
        (1 to 5).foreach { day =>
          Article.createWithAttributes(
            "title" -> s"Learning Scala: Day $day",
            "body" -> "日本へようこそ。東京は楽しいよ。",
            "blogName" -> "Apply in Tokyo"
          )
        }
        (1 to 6).foreach { day =>
          Article.createWithAttributes(
            "title" -> s"Learning Scala: Day $day",
            "body" -> "Welcome to New York!",
            "blogName" -> "Apply in NY"
          )
        }
        (1 to 7).foreach { day =>
          Article.createWithAttributes(
            "title" -> s"Learning Scala: Day $day",
            "body" -> "Bonjour et bienvenue à Paris!",
            "blogName" -> "Apply in Paris"
          )
        }
        Article
          .joins(Article.blogRef)
          .where("blogName" -> "Apply in Tokyo")
          .apply()
          .size should equal(5)
        Article
          .joins(Article.blogRef)
          .where("blogName" -> "Apply in NY")
          .apply()
          .size should equal(6)
        Article
          .joins(Article.blogRef)
          .where("blogName" -> "Apply in Paris")
          .apply()
          .size should equal(7)
      }
    }
  }
}
