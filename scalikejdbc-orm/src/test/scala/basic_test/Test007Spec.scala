package basic_test

import org.joda.time.DateTime
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{ Tag => _ }
import scalikejdbc._
import scalikejdbc.orm.CRUDMapper
import util.DBSeeds

class Test007Spec extends AnyFunSpec with Matchers with DBSeeds {

  Class.forName("org.h2.Driver")
  ConnectionPool.add(
    "test007",
    "jdbc:h2:mem:test007;MODE=PostgreSQL",
    "sa",
    "sa"
  )

  override val dbSeedsAutoSession = NamedAutoSession("test007")

  addSeedSQL(
    sql"create table blog (id bigserial not null, name varchar(100) not null)"
  )
  addSeedSQL(sql"""
   create table article (
     id bigserial not null,
     blog_id bigint not null references blog(id),
     title varchar(1000) not null,
     body text not null,
     created_at timestamp not null default current_timestamp
   )""")
  runIfFailed(sql"select count(1) from blog")

  // entities
  case class Blog(id: Long, name: String, articles: Seq[Article] = Seq.empty)
  object Blog extends CRUDMapper[Blog] {
    override val connectionPoolName = "test007"
    override def defaultAlias = createAlias("b")
    override def extract(rs: WrappedResultSet, rn: ResultName[Blog]) =
      autoConstruct(rs, rn, "articles")

    hasMany[Article](
      many = Article -> Article.defaultAlias,
      on = (b, a) => sqls.eq(b.id, a.blogId),
      merge = (blog, articles) => blog.copy(articles = articles)
    ).byDefault
  }
  case class Article(
    id: Long,
    blogId: Long,
    title: String,
    body: String,
    createdAt: DateTime,
    blog: Option[Blog] = None
  )
  object Article extends CRUDMapper[Article] {
    override val connectionPoolName = "test007"
    override def defaultAlias = createAlias("a")
    override def extract(rs: WrappedResultSet, rn: ResultName[Article]) =
      autoConstruct(rs, rn, "blog")

    lazy val blogRef = belongsTo[Blog](Blog, (a, b) => a.copy(blog = b))
  }

  def fixture(implicit session: DBSession): Unit = {}

  describe("The test") {
    it("should work as expected") {
      NamedDB("test007").localTx { implicit session =>
        fixture(session)

        val blogId1 = Blog.createWithAttributes("name" -> "Apply in Tokyo")
        val blogId2 = Blog.createWithAttributes("name" -> "Apply in NY")
        val blogId3 = Blog.createWithAttributes("name" -> "Apply in Paris")
        (1 to 20).foreach { day =>
          Article.createWithAttributes(
            "title" -> s"Learning Scala: Day $day",
            "body" -> "xxx",
            "blogId" -> blogId1
          )
          Article.createWithAttributes(
            "title" -> s"Learning Scala: Day $day",
            "body" -> "xxx",
            "blogId" -> blogId2
          )
          Article.createWithAttributes(
            "title" -> s"Learning Scala: Day $day",
            "body" -> "xxx",
            "blogId" -> blogId3
          )
        }
        val blogs = Blog.findAllWithLimitOffset(2, 0)
        blogs.size should equal(2)
      }
    }
  }
}
