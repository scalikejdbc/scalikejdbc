package scalikejdbc

import org.scalatest._

class DB_MetaDataSpec extends FlatSpec with Matchers with Settings {

  behavior of "DB's metadata operations"

  it should "retrieve metadata" in {

    try {

      DB autoCommit { implicit s =>
        try {
          SQL(
            """
      create table meta_groups (
        id int generated always as identity,
        name varchar(30) default 'NO NAME' not null,
        primary key(id)
      );
            """).execute.apply()
        } catch {
          case e: Exception =>
            SQL(
              """
      create table meta_groups (
        id integer primary key,
        name varchar(30) default 'NO NAME' not null
      );
            """).execute.apply()
        }

        try {
          SQL("""
      create table meta_members (
        id int generated always as identity,
        name varchar(30) default 'foooooooo baaaaaar' not null,
        group_id int,
        description varchar(1000),
        birthday date,
        created_at timestamp not null,
        primary key(id)
      );
            """).execute.apply()
        } catch {
          case e: Exception =>
            SQL("""
      create table meta_members (
        id integer primary key,
        name varchar(30) default 'foooooooo baaaaaar' not null,
        group_id integer,
        description varchar(1000),
        birthday date,
        created_at timestamp not null
      );
          """).execute.apply()
        }

        try {
          SQL("comment on table meta_members is 'website members';").execute.apply()
          SQL("comment on column meta_members.name is 'Full name';").execute.apply()
          SQL("comment on column meta_members.description is 'xxxxxxxxxxxxxxxxyyyyyyyyyyyyyyyzzzzzzzzzzz';").execute.apply()
        } catch {
          case e: Exception =>
            SQL("alter table meta_members comment 'website members';").execute.apply()
            SQL("alter table meta_members change name name varchar(30) not null comment 'Full name';").execute.apply()
            SQL("alter table meta_members change description description varchar(1000) comment 'xxxxxxxxxxxxxxxxyyyyyyyyyyyyyyyzzzzzzzzzzz';").execute.apply()
        }
        SQL("alter table meta_members add foreign key (group_id) references meta_groups(id);").execute.apply()
        SQL("create unique index meta_members_name_and_group on meta_members(name, group_id);").execute.apply()
        SQL("create index meta_members_birthday on meta_members(birthday);").execute.apply()
      }

      // find table names
      DB.getTableNames("*").size should be >= (2)
      DB.getTableNames("%").size should be >= (2)

      NamedDB('default).getTableNames("*").size should be >= (2)
      NamedDB('default).getTableNames("%").size should be >= (2)

      // showTables returns string value
      DB.showTables("%")
      NamedDB('default).showTables("%")

      // describe table
      DB.getTable("META_MEMBERS").isDefined should be(true)
      NamedDB('default).getTable("META_MEMBERS").isDefined should be(true)

      // describe returns string value
      DB.describe("meta_members")
      NamedDB('default).describe("meta_members")

      // get column names
      DB.getColumnNames("meta_members").size should equal(6)
      DB.getColumnNames("Meta_Members").size should equal(6)
      DB.getColumnNames("META_MEMBERS").size should equal(6)

    } finally {
      DB autoCommit { implicit s =>
        SQL("drop table meta_members").execute.apply()
        SQL("drop table meta_groups").execute.apply()
      }
    }
  }

}
