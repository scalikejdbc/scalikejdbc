package models

import scalikejdbc._
import scalikejdbc.SQLInterpolation._

case class NewProject(folder: String, name: String)
case class Project(id: Long, folder: String, name: String)

case class ProjectMember(projectId: Long, userEmail: String)
object ProjectMember extends SQLSyntaxSupport[ProjectMember] {
  def apply(p: ResultName[ProjectMember])(rs: WrappedResultSet) = new ProjectMember(
    projectId = rs.long(p.projectId),
    userEmail = rs.string(p.userEmail)
  )
}

object Project extends SQLSyntaxSupport[Project] {
 
  def apply(p: ResultName[Project])(rs: WrappedResultSet) = new Project(
    id = rs.long(p.id), 
    folder = rs.string(p.folder), 
    name = rs.string(p.name)
  )

  private val p = Project.syntax("p")
  private val u = User.syntax("u")
  private val m = ProjectMember.syntax("m")

  private val auto = AutoSession
    
  def findById(id: Long)(implicit session: DBSession = auto): Option[Project] = {
    sql"select ${p.result.*} from ${Project as p} where ${p.id} = ${id}".map(Project(p.resultName)).single.apply()
  }
  
  def findInvolving(user: String)(implicit session: DBSession = auto): Seq[Project] = {
    sql"""
      select ${p.result.*} from ${Project as p} 
      join ${ProjectMember as m} on ${p.id} = ${m.project_id}
      where ${m.userEmail} = ${user}
      """.map(Project(p.resultName)).list.apply()
  }

  def rename(id: Long, newName: String)(implicit session: DBSession = auto) {
    sql"update ${Project as p} set ${p.name} = ${name} where ${p.id} = ${id}".update.apply()
  }

  def delete(id: Long)(implicit session: DBSession = auto) {
    sql"delete from ${Project as p} where ${p.id} = ${id}".update.apply()
  }
  
  def deleteInFolder(folder: String)(implicit session: DBSession = auto) {
    sql"delete from ${Project as p} where ${p.folder} = ${folder}".update.apply()
  }
  
  def renameFolder(folder: String, newName: String)(implicit session: DBSession = auto) {
    sql"update ${Project as p} set ${p.folder} = ${newName} where ${p.folder} = ${folder}"
      .update.apply()
  }
  
  def membersOf(project: Long)(implicit session: DBSession = auto): Seq[User] = {
    val u = User.syntax("u")
    sql"""
      select ${u.result.*} 
      from ${User as u} join ${ProjectMember as m} on ${m.userEmail} = ${u.email}
      where ${m.projectId} = ${project}
    """.map(User(u.resultName)).list.apply()
  }
  
  def addMember(project: Long, user: String)(implicit session: DBSession = auto) {
    sql"insert into ${ProjectMember.table} values (${project}, ${user})".update.apply()
  }
  
  def removeMember(project: Long, user: String)(implicit session: DBSession = auto) {
    sql"delete from ${ProjectMember as m} where ${m.projectId} = ${project} and ${m.userEmail} = ${user}".update.apply()
  }
  
  def isMember(project: Long, user: String)(implicit session: DBSession = auto): Boolean = {
    sql"""
      select count(${u.email}) = 1 as is_member 
      from ${User as u} join ${ProjectMember as m} on ${m.userEmail} = ${u.email}
      where ${m.projectId} = ${project} and ${u.email} = ${user}
    """.map(rs => rs.boolean("is_member").asInstanceOf[Boolean]).single.apply().getOrElse(false)
  }
   
  def create(project: NewProject, members: Seq[String])(implicit session: DBSession = auto): Project = {
     // Insert the project
     val newId: Long = sql"select next value for project_seq as v from dual".map(rs => rs.long("v")).single.apply().get
     sql"insert into ${Project.table} (id, name, folder) values (${newId}, ${project.name}, ${project.folder})".update.apply()
     // Add members
     members.foreach(email => sql"insert into ${ProjectMember.table} values (${newId}, ${email})".update.apply())
     Project(id = newId, name = project.name, folder = project.folder)
  }
  
}
