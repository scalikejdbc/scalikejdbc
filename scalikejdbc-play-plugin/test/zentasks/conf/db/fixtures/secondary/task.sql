# --- !Ups
create table task (
id bigint not null primary key,
title varchar(255) not null,
done boolean,
due_date timestamp,
assigned_to varchar(255),
project bigint not null,
folder varchar(255),
foreign key(assigned_to) references users(email) on delete set null,
foreign key(project) references project(id) on delete cascade
);
create sequence task_seq start with 1000;

# --- !Downs
drop sequence task_seq;
drop table task;

