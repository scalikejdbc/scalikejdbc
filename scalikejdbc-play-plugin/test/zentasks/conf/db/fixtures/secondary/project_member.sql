
# --- !Ups
create table project_member (
project_id bigint not null,
user_email varchar(255) not null,
foreign key(project_id) references project(id) on delete cascade,
foreign key(user_email) references users(email) on delete cascade
);

# --- !Downs
drop table project_member;

