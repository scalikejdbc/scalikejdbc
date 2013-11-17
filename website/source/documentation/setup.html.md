---
title: Setup - ScalikeJDBC
---

## Setup

<hr/>
ScalikeJDBC libraries are available on the Maven central repository.

[http://search.maven.org/#search%7Cga%7C1%7Cscalikejdbc](http://search.maven.org/#search%7Cga%7C1%7Cscalikejdbc)

<hr/>
### Core Library & Interpolation

Add latest version into your `build.sbt` or `project/Build.scala`. Don't forget JDBC driver and slf4j implementation.

If you're still using Scala 2.9, you can't use `scalikejdbc-interpolation`.

```
libraryDependencies ++= Seq(
  "com.github.seratch" %% "scalikejdbc"               % "[1.6,)",
  "com.github.seratch" %% "scalikejdbc-interpolation" % "[1.6,)",
  "com.h2database"     %  "h2"                        % "[1.3,)",
  "ch.qos.logback"     %  "logback-classic"           % "[1.0,)"
)
```

In your application, just add the following import.

```java
import scalikejdbc._, SQLInterpolation._
```

<hr/>
### Testing Support

Using `scalikejdbc-test` is highly recommended to improve your application.

```
libraryDependencies ++= Seq(
  "com.github.seratch" %% "scalikejdbc"               % "[1.6,)",
  "com.github.seratch" %% "scalikejdbc-interpolation" % "[1.6,)",
  "com.github.seratch" %% "scalikejdbc-test"          % "[1.6,)"   % "test",
  "com.h2database"     %  "h2"                        % "[1.3,)",
  "ch.qos.logback"     %  "logback-classic"           % "[1.0,)"
)
```

Usage: [/documentation/testing](/documentation/testing.html)

<hr/>
### Typesafe Config Reader

If you use `application.conf` as settings file, add `scalikejdbc-config` too.

```
libraryDependencies ++= Seq(
  "com.github.seratch" %% "scalikejdbc"               % "[1.6,)",
  "com.github.seratch" %% "scalikejdbc-interpolation" % "[1.6,)",
  "com.github.seratch" %% "scalikejdbc-config"        % "[1.6,)",
  "com.h2database"     %  "h2"                        % "[1.3,)",
  "ch.qos.logback"     %  "logback-classic"           % "[1.0,)"
)
```

Usage: [/documentation/configuration](/documentation/configuration.html)

<hr/>
### Reverse Engineering

ScalikeJDBC support reverse engineering Scala code from existing database.
You need to setup an sbt plugin named `scalikejdbc-mapper-generator`.

##### project/plugins.sbt

```
// Don't forget adding your JDBC driver
libraryDependencies += "org.hsqldb" % "hsqldb" % "[2,)"

addSbtPlugin("com.github.seratch" %% "scalikejdbc-mapper-generator" % "[1.6,)")
```

##### build.sbt

```
scalikejdbcSettings
```

##### project/scalikejdbc.properties

```
jdbc.driver=org.h2.Driver
jdbc.url=jdbc:h2:file:db/hello
jdbc.username=sa
jdbc.password=
jdbc.schema=
generator.packageName=models
# generator.lineBreak: LF/CRLF
geneartor.lineBreak=LF
# generator.template: basic/namedParameters/executable/interpolation/queryDsl
generator.template=queryDsl
# generator.testTemplate: specs2unit/specs2acceptance/ScalaTestFlatSpec
generator.testTemplate=specs2unit
generator.encoding=UTF-8
```

Usage: [/documentation/reverse-engineering](/documentation/reverse-engineering.html)

<hr/>
### Play Framework Integration

Add `scalikejdbc-play-plugin` and `scalikejdbc-play-fixture-plugin` (optional) as Play plugins.

##### project/Build.scala

```
val appDependencies = Seq(
  "com.github.seratch" %% "scalikejdbc"             % "[1.6,)",
  "com.github.seratch" %% "scalikejdbc-play-plugin" % "[1.6,)",
  "com.github.seratch" %% "scalikejdbc-play-fixture-plugin" % "[1.6,)", // optional
  // substitute this for whatever DB driver you're using:
  "com.h2database"     %  "h2"                      % "1.3,)"
)
```

##### conf/play.plugins

```
10000:scalikejdbc.PlayPlugin
```

If you use fixture-plugin too, PlayFixturePlugin should be loaded after PlayPlugin:

```
10000:scalikejdbc.PlayPlugin
11000:scalikejdbc.PlayFixturePlugin
```

Usage: [/documentation/playframework-support](/documentation/playframework-support.html)


<hr/>
### dbconsole

A simple console to connect database via JDBC.

##### Mac OS X, Linux

```
curl -L http://git.io/dbconsole | sh
```

##### Windows

```
http://git.io/dbconsole.bat
```

Usage: [/documentation/dbconsole](/documentation/dbconsole.html)
