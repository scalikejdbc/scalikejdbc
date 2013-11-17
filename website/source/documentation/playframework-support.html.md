---
title: Play Framework Support - ScalikeJDBC
---

## Play Framework Support

<hr/>
### How to setup

See [/documentation/setup](/documentation/setup.html).

<hr/>
### Configuration

Here is some configuration examples. Basically it's very simple:

#### conf/application.conf

```sh
# Database configuration
# ~~~~~
# You can declare as many datasources as you want.
# By convention, the default datasource is named `default`
db.default.driver=org.h2.Driver
db.default.url="jdbc:h2:mem:play"
db.default.user="sa"
db.default.password="sa"

db.secondary.driver=org.h2.Driver
db.secondary.url="jdbc:h2:mem:play2"
db.secondary.user="sa"
db.secondary.password="sa"

# ScalikeJDBC original configuration

#db.default.poolInitialSize=10
#db.default.poolMaxSize=10
#db.default.poolValidationQuery=

scalikejdbc.global.loggingSQLAndTime.enabled=true
scalikejdbc.global.loggingSQLAndTime.logLevel=debug
scalikejdbc.global.loggingSQLAndTime.warningEnabled=true
scalikejdbc.global.loggingSQLAndTime.warningThresholdMillis=1000
scalikejdbc.global.loggingSQLAndTime.warningLogLevel=warn

#scalikejdbc.play.closeAllOnStop.enabled=true

# You can disable the default DB plugin
dbplugin=disabled
evolutionplugin=disabled
```

#### Fixtures

Fixtures are optional. If you don't nee, no need to use them.

##### conf/application.conf

```sh
db.default.fixtures.test=[ "project.sql", "project_member.sql" ]
```

##### conf/db/fixtures/default/project.sql

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

##### conf/db/fixtures/defaut/project_member.sql

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

<hr/>
### More Examples

Take a look at Typesafe Activator template:

![Typesafe](images/typesafe.png)

You can try a [Play framework](http://www.playframework.com/) sample app which uses ScalikeJDBC on [Typesafe Activator](http://typesafe.com/activator).

Activator page: [Hello ScalikeJDBC!](http://typesafe.com/activator/template/scalikejdbc-activator-template)

See on GitHub: [seratch/hello-scalikejdbc](https://github.com/seratch/hello-scalikejdbc)
