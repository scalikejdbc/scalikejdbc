package models

import scalikejdbc._
import scalikejdbc.SQLInterpolation._

case class ProjectMember(projectId: Long, userEmail: String)

object ProjectMember extends SQLSyntaxSupport[ProjectMember] {
  def apply(syntax: SyntaxProvider[ProjectMember])(rs: WrappedResultSet) = {
    val p = syntax.resultName
    new ProjectMember(
      projectId = rs.long(p.projectId),
      userEmail = rs.string(p.userEmail)
    )
  }
}

case class NewProject(folder: String, name: String)

case class Project(id: Long, folder: String, name: String)

object Project extends SQLSyntaxSupport[Project] {

  def apply(syntax: SyntaxProvider[Project])(rs: WrappedResultSet) = {
    val p = syntax.resultName
    new Project(
      id = rs.long(p.id), 
      folder = rs.string(p.folder), 
      name = rs.string(p.name)
    )
  }

  private val p = Project.syntax("p")
  private val u = User.syntax("u")
  private val m = ProjectMember.syntax("m")

  private val auto = AutoSession
    
  def findById(id: Long)(implicit session: DBSession = auto): Option[Project] = withSQL { 
    select.from(Project as p).where.eq(p.id, id) 
  }.map(Project(p)).single.apply()
  
  def findInvolving(user: String)(implicit session: DBSession = auto): Seq[Project] = withSQL {
    select
      .from(Project as p)
      .join(ProjectMember as m).on(p.id, m.projectId)
      .where.eq(m.userEmail, user)
  }.map(Project(p)).list.apply()

  def rename(id: Long, newName: String)(implicit session: DBSession = auto): Unit = applyUpdate {
    update(Project as p).set(p.name -> newName).where.eq(p.id, id)
  }

  def delete(id: Long)(implicit session: DBSession = auto): Unit = applyUpdate {
    deleteFrom(Project as p).where.eq(p.id, id)
  }
  
  def deleteInFolder(folder: String)(implicit session: DBSession = auto): Unit = applyUpdate {
    deleteFrom(Project as p).where.eq(p.folder, folder)
  }
  
  def renameFolder(folder: String, newName: String)(implicit session: DBSession = auto): Unit = applyUpdate {
    update(Project as p).set(p.folder -> newName).where.eq(p.folder, folder)
  }
  
  def membersOf(project: Long)(implicit session: DBSession = auto): Seq[User] = withSQL {
    select
      .from(User as u)
      .join(ProjectMember as m).on(m.userEmail, u.email)
      .where.eq(m.projectId, project)
  }.map(User(u)).list.apply()
  
  def addMember(project: Long, user: String)(implicit session: DBSession = auto): Unit = applyUpdate {
    insert.into(ProjectMember).values(project, user)
  }
  
  def removeMember(project: Long, user: String)(implicit session: DBSession = auto): Unit = applyUpdate {
    deleteFrom(ProjectMember as m).where.eq(m.projectId, project).and.eq(m.userEmail, user)
  }
  
  def isMember(project: Long, user: String)(implicit session: DBSession = auto): Boolean = withSQL {
    select(sqls"count(${u.email}) = 1 as is_member")
      .from(User as u)
      .join(ProjectMember as m).on(m.userEmail, u.email)
      .where.eq(m.projectId, project).and.eq(u.email, user)
  }.map(rs => rs.boolean("is_member").asInstanceOf[Boolean]).single.apply().getOrElse(false)
   
  def create(project: NewProject, members: Seq[String])(implicit session: DBSession = auto): Project = {
     // Insert the project
     val newId = sql"select next value for project_seq as v from dual".map(_.long("v")).single.apply().get
     applyUpdate { insert.into(Project).values(newId, project.name, project.folder) }
     // Add members
     members foreach { email => applyUpdate(insert.into(ProjectMember).values(newId, email)) }
     Project(id = newId, name = project.name, folder = project.folder)
  }
  
}
