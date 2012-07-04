package models

import scalikejdbc._

case class User(email: String, name: String, password: String)

object User {
  
  // -- Parsers
  
  /**
   * Parse a User from a ResultSet
   */
  val simple = (rs: WrappedResultSet) => User(
    rs.string("user.email"), 
    rs.string("user.name"), 
    rs.string("user.password")
  )
  
  // -- Queries
  
  /**
   * Retrieve a User from email.
   */
  def findByEmail(email: String)(implicit session: DBSession = AutoSession): Option[User] = {
    SQL("select * from user where email = {email}").bindByName('email -> email).map(simple).single.apply()
  }
  
  /**
   * Retrieve all users.
   */
  def findAll()(implicit session: DBSession = AutoSession): Seq[User] = {
    SQL("select * from user").map(simple).list.apply().toSeq
  }
  
  /**
   * Authenticate a User.
   */
  def authenticate(email: String, password: String)(implicit session: DBSession = AutoSession): Option[User] = {
    SQL(
      """
       select * from user where 
       email = {email} and password = {password}
      """
    ).bindByName('email -> email, 'password -> password).map(simple).single.apply()
  }
   
  /**
   * Create a User.
   */
  def create(user: User)(implicit session: DBSession = AutoSession): User = {
    SQL(
      """
        insert into user values (
          {email}, {name}, {password} 
        )
      """
    ).bindByName(
      'email -> user.email, 
      'name -> user.name, 
      'password -> user.password
    ).update.apply()
    user
  }
  
}
