
# --- !Ups
create table project (
id bigint not null primary key,
name varchar(255) not null,
folder varchar(255) not null
);
create sequence project_seq start with 1000;

# --- !Downs
drop sequence project_seq;
drop table project;

