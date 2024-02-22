package foo

import scalikejdbc.interpolation.Implicits.scalikejdbcSQLSyntaxToStringImplicitDef

case class User(id: Long, name: String)

object User {
  val table = scalikejdbc.SQLSyntaxSupportFactory[User]()
}
