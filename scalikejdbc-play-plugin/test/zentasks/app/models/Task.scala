package models

import java.util.Date

import scalikejdbc._
import scalikejdbc.SQLInterpolation._

case class NewTask(
  folder: String, 
  project: Long, 
  title: String, 
  done: Boolean, 
  dueDate: Option[Date], 
  assignedTo: Option[String]
)

case class Task(
  id: Long, 
  folder: String,
  project: Long,
  title: String,
  done: Boolean,
  dueDate: Option[Date],
  assignedTo: Option[String]
)

object Task extends SQLSyntaxSupport[Task] {
  
  def apply(t: ResultName[Task])(rs: WrappedResultSet) = new Task(
     id = rs.long(t.id), 
     folder = rs.string(t.folder), 
     project = rs.long(t.project), 
     title = rs.string(t.title), 
     done = rs.boolean(t.done), 
     dueDate = rs.timestampOpt(t.due_date), 
     assignedTo = rs.stringOpt(t.assigned_to)
  )

  def apply(t: ResultName[Task], p: ResultName[Project])(rs: WrappedResultSet): (Task, Project) = (Task(t)(rs), Project(p)(rs))

  private val t = Task.syntax("t")
  private val p = Project.syntax("p")
  private val m = ProjectMember.syntax("m")
  private val auto = AutoSession
  
  def findById(id: Long)(implicit session: DBSession = auto): Option[Task] = {
    sql"select ${t.result.*} from ${Task as t} where ${t.id} = ${id}".map(Task(t.resultName)).single.apply()
  }
  
  def findTodoInvolving(user: String)(implicit session: DBSession = auto): Seq[(Task,Project)] = {
    sql"""
      select 
        ${t.result.*}, ${p.result.*}
      from 
        ${Task as t} 
        join ${ProjectMember as m} on ${m.projectId} = ${t.project}
        join ${Project as p} on ${p.id} = ${m.projectId}
      where 
        ${t.done} = false and ${m.userEmail} = ${user}
    """.map(Task(t.resultName, p.resultName)).list.apply()
  }

  def findByProject(project: Long)(implicit session: DBSession = auto): Seq[Task] = {
    sql"select ${t.result.*} from ${Task as t} where ${t.project} = ${project}".map(Task(t.resultName)).list.apply()
  }

  /**
   * Delete a task
   */
  def delete(id: Long)(implicit session: DBSession = auto) {
    sql"delete from ${Task as t} where ${t.id} = ${id}".update.apply()
  }
  
  def deleteInFolder(projectId: Long, folder: String)(implicit session: DBSession = auto) {
    sql"delete from ${Task as t} where ${t.project} = ${projectId} and ${t.folder} = ${folder}".update.apply()
  }
  
  def markAsDone(taskId: Long, done: Boolean)(implicit session: DBSession = auto) {
    sql"update ${Task as t} set ${t.done} = ${done} where ${t.id} = ${taskId}".update.apply()
  }
  
  def renameFolder(projectId: Long, folder: String, newName: String)(implicit session: DBSession = auto) {
    sql"update ${Task as t} set ${t.folder} = ${newName} where ${t.folder} = ${folder} and ${t.project} = ${projectId}".update.apply()
  }
  
  def isOwner(task: Long, user: String)(implicit session: DBSession = auto): Boolean = {
   val p = Project.syntax("p")
   sql"""
      select count(${t.id}) = 1 as v 
      from 
        ${Task as t}
        join ${Project as p} on ${t.project} = ${p.id}
        join ${ProjectMember as m} on ${m.projectId} = ${p.id}
      where 
        ${m.userEmail} = ${user} and ${t.id} = ${task}
    """.map(rs => rs.boolean("v").asInstanceOf[Boolean]).single.apply().getOrElse(false)
  }

  def create(task: NewTask)(implicit session: DBSession = auto): Task = {
    val newId = sql"select next value for task_seq as v from dual".map(rs => rs.long("v")).single.apply().get
    sql"""
      insert into ${Task.table} (id, folder, project, title, done, due_date, assigned_to) values (
        ${newId}, ${task.folder}, ${task.project}, ${task.title}, ${task.done}, ${task.dueDate}, ${task.assignedTo} 
      )
    """.update.apply()

    Task(
      id = newId,
      folder = task.folder,
      project = task.project,
      title = task.title,
      done = task.done,
      dueDate = task.dueDate,
      assignedTo = task.assignedTo
    )
  }
  
}
