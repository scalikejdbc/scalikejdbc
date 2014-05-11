import scalikejdbc._

object initializer extends App {

  Class.forName("org.h2.Driver")
  ConnectionPool.singleton("jdbc:h2:./db;MODE=PostgreSQL;AUTO_SERVER=TRUE", "u", "p")

  DB autoCommit { implicit s =>
    sql"create table programmers (id bigint generated always as identity, name varchar(128))"
      .execute.apply()
  }

}
