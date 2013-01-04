package com.example

import scalikejdbc._

trait DBSettings {

  Class.forName("org.h2.Driver")

  val url = "jdbc:h2:mem:generating-specs"
  val username = "sa"
  val password = ""
  ConnectionPool.singleton(url, username, password)

  try {
    DB autoCommit { implicit s =>
      SQL("""
      create table member (
        id int generated always as identity,
        name varchar(30) not null,
        member_group_id int,
        description varchar(1000),
        birthday date,
        created_at timestamp not null,
        primary key(id)
      )
          """).execute.apply()
    }
  } catch { case e: Exception => }

}
