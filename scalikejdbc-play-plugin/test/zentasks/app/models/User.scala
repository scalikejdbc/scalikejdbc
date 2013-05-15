package models

import scalikejdbc._
import scalikejdbc.SQLInterpolation._

case class User(email: String, name: String, password: String) 

object User extends SQLSyntaxSupport[User] {

  override val tableName = "users"
  override val columns = Seq("email", "name", "password")

  def apply(syntax: SyntaxProvider[User])(rs: WrappedResultSet) = {
    val u = syntax.resultName
    new User(email = rs.string(u.email), name = rs.string(u.name), password = rs.string(u.password))
  }

  private val u = User.syntax("u") 

  private val auto = AutoSession
 
  def findByEmail(email: String)(implicit session: DBSession = auto): Option[User] = withSQL {
    select.all(u).from(User as u).where.eq(u.email, email)
  }.map(User(u)).single.apply()
  
  def findAll()(implicit session: DBSession = auto): Seq[User] = withSQL {
    select.all(u).from(User as u)
  }.map(User(u)).list.apply()
  
  def authenticate(email: String, password: String)(implicit session: DBSession = auto): Option[User] = withSQL {
    select.all(u).from(User as u).where.eq(u.email, email).and.eq(u.password, password)
  }.map(User(u)).single.apply()
   
  def create(user: User)(implicit session: DBSession = auto): User = {
    applyUpdate { insert.into(User).values(user.email, user.name, user.password) }
    user
  }
  
}
