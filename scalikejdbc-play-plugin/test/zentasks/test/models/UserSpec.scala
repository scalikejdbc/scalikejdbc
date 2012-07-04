package models

import org.specs2.mutable._

import scalikejdbc.NamedDB

class UserSpec extends Specification {

  object Fixture {

    import scalikejdbc._

    ConnectionPool.add("UserSpec", "jdbc:h2:mem:UserSpec", "", "")
    NamedDB("UserSpec") autoCommit {
      implicit session =>

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

        SQL(ddl).execute.apply()
    }

  }

  "User" should {
    "have #create and #findByEmail" in {
      NamedDB("UserSpec") localTx {
        implicit session =>
          val user = User.findByEmail("seratch@gmail.com").getOrElse {
            User.create(
              User(
                email = "seratch@gmail.com",
                name = "seratch",
                password = "play20"
              )
            )
          }
          user.name must equalTo("seratch")
      }
    }
  }

}

