package models

import scalikejdbc._
import scalikejdbc.SQLInterpolation._

case class User(email: String, name: String, password: String) 

object User extends SQLSyntaxSupport[User] {

  override val tableName = "users"
  override val columns = Seq("email", "name", "password")

  def apply(u: ResultName[User])(rs: WrappedResultSet) = new User(
    email = rs.string(u.email), name = rs.string(u.name), password = rs.string(u.password)
  )

  private val u = User.syntax("u") 

  private val auto = AutoSession
 
  def findByEmail(email: String)(implicit session: DBSession = auto): Option[User] = {
    sql"select ${u.result.*} from ${User as u} where ${u.email} = ${email}".map(User(u.resultName)).single.apply()
  }
  
  def findAll()(implicit session: DBSession = auto): Seq[User] = {
    sql"select ${u.result.*} from ${User as u}".map(User(u.resultName)).list.apply()
  }
  
  def authenticate(email: String, password: String)(implicit session: DBSession = auto): Option[User] = {
    sql"select ${u.result.*} from ${User as u} where ${u.email} = ${email} and ${u.password} = ${password}".map(User(u.resultName)).single.apply()
  }
   
  /**
   * Create a User.
   */
  def create(user: User)(implicit session: DBSession = auto): User = {
    sql"insert into ${User.table} values (${user.email}, ${user.name}, ${user.password})".update.apply()
    user
  }
  
}
