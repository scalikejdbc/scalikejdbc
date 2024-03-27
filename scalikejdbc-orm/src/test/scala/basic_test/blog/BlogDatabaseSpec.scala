package basic_test.blog

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{ Tag => _ }
import scalikejdbc._
import scalikejdbc.orm.Pagination
import util.DBSeeds

class BlogDatabaseSpec extends AnyFunSpec with Matchers with DBSeeds {

  Class.forName("org.h2.Driver")
  ConnectionPool.add("blog", "jdbc:h2:mem:blog", "sa", "sa")

  override val dbSeedsAutoSession = NamedAutoSession("blog")

  addSeedSQL(
    sql"""
create table posts (
  id bigint auto_increment primary key not null,
  title varchar(128) not null,
  body varchar(1024) not null,
  view_count number(3) not null default 0,
  created_at timestamp not null,
  updated_at timestamp
)
""",
    sql"""
create table tags (
  id bigint auto_increment primary key not null,
  name varchar(128) not null,
  created_at timestamp not null,
  updated_at timestamp
)
""",
    sql"""
create table posts_tags (
  post_id bigint not null,
  tag_id bigint not null
)
"""
  )

  runIfFailed(sql"select count(1) from posts")

  describe("hasManyThrough without byDefault") {
    it("should work as expected") {
      NamedDB("blog").localTx { implicit s =>
        val postId =
          Post.createWithAttributes(
            "title" -> "Hello World!",
            "body" -> "This is the first entry..."
          )
        val scalaTagId = Tag.createWithAttributes("name" -> "Scala")
        val rubyTagId = Tag.createWithAttributes("name" -> "Ruby")
        val pt = PostTag.column
        insert
          .into(PostTag)
          .namedValues(pt.postId -> postId, pt.tagId -> scalaTagId)
          .toSQL
          .update
          .apply()
        insert
          .into(PostTag)
          .namedValues(pt.postId -> postId, pt.tagId -> rubyTagId)
          .toSQL
          .update
          .apply()

        {
          val id = Post.limit(1).apply().head.id
          val post = Post.joins(Post.tagsRef).findById(id)
          post.get.tags.size should equal(2)
        }

        {
          // it should work when joining twice
          val id = Post.limit(1).apply().head.id
          val post = Post.joins(Post.tagsRef, Post.tagsRef).findById(id)
          post.get.tags.size should equal(2)
        }

        {
          // should work with BigDecimal
          val post = Post.limit(1).apply().head
          Post.updateById(post.id).withAttributes("viewCount" -> 123)
          Post.findById(post.id).get.viewCount should equal(123)
        }
      }
    }
  }

  describe("pagination with one-to-many relationships") {
    it("should work as expected") {

      NamedDB("blog").localTx { implicit s =>
        val postId =
          Post.createWithAttributes(
            "title" -> "Hello World!",
            "body" -> "This is the first entry..."
          )
        val scalaTagId = Tag.createWithAttributes("name" -> "Scala")
        val rubyTagId = Tag.createWithAttributes("name" -> "Ruby")
        val pt = PostTag.column
        insert
          .into(PostTag)
          .namedValues(pt.postId -> postId, pt.tagId -> scalaTagId)
          .toSQL
          .update
          .apply()
        insert
          .into(PostTag)
          .namedValues(pt.postId -> postId, pt.tagId -> rubyTagId)
          .toSQL
          .update
          .apply()

        // clear fixture data
        sql"truncate table posts".execute.apply()
        sql"truncate table tags".execute.apply()
        sql"truncate table posts_tags".execute.apply()

        // prepare data
        val tagIds = (1 to 10).map { i =>
          Tag.createWithAttributes("name" -> s"tag$i")
        }
        (1 to 10).map { i =>
          val id = Post.createWithAttributes(
            "title" -> s"entry $i",
            "body" -> "foo bar baz"
          )
          tagIds.take(3).foreach { tagId =>
            withSQL {
              insert
                .into(PostTag)
                .namedValues(pt.postId -> id, pt.tagId -> tagId)
            }.update.apply()
          }
        }
        (11 to 20).map { i =>
          val id = Post.createWithAttributes(
            "title" -> s"entry $i",
            "body" -> "bulah bulah..."
          )
          tagIds.take(4).foreach { tagId =>
            withSQL {
              insert
                .into(PostTag)
                .namedValues(pt.postId -> id, pt.tagId -> tagId)
            }.update.apply()
          }
        }

        // #paginate in Querying
        {
          val posts =
            Post.joins(Post.tagsRef).paginate(Pagination.page(1).per(3)).apply()
          posts.size should equal(3)
          posts(0).tags.size should equal(3)
          posts(1).tags.size should equal(3)
          posts(2).tags.size should equal(3)
        }
        {
          val posts =
            Post.joins(Post.tagsRef).paginate(Pagination.page(7).per(3)).apply()
          posts.size should equal(2)
          posts(0).tags.size should equal(4)
          posts(1).tags.size should equal(4)
        }
        {
          val posts =
            Post.joins(Post.tagsRef).paginate(Pagination.page(8).per(3)).apply()
          posts.size should equal(0)
        }

        {
          val posts =
            Post
              .joins(Post.tagsRef)
              .where("body" -> "foo bar baz")
              .paginate(Pagination.page(1).per(3))
              .apply()
          posts.size should equal(3)
          posts(0).tags.size should equal(3)
          posts(1).tags.size should equal(3)
          posts(2).tags.size should equal(3)
        }
        {
          val posts =
            Post
              .joins(Post.tagsRef)
              .where("body" -> "foo bar baz")
              .paginate(Pagination.page(4).per(3))
              .apply()
          posts.size should equal(1)
          posts(0).tags.size should equal(3)
        }
        {
          val posts =
            Post
              .joins(Post.tagsRef)
              .where("body" -> "foo bar baz")
              .paginate(Pagination.page(5).per(3))
              .apply()
          posts.size should equal(0)
        }

        // #findAllWithPagination in Finder
        {
          val posts = Post
            .joins(Post.tagsRef)
            .findAllWithPagination(Pagination.page(1).per(3))
          posts.size should equal(3)
          posts(0).tags.size should equal(3)
          posts(1).tags.size should equal(3)
          posts(2).tags.size should equal(3)
        }
        {
          val posts = Post
            .joins(Post.tagsRef)
            .findAllWithPagination(Pagination.page(7).per(3))
          posts.size should equal(2)
          posts(0).tags.size should equal(4)
          posts(1).tags.size should equal(4)
        }
        {
          val posts = Post
            .joins(Post.tagsRef)
            .findAllWithPagination(Pagination.page(8).per(3))
          posts.size should equal(0)
        }

        val p = Post.defaultAlias

        // #findAllByWithPagination in Finder
        {
          val posts =
            Post
              .joins(Post.tagsRef)
              .findAllByWithPagination(
                sqls.eq(p.body, "foo bar baz"),
                Pagination.page(1).per(3)
              )
          posts.size should equal(3)
          posts(0).tags.size should equal(3)
          posts(1).tags.size should equal(3)
          posts(2).tags.size should equal(3)
        }
        {
          val posts =
            Post
              .joins(Post.tagsRef)
              .findAllByWithPagination(
                sqls.eq(p.body, "foo bar baz"),
                Pagination.page(4).per(3)
              )
          posts.size should equal(1)
          posts(0).tags.size should equal(3)
        }
        {
          val posts =
            Post
              .joins(Post.tagsRef)
              .findAllByWithPagination(
                sqls.eq(p.body, "foo bar baz"),
                Pagination.page(5).per(3)
              )
          posts.size should equal(0)
        }
      }
    }
  }
}
