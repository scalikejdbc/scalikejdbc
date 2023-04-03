package scalikejdbc

class HibernateSQLFormatter extends SQLFormatter {
  private val formatter =
    new org.hibernate.engine.jdbc.internal.BasicFormatterImpl()
  def format(sql: String) = formatter.format(sql)
}
