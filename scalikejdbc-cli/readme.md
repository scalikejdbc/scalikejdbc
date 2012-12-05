# ScalikeJDBC Command Line Interface

A simple console to connect database via JDBC.

## Getting started

Execute setup script as follows.

```
curl -L http://git.io/dbconsole | sh
```

The script downloads sbt launcher and prepare commands - `dbconsole` and `dbconsole_config`.

```
$ curl -L http://git.io/dbconsole | sh
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100  3660  100  3660    0     0   2619      0  0:00:01  0:00:01 --:--:--  2619
--2012-11-29 20:25:26--  http://repo.typesafe.com/typesafe/ivy-releases/org.scala-sbt/sbt-launch/0.12.1/sbt-launch.jar
Resolving repo.typesafe.com... 23.21.39.75
Connecting to repo.typesafe.com|23.21.39.75|:80... connected.
HTTP request sent, awaiting response... 200 OK
Length: 1103618 (1.1M) [application/java-archive]
Saving to: `sbt-launch.jar'

100%[===============================================================================================================>] 1,103,618    571K/s   in 1.9s    

2012-11-29 20:17:41 (580 KB/s) - `sbt-launch.jar' saved [1103618/1103618]


command installed to /Users/k-sera/bin/scalikejdbc-cli/dbconsole
command installed to /Users/k-sera/bin/scalikejdbc-cli/dbconsole_config

Please execute `source ~/.bash_profile`

$ dbconsole

--- DB Console with ScalikeJDBC ---

Select a profile defined at ~/bin/scalikejdbc-cli/config.properties

default

Starting sbt console...

[info] Set current project to default-8d98e7 (in build file:/Users/seratch/bin/scalikejdbc-cli/)
[info] Starting scala interpreter...
[info] 
import scalikejdbc._
import scalikejdbc.StringSQLRunner._
initialize: ()Unit
Welcome to Scala version 2.9.2 (Java HotSpot(TM) Client VM, Java 1.6.0_29).
Type in expressions to have them evaluated.
Type :help for more information.

scala> "create table users(id bigint primary key, name varchar(256));".run
res0: List[Map[String,Any]] = List(Map(RESULT -> false))

scala> "insert into users values (1, 'Alice')".run
res1: List[Map[String,Any]] = List(Map(RESULT -> false))

scala> "insert into users values (2, 'Bob')".as[Boolean]
res2: Boolean = false

scala> "select * from users".run
res3: List[Map[String,Any]] = List(Map(ID -> 1, NAME -> Alice), Map(ID -> 2, NAME -> Bob))

scala> "select name from users".asList[String]
res4: List[String] = List(Alice, Bob)

scala> "select id from users".asList[Long]
res5: List[Long] = List(1, 2)

scala> :q

[success] Total time: 48 s, completed Nov 29, 2012 8:56:36 PM
```

## Configuration

Use `dbconsole_config` command to edit `~/bin/scalikejdbc-cli/config.properties`.

```
default.jdbc.url=jdbc:h2:mem:default
default.jdbc.username=
default.jdbc.password=
pg.jdbc.url=jdbc:postgresql://hostname/dbname
pg.jdbc.username=alice
pg.jdbc.password=bob
```


