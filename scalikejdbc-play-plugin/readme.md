# ScalikeJDBC Play Plugin

## ScalikeJDBC

ScalikeJDBC is A tidy SQL-based DB access library for Scala developers.

https://github.com/seratch/scalikejdbc


## Play 2.0 Scala

ScalikeJDBC works with Play20 seamlessly.

http://www.playframework.org/documentation/2.0/ScalaHome


## Setting up manually

See Zentasks example in detail.

https://github.com/seratch/scalikejdbc/tree/master/scalikejdbc-play-plugin/test/zentasks

### project/Build.scala

```scala
val appDependencies = Seq(
  "com.github.seratch" %% "scalikejdbc"             % "[1.5,)",
  "com.github.seratch" %% "scalikejdbc-play-plugin" % "[1.5,)"
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

### conf/play.plugins

```
10000:scalikejdbc.PlayPlugin
```

### app/models/Project.scala

```scala
import scalikejdbc._
import scalikejdbc.SQLInterpolation._

case class Project(id: Long, folder: String, name: String)

object Project extends SQLSyntaxSupport[Project] {

  def apply(p: ResultName[Project])(rs: WrappedResultSet): Project = new Project(
    id = rs.long(p.id), 
    folder = rs.string(p.folder), 
    name = rs.string(p.name)
  )

  private val p = Project.syntax("p")

  def find(id: Long)(implicit session: DBSession = AutoSession): Option[Project] = withSQL {
    select.from(Project as p).where.eq(p.id, id)
  }.map(Project(p.resultName)).single.apply()

...
}
```

## Generating models

See also:

https://github.com/seratch/scalikejdbc/tree/master/scalikejdbc-mapper-generator

