package models

import scalikejdbc._

case class NewProject(
  folder: String, 
  name: String
)

case class Project(
  id: Long, 
  folder: String, 
  name: String
)

object Project {
  
  // -- Queries
  private val simple = (rs: WrappedResultSet) => Project(
    rs.long("id"), 
    rs.string("folder"), 
    rs.string("name")
  )
    
  /**
   * Retrieve a Project from id.
   */
  def findById(id: Long)(implicit session: DBSession = AutoSession): Option[Project] = {
    SQL("select * from project where id = {id}")
      .bindByName('id -> id).map(simple).single.apply()
  }
  
  /**
   * Retrieve project for user
   */
  def findInvolving(user: String)(implicit session: DBSession = AutoSession): Seq[Project] = {
    SQL(
      """
        select * from project 
        join project_member on project.id = project_member.project_id 
        where project_member.user_email = {user}
      """
    ).bindByName('user -> user).map(simple).list.apply().toSeq
  }
  
  /**
   * Update a project.
   */
  def rename(id: Long, newName: String)(implicit session: DBSession = AutoSession) {
    SQL("update project set name = {name} where id = {id}")
      .bindByName('id -> id, 'name -> newName).update.apply()
  }
  
  /**
   * Delete a project.
   */
  def delete(id: Long)(implicit session: DBSession = AutoSession) {
    SQL("delete from project where id = {id}").bindByName('id -> id).update.apply()
  }
  
  /**
   * Delete all project in a folder
   */
  def deleteInFolder(folder: String)(implicit session: DBSession = AutoSession) {
    SQL("delete from project where folder = {folder}").bindByName('folder -> folder).update.apply()
  }
  
  /**
   * Rename a folder
   */
  def renameFolder(folder: String, newName: String)(implicit session: DBSession = AutoSession) {
    SQL("update project set folder = {newName} where folder = {folder}")
      .bindByName('folder -> folder, 'newName -> newName).update.apply()
  }
  
  /**
   * Retrieve project member
   */
  def membersOf(project: Long)(implicit session: DBSession = AutoSession): Seq[User] = {
    SQL(
      """
        select user.* from user 
        join project_member on project_member.user_email = user.email 
        where project_member.project_id = {project}
      """
    ).bindByName('project -> project).map(User.simple).list.apply().toSeq
  }
  
  /**
   * Add a member to the project team.
   */
  def addMember(project: Long, user: String)(implicit session: DBSession = AutoSession) {
    SQL("insert into project_member values({project}, {user})")
      .bindByName('project -> project, 'user -> user).map(simple).update.apply()
  }
  
  /**
   * Remove a member from the project team.
   */
  def removeMember(project: Long, user: String)(implicit session: DBSession = AutoSession) {
    SQL("delete from project_member where project_id = {project} and user_email = {user}")
      .bindByName('project -> project, 'user -> user).update.apply()
  }
  
  /**
   * Check if a user is a member of this project
   */
  def isMember(project: Long, user: String)(implicit session: DBSession = AutoSession): Boolean = {
    SQL(
      """
        select count(user.email) = 1 as is_member from user 
        join project_member on project_member.user_email = user.email 
        where project_member.project_id = {project} and user.email = {user}
      """
    ).bindByName('project -> project, 'user -> user)
     .map(rs => rs.boolean("is_member").asInstanceOf[Boolean]).single.apply().getOrElse(false)
  }
   
  /**
   * Create a Project.
   */
  def create(project: NewProject, members: Seq[String])(implicit session: DBSession = AutoSession): Project = {
     // Insert the project
     val newId: Long = SQL("select next value for project_seq as v from dual")
       .map(rs => rs.long("v")).single.apply().get
     SQL(
       """
         insert into project (id, name, folder) values (
           {id}, {name}, {folder} 
         )
       """
     ).bindByName('id -> newId, 'name -> project.name, 'folder -> project.folder).update.apply()
     // Add members
     members.foreach { email =>
       SQL("insert into project_member values ({id}, {email})")
         .bindByName('id -> newId, 'email -> email).update.apply()
     }
     Project(id = newId, name = project.name, folder = project.folder)
  }
  
}
