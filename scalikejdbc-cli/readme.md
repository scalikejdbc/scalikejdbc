# ScalikeJDBC Command Line Interface

A simple console to connect database via JDBC.

## Getting started

Execute setup script as follows.

- Mac OS X, Linux

Run the following script from command line.

```sh
curl -L http://git.io/dbconsole | sh
```

- Windows

Run the following batch file.

```
http://git.io/dbconsole.bat
```

The setup script downloads sbt launcher and setting up `dbconsole` command.

```sh
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

Please execute `source ~/.bash_profile`
```

`dbconsole` is pretty simple.

```sh
$ dbconsole -h

dbconsole is an extended sbt console to connect database easily.

Usage:
  dbconsole [OPTION]... [PROFILE]

General options:
  -e, --edit    edit configuration, then exit
  -c, --clean   clean sbt environment, then exit
  -h, --help    show this help, then exit

```

Please try it now with sandbox database.

```sh
$ dbconsole sandbox

Starting sbt console for sandbox...

[info] Set current project to default-8d98e7 (in build file:/Users/seratch/bin/scalikejdbc-cli/)
[info] Starting scala interpreter...
[info] 
import scalikejdbc._
import scalikejdbc.StringSQLRunner._
initialize: ()Unit
Welcome to Scala version 2.9.2 (Java HotSpot(TM) Client VM, Java 1.6.0_29).
Type in expressions to have them evaluated.
Type :help for more information.

scala> tables
IRIS

scala> describe("iris")

Table: IRIS (The Iris flower data set is a multivariate data set introduced by Sir Ronald Fisher (1936) as an example of discrimina..)
+--------------+-------------+------+-----+---------+-----------------+----------------------+
| Field        | Type        | Null | Key | Default | Extra           | Description          |
+--------------+-------------+------+-----+---------+-----------------+----------------------+
| IRIS_ID      | INTEGER(10) | NO   | PRI | NULL    |                 | The unique id        |
| SEPAL_LENGTH | DOUBLE(17)  | NO   |     | NULL    |                 | The length of sepals |
| SEPAL_WIDTH  | DOUBLE(17)  | NO   |     | NULL    |                 | The width of sepals  |
| PETAL_LENGTH | DOUBLE(17)  | NO   |     | NULL    |                 | The length of petals |
| PETAL_WIDTH  | DOUBLE(17)  | NO   |     | NULL    |                 | The width of petals  |
| SPECIES      | VARCHAR(16) | NO   |     | NULL    |                 | The species name     |
+--------------+-------------+------+-----+---------+-----------------+----------------------+
Indexes:
  "PRIMARY_KEY_2" UNIQUE, (IRIS_ID)


scala> "select count(1) from iris".run
res2: List[Map[String,Any]] = List(Map(COUNT(1) -> 150))

scala> "select count(1) from iris".as[Long]
res3: Long = 150

scala> "select iris_id from iris".asList[Int]
res4: List[Int] = List(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147, 148, 149, 150)

scala> "select species, count(1) from iris group by species".run
res5: List[Map[String,Any]] = List(Map(SPECIES -> versicolor, COUNT(1) -> 50), Map(SPECIES -> setosa, COUNT(1) -> 50), Map(SPECIES -> virginica, COUNT(1) -> 50))

scala> "insert into iris values (151, 0.1, 0.2, 0.3, 0.4, 'xxx');".run.asOption[Boolean]
res6: Option[Boolean] = Some(false)

scala> DB localTx { implicit session =>
     |   "insert into iris values (152, 0.1, 0.2, 0.3, 0.4, 'yyy');".run
     |   throw new RuntimeException
     | }
java.lang.RuntimeException
	at $anonfun$1.apply(<console>:16)
	at $anonfun$1.apply(<console>:14)
	at scalikejdbc.DB$$anonfun$localTx$2$$anonfun$apply$1.apply(DB.scala:606)
	at scala.util.control.Exception$Catch.apply(Exception.scala:88)
	at scalikejdbc.DB$$anonfun$localTx$2.apply(DB.scala:604)
	...

scala> "select count(1) from iris".as[Long]
res8: Long = 151

scala> :q

[success] Total time: 48 s, completed Nov 29, 2012 8:56:36 PM
```

## Configuration

Use `dbconsole -e` command to edit `~/bin/scalikejdbc-cli/config.properties`.

```properties
sandbox.jdbc.url=jdbc:h2:file:db/sandbox
sandbox.jdbc.username=
sandbox.jdbc.password=
#mysql.jdbc.url=jdbc:mysql://localhost:3306/dbname
#postgres.jdbc.url=jdbc:postgresql://localhost:5432/dbname
#oracle.jdbc.url=jdbc:oracle:thin:@localhost:1521:dbname
```


### Adding custom functions
dbconsole loads all Scala files in `~/bin/scalikejdbc-cli/init/` on start-up. There is only a file named `init.scala` in it by default.
You can add your custom functions by adding Scala files to this directory. The files you added are loaded on start-up together as `initialCommands` of sbt console.

For example, if you want to use `hello()` function in dbcosole, save `def hello() = println("Hello")` as `hello.scala` or somethiing.
