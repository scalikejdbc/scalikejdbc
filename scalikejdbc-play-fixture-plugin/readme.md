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

INSERT INTO project (id, name, folder) VALUES (1, 'Play 2.0', 'Play framework');
INSERT INTO project (id, name, folder) VALUES (2, 'Play 1.2.4', 'Play framework');
INSERT INTO project (id, name, folder) VALUES (3, 'Website', 'Play framework');
INSERT INTO project (id, name, folder) VALUES (4, 'Secret project', 'Zenexity');
INSERT INTO project (id, name, folder) VALUES (5, 'Playmate', 'Zenexity');
INSERT INTO project (id, name, folder) VALUES (6, 'Things to do', 'Personal');
INSERT INTO project (id, name, folder) VALUES (7, 'Play samples', 'Zenexity');
INSERT INTO project (id, name, folder) VALUES (8, 'Private', 'Personal');
INSERT INTO project (id, name, folder) VALUES (9, 'Private', 'Personal');
INSERT INTO project (id, name, folder) VALUES (10, 'Private', 'Personal');
INSERT INTO project (id, name, folder) VALUES (11, 'Private', 'Personal');
ALTER SEQUENCE project_seq RESTART WITH 12;

# --- !Downs
ALTER SEQUENCE project_seq RESTART WITH 1;
DELETE FROM project;
```

### conf/db/fixtures/defaut/project_member.sql

```sql
# --- !Ups

INSERT INTO project_member (project_id, user_email) VALUES (1, 'guillaume@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (1, 'maxime@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (1, 'sadek@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (1, 'erwan@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (2, 'guillaume@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (2, 'erwan@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (3, 'guillaume@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (3, 'maxime@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (4, 'guillaume@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (4, 'maxime@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (4, 'sadek@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (4, 'erwan@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (5, 'maxime@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (6, 'guillaume@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (7, 'guillaume@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (7, 'maxime@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (8, 'maxime@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (9, 'guillaume@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (10, 'erwan@sample.com');
INSERT INTO project_member (project_id, user_email) VALUES (11, 'sadek@sample.com');

# --- !Downs

DELETE FROM project_member;
```

