package foo

case class User(id: Long, name: String)

object User {
  val table = scalikejdbc.SQLSyntaxSupportFactory[User]()
}
