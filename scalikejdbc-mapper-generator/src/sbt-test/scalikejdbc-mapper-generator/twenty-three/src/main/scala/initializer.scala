import scalikejdbc._

object initializer extends App {

  Class.forName("org.h2.Driver")
  ConnectionPool.singleton("jdbc:h2:./db;MODE=PostgreSQL;AUTO_SERVER=TRUE", "u", "p")

  DB autoCommit { implicit s =>
    SQL("create table twenty_three(field1 bigint generated always as identity," +
        (2 to 23).map("field" + _ + " bigint").mkString(",") + ")" ).execute.apply()
  }

}
