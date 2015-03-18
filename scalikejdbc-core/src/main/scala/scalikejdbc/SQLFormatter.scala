package scalikejdbc

/**
 * SQL formatter
 */
trait SQLFormatter {

  def format(sql: String): String

}
