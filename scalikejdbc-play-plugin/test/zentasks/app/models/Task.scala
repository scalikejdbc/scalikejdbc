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

  def apply(syntax: SyntaxProvider[Task])(rs: WrappedResultSet) = {
    val t = syntax.resultName
    new Task(
      id = rs.long(t.id), 
      folder = rs.string(t.folder), 
      project = rs.long(t.project), 
      title = rs.string(t.title), 
      done = rs.boolean(t.done), 
      dueDate = rs.timestampOpt(t.dueDate), 
      assignedTo = rs.stringOpt(t.assignedTo)
    )
  }

  def apply(t: SyntaxProvider[Task], p: SyntaxProvider[Project])(rs: WrappedResultSet): (Task, Project) = (Task(t)(rs), Project(p)(rs))

  private val t = Task.syntax("t")
  private val p = Project.syntax("p")
  private val m = ProjectMember.syntax("m")

  private val auto = AutoSession
  
  def findById(id: Long)(implicit session: DBSession = auto): Option[Task] = withSQL {
    select.from(Task as t).where.eq(t.id, id)
  }.map(Task(t)).single.apply()
  
  def findTodoInvolving(user: String)(implicit session: DBSession = auto): Seq[(Task,Project)] = withSQL {
    select
      .from(Task as t)
      .join(ProjectMember as m).on(m.projectId, t.project)
      .join(Project as p).on(p.id, m.projectId)
      .where.append(sqls"${t.done} = false").and.eq(m.userEmail, user)
  }.map(Task(t, p)).list.apply()

  def findByProject(project: Long)(implicit session: DBSession = auto): Seq[Task] = withSQL {
    select.from(Task as t).where.eq(t.project, project)
  }.map(Task(t)).list.apply()

  /**
   * Delete a task
   */
  def delete(id: Long)(implicit session: DBSession = auto): Unit = applyUpdate {
    deleteFrom(Task as t).where.eq(t.id, id)
  }
  
  def deleteInFolder(projectId: Long, folder: String)(implicit session: DBSession = auto): Unit = applyUpdate {
    deleteFrom(Task as t).where.eq(t.project, projectId).and.eq(t.folder, folder)
  }
  
  def markAsDone(taskId: Long, done: Boolean)(implicit session: DBSession = auto): Unit = applyUpdate {
    update(Task as t).set(t.done -> done).where.eq(t.id, taskId)
  }
  
  def renameFolder(projectId: Long, folder: String, newName: String)(implicit session: DBSession = auto): Unit = applyUpdate {
    update(Task as t).set(t.folder -> newName).where.eq(t.folder, folder).and.eq(t.project, projectId)
  }
 
  def isOwner(task: Long, user: String)(implicit session: DBSession = auto): Boolean = withSQL {
   select(sqls"count(${t.id}) = 1 as v")
     .from(Task as t)
     .join(Project as p).on(t.project, p.id)
     .join(ProjectMember as m).on(m.projectId, p.id)
     .where.eq(m.userEmail, user).and.eq(t.id, task)
  }.map(rs => rs.boolean("v").asInstanceOf[Boolean]).single.apply().getOrElse(false)

  def create(task: NewTask)(implicit session: DBSession = auto): Task = {
    val newId = sql"select next value for task_seq as v from dual".map(rs => rs.long("v")).single.apply().get
    applyUpdate { 
      insert.into(Task).values(newId, task.title, task.done, task.dueDate, task.assignedTo, task.project, task.folder)
    }
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
