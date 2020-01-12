package scalikejdbc

import org.scalatest._
import org.scalatest.OptionValues._
import java.util.Locale.{ ENGLISH => en }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DB_MetaDataSpec extends AnyFlatSpec with Matchers with Settings with LogSupport {

  behavior of "DB's metadata operations"

  it should "retrieve metadata" in {

    try {
      DB autoCommit { implicit s =>
        execute("""
          create table meta_groups (
            id int generated always as identity,
            name varchar(30) default 'NO NAME' not null,
            primary key(id)
          );
          """, """
          create table meta_groups (
            id integer primary key,
            name varchar(30) default 'NO NAME' not null
          );
          """)

        execute("""
          create table meta_members (
            id int generated always as identity,
            name varchar(30) default 'foooooooo baaaaaar' not null,
            group_id int,
            description varchar(1000),
            birthday date,
            created_at timestamp not null,
            primary key(id)
          );
          """, """
          create table meta_members (
            id integer primary key,
            name varchar(30) default 'foooooooo baaaaaar' not null,
            group_id integer,
            description varchar(1000),
            birthday date,
            created_at timestamp not null
          );
          """)

        execute(
          "comment on table meta_members is 'website members';",
          "alter table meta_members comment 'website members';")
        execute(
          "comment on column meta_members.name is 'Full name';",
          "alter table meta_members change name name varchar(30) not null comment 'Full name';")
        execute(
          "comment on column meta_members.description is 'xxxxxxxxxxxxxxxxyyyyyyyyyyyyyyyzzzzzzzzzzz';",
          "alter table meta_members change description description varchar(1000) comment 'xxxxxxxxxxxxxxxxyyyyyyyyyyyyyyyzzzzzzzzzzz';")

        execute("alter table meta_members add foreign key (group_id) references meta_groups(id);")
        execute("create unique index meta_members_name_and_group on meta_members(name, group_id);")
        execute("create index meta_members_birthday on meta_members(birthday);")
      }

      // find table names
      for (
        (act, i) <- Seq(
          DB.getTableNames("*"),
          DB.getTableNames("%"),
          DB.getTableNames("meta_%"),
          DB.getTableNames("META_%"),
          NamedDB(Symbol("default")).getTableNames("*"),
          NamedDB(Symbol("default")).getTableNames("%"),
          NamedDB(Symbol("default")).getTableNames("meta_%"),
          NamedDB(Symbol("default")).getTableNames("META_%")).zipWithIndex
      ) withClue(s"No. ${i}") {
        lower(act) should contain allOf ("meta_groups", "meta_members")
      }

      for (
        act <- Seq(
          DB.getTableNames("%.%"),
          NamedDB(Symbol("default")).getTableNames("%.%"))
      ) {
        // mysql is not support schema
        if (driverClassName == "com.mysql.jdbc.Driver") {
          lower(act) should contain allOf ("meta_groups", "meta_members")
        } else {
          lower(act) should contain allOf ("public.meta_groups", "public.meta_members")
        }
      }

      for (
        (act, i) <- Seq(
          DB.getTableNames("%ta_me%"),
          DB.getTableNames("%TA_ME%"),
          NamedDB(Symbol("default")).getTableNames("%ta_me%"),
          NamedDB(Symbol("default")).getTableNames("%TA_ME%")).zipWithIndex
      ) withClue(s"No. ${i}") {
        lower(act) should (contain("meta_members") and not contain ("meta_groups"))
      }

      DB.showTables("dummy") should be(empty)
      NamedDB(Symbol("default")).showTables("dummy") should be(empty)

      // showTables returns string value
      lower(DB.showTables("%")) should (include("meta_groups") and include("meta_members"))
      lower(NamedDB(Symbol("default")).showTables("%")) should (include("meta_groups") and include("meta_members"))

      DB.showTables("dummy") should be("")
      NamedDB(Symbol("default")).showTables("dummy") should be("")

      // describe table
      for (
        act <- Seq(
          DB.getTable("META_MEMBERS"),
          DB.getTable("meta_members"),
          NamedDB(Symbol("default")).getTable("META_MEMBERS"),
          NamedDB(Symbol("default")).getTable("meta_members"))
      ) {
        if (driverClassName == "com.mysql.jdbc.Driver") {
          lower(act.value.schema) should equal(null)
        } else {
          lower(act.value.schema) should equal("public")
        }
        lower(act.value.name) should equal("meta_members")

        act.value.columns should have size (6)
        act.value.foreignKeys should have size (1)

        if (url.startsWith("jdbc:postgresql")) {
          act.value.indices should have size (3)
        } else {
          act.value.indices should have size (4) // contain foreign key
        }
      }

      DB.getTable("dummy").isDefined should be(false)
      NamedDB(Symbol("default")).getTable("dummy").isDefined should be(false)

      // describe returns string value
      lower(DB.describe("meta_members")) should include("meta_members")
      lower(NamedDB(Symbol("default")).describe("meta_members")) should include("meta_members")

      DB.describe("dummy") should be("Not found.")
      NamedDB(Symbol("default")).describe("dummy") should be("Not found.")

      // get column names
      val exp = List("id", "name", "group_id", "description", "birthday", "created_at")
      for (
        table <- List("meta_members", "Meta_Members", "META_MEMBERS")
      ) withClue(s"table [${table}]") {
        lower(DB.getColumnNames(table)) should equal(exp)
      }

      DB.getColumnNames("dummy") should be(empty)

    } finally {
      DB autoCommit { implicit s =>
        execute("drop table meta_members")
        execute("drop table meta_groups")
      }
    }
  }

  it should "retrieve metadata with schemas" in {
    // mysql is not support schema
    if (driverClassName != "com.mysql.jdbc.Driver") {

      try {

        DB autoCommit { implicit s =>
          execute("create schema other;")

          execute("""
            create table public.meta_members (
              id int generated always as identity,
              title varchar(30)  not null,
              special_day date,
              updated_at timestamp not null,
              primary key(id)
            );
            """, """
            create table public.meta_members (
              id integer primary key,
              title varchar(30)  not null,
              special_day date,
              updated_at timestamp not null
            );
            """)

          execute("""
            create table other.meta_members (
              id int generated always as identity,
              name varchar(30) default 'foooooooo baaaaaar' not null,
              group_id int,
              description varchar(1000),
              birthday date,
              created_at timestamp not null,
              primary key(id)
            );
            """, """
            create table other.meta_members (
              id integer primary key,
              name varchar(30) default 'foooooooo baaaaaar' not null,
              group_id integer,
              description varchar(1000),
              birthday date,
              created_at timestamp not null
            );
            """)

          execute("""
            create table other.meta_groups (
              id int generated always as identity,
              name varchar(30) not null,
              primary key(id)
            );
            """, """
            create table other.meta_groups (
              id integer primary key,
              name varchar(30) not null
            );
            """)
        }

        // find table names

        for (
          (act, i) <- Seq(
            DB.getTableNames("*"),
            DB.getTableNames("%"),
            DB.getTableNames("meta_%"),
            DB.getTableNames("META_%"),
            NamedDB(Symbol("default")).getTableNames("*"),
            NamedDB(Symbol("default")).getTableNames("%"),
            NamedDB(Symbol("default")).getTableNames("meta_%"),
            NamedDB(Symbol("default")).getTableNames("META_%")).zipWithIndex
        ) withClue(s"No. ${i}") {
          if (driverClassName == "org.h2.Driver") {
            // public.meta_members
            lower(act).count(_ == "meta_members") should be(1)
            lower(act) should not contain ("meta_groups")
          } else {
            // public.meta_members, other.meta_members, other.meta_groups
            lower(act).count(_ == "meta_members") should be(2)
            lower(act).count(_ == "meta_groups") should be(1)
          }
        }

        for (
          act <- Seq(
            DB.getTableNames("%.%"),
            NamedDB(Symbol("default")).getTableNames("%.%"))
        ) {
          // public.meta_members, other.meta_members, other.meta_groups
          lower(act).count(_ == "public.meta_members") should be(1)
          lower(act).count(_ == "other.meta_members") should be(1)
          lower(act).count(_ == "other.meta_groups") should be(1)
        }

        for (
          act <- Seq(
            DB.getTableNames("%mem%"),
            DB.getTableNames("%MEM%"),
            NamedDB(Symbol("default")).getTableNames("%mem%"),
            NamedDB(Symbol("default")).getTableNames("%MEM%"))
        ) {
          if (driverClassName == "org.h2.Driver") {
            // public.meta_members
            lower(act).count(_ == "meta_members") should be(1)
            lower(act) should not contain ("meta_groups")
          } else {
            // public.meta_members, other.meta_members
            lower(act).count(_ == "meta_members") should be(2)
            lower(act) should not contain ("meta_groups")
          }
        }

        for (
          (act, i) <- Seq(
            DB.getTableNames("public.*"),
            DB.getTableNames("public.%"),
            DB.getTableNames("public.meta_%"),
            DB.getTableNames("PUBLIC.META_%"),
            NamedDB(Symbol("default")).getTableNames("public.*"),
            NamedDB(Symbol("default")).getTableNames("public.%"),
            NamedDB(Symbol("default")).getTableNames("public.meta_%"),
            NamedDB(Symbol("default")).getTableNames("PUBLIC.META_%")).zipWithIndex
        ) withClue(s"No. ${i}") {
          lower(act) should (contain("public.meta_members") and not contain ("other.meta_groups"))
        }

        for (
          (act, i) <- Seq(
            DB.getTableNames("other.*"),
            DB.getTableNames("other.%"),
            DB.getTableNames("other.meta_%"),
            DB.getTableNames("OTHER.META_%"),
            NamedDB(Symbol("default")).getTableNames("other.*"),
            NamedDB(Symbol("default")).getTableNames("other.%"),
            NamedDB(Symbol("default")).getTableNames("other.meta_%"),
            NamedDB(Symbol("default")).getTableNames("OTHER.META_%")).zipWithIndex
        ) withClue(s"No. ${i}") {
          lower(act) should (contain allOf ("other.meta_members", "other.meta_groups"))
        }

        lower(DB.getTableNames("dummy.*")) should be(empty)
        lower(NamedDB(Symbol("default")).getTableNames("dummy.*")) should be(empty)

        // showTables returns string value
        for (
          act <- Seq(
            DB.showTables("%"),
            NamedDB(Symbol("default")).showTables("%"))
        ) {
          if (driverClassName == "org.h2.Driver") {
            // public.meta_members
            lower(act) should (include("meta_members") and not include ("meta_groups"))
          } else {
            // public.meta_members, other.meta_members, other.meta_groups
            lower(act) should (include("meta_members") and include("meta_groups"))
          }
        }

        for (
          act <- Seq(
            DB.showTables("public.%"),
            NamedDB(Symbol("default")).showTables("public.%"))
        ) {
          lower(act) should (include("public.meta_members") and not include ("other.meta_members") and not include ("other.meta_groups"))
        }

        for (
          act <- Seq(
            DB.showTables("other.%"),
            NamedDB(Symbol("default")).showTables("other.%"))
        ) {
          lower(act) should (not include ("public.meta_members") and include("other.meta_members") and include("other.meta_groups"))
        }

        // describe table
        for (
          (act, i) <- Seq(
            DB.getTable("public.meta_members"),
            DB.getTable("PUBLIC.META_MEMBERS"),
            NamedDB(Symbol("default")).getTable("public.meta_members"),
            NamedDB(Symbol("default")).getTable("PUBLIC.META_MEMBERS")).zipWithIndex
        ) withClue(s"No. ${i}") {
          lower(act.value.schema) should equal("public")
          lower(act.value.name) should equal("meta_members")
          act.value.columns should have size (4)
        }

        for (
          (act, i) <- Seq(
            DB.getTable("other.meta_members"),
            DB.getTable("OTHER.META_MEMBERS"),
            NamedDB(Symbol("default")).getTable("other.meta_members"),
            NamedDB(Symbol("default")).getTable("OTHER.META_MEMBERS")).zipWithIndex
        ) withClue(s"No. ${i}") {
          lower(act.value.schema) should equal("other")
          lower(act.value.name) should equal("meta_members")
          act.value.columns should have size (6)
        }

        DB.getTable("dummy.*") should be(empty)
        NamedDB(Symbol("default")).getTable("dummy.*") should be(empty)

        // describe returns string value
        for (
          act <- Seq(
            DB.describe("public.meta_members"),
            NamedDB(Symbol("default")).describe("public.meta_members"))
        ) {
          lower(act) should (include("public.meta_members") and not include ("other.meta_members"))
        }

        for (
          act <- Seq(
            DB.describe("other.meta_members"),
            NamedDB(Symbol("default")).describe("other.meta_members"))
        ) {
          lower(act) should (not include ("public.meta_members") and include("other.meta_members"))
        }

        // get column names
        for (
          (schemas, exp) <- Seq(
            (Seq("public", "Public", "PUBLIC"), List("id", "title", "special_day", "updated_at")),
            (Seq("other", "Other", "OTHER"), List("id", "name", "group_id", "description", "birthday", "created_at")));
          schema <- schemas;
          table <- Seq("meta_members", "Meta_Members", "META_MEMBERS")
        ) withClue(s"table [${schema}.${table}]") {
          lower(DB.getColumnNames(s"${schema}.${table}")) should equal(exp)
        }

        lower(DB.getColumnNames("dummy.meta_members")) should be(empty)
        lower(NamedDB(Symbol("default")).getColumnNames("dummy.meta_members")) should be(empty)

      } finally {
        DB autoCommit { implicit s =>
          execute("drop table if exists public.meta_members")
          execute("drop table if exists other.meta_members")
          execute("drop table if exists other.meta_groups")
          execute("drop schema if exists other")
        }
      }
    }
  }

  it should "retrieve metadata that name is same as information schema in h2" in {
    if (driverClassName == "org.h2.Driver") {

      try {

        DB autoCommit { implicit s =>
          // "user" is same as information schema
          execute(
            """
            create table users (
              id int generated always as identity,
              title varchar(30)  not null,
              primary key(id)
            );
            """, """
            create table users (
              id integer primary key,
              title varchar(30)  not null
            );
            """)
        }

        // find table names
        for (
          act <- Seq(
            DB.getTableNames("%"),
            NamedDB(Symbol("default")).getTableNames("%"))
        ) {
          lower(act) should contain("users")
        }

        for (
          act <- Seq(
            DB.getTableNames("public.%"),
            NamedDB(Symbol("default")).getTableNames("public.%"))
        ) {
          lower(act) should contain("public.users")
        }

        // describe table
        for (
          act <- Seq(
            DB.getTable("users"),
            NamedDB(Symbol("default")).getTable("users"))
        ) {
          lower(act.value.schema) should equal("public")
          lower(act.value.name) should equal("users")

          act.value.columns should have size (2)
          act.value.foreignKeys should have size (0)
          act.value.indices should have size (1)
        }

        for (
          act <- Seq(
            DB.getTable("public.users"),
            NamedDB(Symbol("default")).getTable("public.users"))
        ) {
          lower(act.value.schema) should equal("public")
          lower(act.value.name) should equal("users")

          act.value.columns should have size (2)
          act.value.foreignKeys should have size (0)
          act.value.indices should have size (1)
        }

        // get column names
        val exp = List("id", "title")
        for (
          table <- List("public.users", "users")
        ) withClue(s"table [${table}]") {
          lower(DB.getColumnNames(table)) should equal(exp)
        }

      } finally {
        DB autoCommit { implicit s =>
          execute("drop table if exists users")
        }
      }
    }
  }
  it should "get all columns" in {

    if (driverClassName == "com.mysql.jdbc.Driver") {
      try {
        // There was a bug that MySQL returns all columns of same name tables.
        DB autoCommit { implicit s =>
          execute("create table getcolumns(id1 integer,c1 integer);")
          execute("create database otherdb;")
          execute("create table otherdb.getcolumns(id2 integer,c2 integer);")
        }
        DB.getTable("getcolumns").get.columns.map(_.name.toLowerCase) should contain allOf ("id1", "c1")

      } finally {
        DB autoCommit { implicit s =>
          execute("drop database otherdb")
          execute("drop table if exists getcolumns")
        }
      }
    } else {

      try {

        DB autoCommit { implicit s =>
          execute("create table getcolumns(id1 integer, c1 integer);")
        }

        DB.getTable("getcolumns").get.columns.map(_.name.toLowerCase) should contain allOf ("id1", "c1")

      } finally {
        DB autoCommit { implicit s =>
          execute("drop table if exists getcolumns")
        }
      }
    }

  }

  private def execute(sqls: String*)(implicit session: DBSession): Unit = {
    for (sql <- sqls) {
      try {
        SQL(sql).execute.apply()
        return
      } catch {
        case e: Exception =>
          log.debug(e.toString)
      }
    }

    throw new RuntimeException("Failed to execute sqls :" + sqls)
  }

  private def lower(list: List[String]): List[String] = Option(list).map(_.map(_.toLowerCase(en))).orNull
  private def lower(str: String): String = Option(str).map(_.toLowerCase(en)).orNull

}
