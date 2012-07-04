import play.api._

import models._

object Global extends GlobalSettings {
  
  override def onStart(app: Application) {
    InitialData.createTables()
    InitialData.insert()
  }
  
}

/**
 * Initial set of data to be imported 
 * in the sample application.
 */
object InitialData {
  
  def date(str: String) = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(str)

  def createTables() = {

    import scalikejdbc._
    val ddl = """
drop table user if exists;
create table user (
  email                     varchar(255) not null primary key,
  name                      varchar(255) not null,
  password                  varchar(255) not null
);

drop table project if exists;
create table project (
  id                        bigint not null primary key,
  name                      varchar(255) not null,
  folder                    varchar(255) not null
);

drop sequence project_seq if exists;
create sequence project_seq start with 1000;

drop table project_member if exists;
create table project_member (
  project_id                bigint not null,
  user_email                varchar(255) not null,
  foreign key(project_id)   references project(id) on delete cascade,
  foreign key(user_email)   references user(email) on delete cascade
);

drop table task if exists;
create table task (
  id                        bigint not null primary key,
  title                     varchar(255) not null,
  done                      boolean,
  due_date                  timestamp,
  assigned_to               varchar(255),
  project                   bigint not null,
  folder                    varchar(255),
  foreign key(assigned_to)  references user(email) on delete set null,
  foreign key(project)      references project(id) on delete cascade
);

drop sequence task_seq if exists;
create sequence task_seq start with 1000;
"""

    DB autoCommit { implicit session =>
      try {
        SQL("select * from user").map(rs => rs).list.apply()
      } catch { case e => 
        SQL(ddl).execute.apply()
      }
    }

  }
  
  def insert() = {
    
    if(User.findAll.isEmpty) {
      
      Seq(
        User("guillaume@sample.com", "Guillaume Bort", "secret"),
        User("maxime@sample.com", "Maxime Dantec", "secret"),
        User("sadek@sample.com", "Sadek Drobi", "secret"),
        User("erwan@sample.com", "Erwan Loisant", "secret")
      ).foreach(User.create)
      
      val projects = Seq(
        NewProject("Play framework", "Play 2.0") -> Seq("guillaume@sample.com", "maxime@sample.com", "sadek@sample.com", "erwan@sample.com"),
        NewProject("Play framework", "Play 1.2.4") -> Seq("guillaume@sample.com", "erwan@sample.com"),
        NewProject("Play framework", "Website") -> Seq("guillaume@sample.com", "maxime@sample.com"),
        NewProject("Zenexity", "Secret project") -> Seq("guillaume@sample.com", "maxime@sample.com", "sadek@sample.com", "erwan@sample.com"),
        NewProject("Zenexity", "Playmate") -> Seq("maxime@sample.com"),
        NewProject("Personal", "Things to do") -> Seq("guillaume@sample.com"),
        NewProject("Zenexity", "Play samples") -> Seq("guillaume@sample.com", "maxime@sample.com"),
        NewProject("Personal", "Private") -> Seq("maxime@sample.com"),
        NewProject("Personal", "Private") -> Seq("guillaume@sample.com"),
        NewProject("Personal", "Private") -> Seq("erwan@sample.com"),
        NewProject("Personal", "Private") -> Seq("sadek@sample.com")
      ).map {
        case (project,members) => Project.create(project, members)
      }

      Seq(
        NewTask("Todo", projects(0).id, "Fix the documentation", false, None, Some("guillaume@sample.com")),
        NewTask("Urgent", projects(0).id, "Prepare the beta release", false, Some(date("2011-11-15")), None),
        NewTask("Todo", projects(8).id, "Buy some milk", false, None, None),
        NewTask("Todo", projects(1).id, "Check 1.2.4-RC2", false, Some(date("2011-11-18")), Some("guillaume@sample.com")),
        NewTask("Todo", projects(6).id, "Finish zentask integration", true, Some(date("2011-11-15")), Some("maxime@sample.com")),
        NewTask( "Todo", projects(3).id, "Release the secret project", false, Some(date("2012-01-01")), Some("sadek@sample.com"))
      ).foreach(Task.create)
      
    }
    
  }
  
}
