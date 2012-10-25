package scalikejdbc

case class StringSQLRunner(sql: String) {

  def run()(implicit session: DBSession = AutoSession): List[Map[String, Any]] = try {
    SQL(sql).map(_.toMap()).list.apply()
  } catch {
    case e: java.sql.SQLException =>
      val result = SQL(sql).execute.apply()
      List(Map("RESULT" -> result))
  }

}

object StringSQLRunner {

  implicit def stringToSQLRunner(sql: String) = StringSQLRunner(sql)

}
