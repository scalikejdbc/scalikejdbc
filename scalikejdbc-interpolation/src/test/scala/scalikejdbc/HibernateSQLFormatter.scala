package scalikejdbc

class HibernateSQLFormatter extends SQLFormatter {

  def format(sql: String) = new org.hibernate.engine.jdbc.internal.BasicFormatterImpl().format(sql)

}
