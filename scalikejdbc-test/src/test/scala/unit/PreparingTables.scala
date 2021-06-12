package unit

import scalikejdbc._

trait PreparingTables {

  try {
    DB autoCommit { implicit s =>
      SQL(
        "create table members (id integer primary key, name varchar(30), created_at timestamp not null)"
      ).execute.apply()
    }
  } catch { case e: Exception => }

  try {
    DB autoCommit { implicit s =>
      SQL(
        "create table mutable_members (id integer primary key, name varchar(30), created_at timestamp not null)"
      ).execute.apply()
    }
  } catch { case e: Exception => }

  try {
    DB autoCommit { implicit s =>
      SQL(
        "create table scalatest_members (id integer primary key, name varchar(30), created_at timestamp not null)"
      ).execute.apply()
    }
  } catch { case e: Exception => }

  try {
    NamedDB("db2") autoCommit { implicit s =>
      SQL(
        "create table members2 (id integer primary key, name varchar(30), created_at timestamp not null)"
      ).execute.apply()
    }
  } catch { case e: Exception => }

  try {
    NamedDB("db2") autoCommit { implicit s =>
      SQL(
        "create table mutable_members2 (id integer primary key, name varchar(30), created_at timestamp not null)"
      ).execute.apply()
    }
  } catch { case e: Exception => }

  try {
    NamedDB("db2") autoCommit { implicit s =>
      SQL(
        "create table scalatest_members2 (id integer primary key, name varchar(30), created_at timestamp not null)"
      ).execute.apply()
    }
  } catch { case e: Exception => }

}
