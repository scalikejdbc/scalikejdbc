package scalikejdbc.play

import java.io.File
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.Files
import org.specs2.mutable._
import org.specs2.specification.BeforeAfterExample

class FixtureSpec extends Specification with BeforeAfterExample {

  def before = {
  }

  def after = {
  }

  def fixture = {
    val script = """
      |
      |# --- !Ups
      |drop table users if exists;
      |create table users (
      |  email                     varchar(255) not null primary key,
      |  name                      varchar(255) not null,
      |  password                  varchar(255) not null
      |);
      |
      |# --- !Downs
      |drop table users if exists;
      |
      |""".stripMargin

    val tmpfile = File.createTempFile("tmp", ".sql")
    tmpfile.deleteOnExit()
    val writer = new java.io.PrintWriter(tmpfile)
    try {
      writer.println(script)
    } finally {
      writer.close()
    }
    Fixture(tmpfile)
  }

  "Fixture" should {

    "has #upScript" in {
      val expected =
        """|drop table users if exists;
           |create table users (
           |  email                     varchar(255) not null primary key,
           |  name                      varchar(255) not null,
           |  password                  varchar(255) not null
           |);
           |""".stripMargin
      fixture.upScript must_== expected
    }

    "has #downScript" in {
      val expected =
        """|drop table users if exists;
           |
           |""".stripMargin
      fixture.downScript must_== expected
    }

  }

}
