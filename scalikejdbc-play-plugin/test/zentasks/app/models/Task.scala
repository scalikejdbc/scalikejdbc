package models

import java.util.Date

import scalikejdbc._

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

object Task {
  
  // -- Parsers
  
  /**
   * Parse a Task from a ResultSet
   */
  val simple = (rs: WrappedResultSet) => Task(
     id = rs.long("id"), 
     folder = rs.string("folder"), 
     project = rs.long("project"), 
     title = rs.string("title"), 
     done = rs.boolean("done"), 
     dueDate = Option(rs.timestamp("due_date")), 
     assignedTo = Option(rs.string("assigned_to"))
   )
  
  val withProject = (rs: WrappedResultSet) => (
    Task(
      id = rs.long("task_id"),
      folder = rs.string("folder"), 
      project = rs.long("project_id"), 
      title = rs.string("title"), 
      done = rs.boolean("done"), 
      dueDate = Option(rs.timestamp("due_date")), 
      assignedTo = Option(rs.string("assigned_to"))
    ), 
    Project(
      id = rs.long("project_id"), 
      folder = rs.string("folder"), 
      name = rs.string("project_name")
    )
  )
  
  // -- Queries
  
  /**
   * Retrieve a Task from the id.
   */
  def findById(id: Long)(implicit session: DBSession = AutoSession): Option[Task] = {
    SQL("select * from task where id = {id}").bindByName('id -> id).map(simple).single.apply()
  }
  
  /**
   * Retrieve todo tasks for the user.
   */
  def findTodoInvolving(user: String)(implicit session: DBSession = AutoSession): Seq[(Task,Project)] = {
    SQL(
      """
        select 
          task.id as task_id,
          project.id as project_id,
          project.name as project_name,
          *
        from 
          task 
          join project_member on project_member.project_id = task.project 
          join project on project.id = project_member.project_id
        where 
          task.done = false and project_member.user_email = {user}
      """
    ).bindByName('user -> user).map(withProject).list.apply().toSeq
  }
  
  /**
   * Find tasks related to a project
   */
  def findByProject(project: Long)(implicit session: DBSession = AutoSession): Seq[Task] = {
    SQL(
      """
        select * from task 
        where task.project = {project}
      """
    ).bindByName('project -> project).map(simple).list.apply().toSeq
  }

  /**
   * Delete a task
   */
  def delete(id: Long)(implicit session: DBSession = AutoSession) {
    SQL("delete from task where id = {id}").bindByName('id -> id).update.apply()
  }
  
  /**
   * Delete all task in a folder.
   */
  def deleteInFolder(projectId: Long, folder: String)(implicit session: DBSession = AutoSession) {
    SQL("delete from task where project = {project} and folder = {folder}")
      .bindByName('project -> projectId, 'foler -> folder).update.apply()
  }
  
  /**
   * Mark a task as done or not
   */
  def markAsDone(taskId: Long, done: Boolean)(implicit session: DBSession = AutoSession) {
    SQL("update task set done = {done} where id = {id}")
      .bindByName('id -> taskId, 'done -> done).update.apply()
  }
  
  /**
   * Rename a folder.
   */
  def renameFolder(projectId: Long, folder: String, newName: String)(implicit session: DBSession = AutoSession) {
    SQL("update task set folder = {newFolder} where folder = {folder} and project = {project}")
      .bindByName('folder -> folder, 'newFolder -> newName, 'project -> projectId).update.apply()
  }
  
  /**
   * Check if a user is the owner of this task
   */
  def isOwner(task: Long, user: String)(implicit session: DBSession = AutoSession): Boolean = {
    SQL(
      """
        select count(task.id) = 1 as v from task 
        join project on task.project = project.id 
        join project_member on project_member.project_id = project.id 
        where project_member.user_email = {user} and task.id = {task}
      """
    ).bindByName('user -> user, 'task -> task)
      .map(rs => rs.boolean("v").asInstanceOf[Boolean]).single.apply().getOrElse(false)
  }

  /**
   * Create a Task.
   */
  def create(task: NewTask)(implicit session: DBSession = AutoSession): Task = {
    val newId = SQL("select next value for task_seq as v from dual").map(rs => rs.long("v")).single.apply().get
    SQL(
      """
        insert into task (id, folder, project, title, done, due_date, assigned_to) values (
          {id}, {folder}, {project}, {title}, {done}, {dueDate}, {assignedTo} 
        )
      """
    ).bindByName(
      'id -> newId,
      'folder -> task.folder, 
      'project -> task.project, 
      'title -> task.title, 
      'done -> task.done,
      'dueDate -> task.dueDate,
      'assignedTo -> task.assignedTo
    ).update.apply()

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
