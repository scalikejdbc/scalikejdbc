# ScalikeJDBC Play Fixture Plugin

## Setting up manually

See Zentasks example in detail.

https://github.com/seratch/scalikejdbc/tree/master/scalikejdbc-play-plugin/test/zentasks

### project/Build.scala

```scala
val appDependencies = Seq(
  "com.github.seratch" %% "scalikejdbc"                     % "[1.5,)",
  "com.github.seratch" %% "scalikejdbc-play-plugin"         % "[1.5,)",
  "com.github.seratch" %% "scalikejdbc-play-fixture-plugin" % "[1.5,)"
)
```

### conf/application.conf

This plugin uses the default Database configuration.

```
# Database configuration
# ~~~~~ 
# You can declare as many datasources as you want.
# By convention, the default datasource is named `default`
db.default.driver=org.h2.Driver
db.default.url="jdbc:h2:mem:play"
db.default.user="sa"
db.default.password="sa"
db.default.fixtures.test=[ "project.sql", "project_member.sql" ]

# You can disable the default DB plugin
dbplugin=disabled
evolutionplugin=disabled
```

### conf/play.plugins

PlayFixturePlugin should be loaded after PlayPlugin.

```
10000:scalikejdbc.PlayPlugin
11000:scalikejdbc.PlayFixturePlugin
```

### conf/db/fixtures/default/project.sql

```sql
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
```

### conf/db/fixtures/defaut/project_member.sql

```sql
# --- !Ups
create table project_member (
project_id bigint not null,
user_email varchar(255) not null,
foreign key(project_id) references project(id) on delete cascade,
foreign key(user_email) references users(email) on delete cascade
);

# --- !Downs
drop table project_member;
```

