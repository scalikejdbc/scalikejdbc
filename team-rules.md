## ScalikeJDBC Contributers' Guide

### Versioning Policy

"{major}.{minor}.{fix}"

- major: kept for supported Scala version compatibily, version 1 must support Scala 2.9 & 2.10, version 2 will support only Scala 2.10 & Scala 2.11 and ???
- minor: kept for functionality and APIs compatibility, same version must provide same functionality and APIs
- fix: to release some improvements, bug fixes and new features

### Coding Rule for project members

- Basically push "feature/xxx" branch first
- Create "feature/xxx" branch and create a pull request when you need code review
- Compatibility always must be kept as far as possible
- scalariform must be applied to all Scala source code
- Prefer creating scala source code for each class/object/trait (of course, except for sealed trait)

### Testing

- Testing with default settings is required when push changes

```sh
sbt library/test
sbt interpolation/test

./scripts/run_tests.sh h2
```

- Testing with H2/HSQLDB/MySQL/PostgreSQL (only latest version) is required before release

```sh
./scripts/run_all_tests.sh
```

See the required settings here:

scalikejdbc-library/src/test/resources/*.properties

### Release

#### Required

- Sonatype release account

Currently @seratch's personal account.

- $HOME/.sbt/0.13/sonatype.sbt

``` scala
credentials += Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", "xxx", "yyy")
```

- $HOME/.sbt/0.13/plugins/gpg.sbt

```scala
// Use latest version
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")
```

#### Operations

The release manager is @seratch. Currently, all the following operations should be done by @seratch.

- Create release note under notes.
- git flow release {version}
- Run all the `./scripts/release_*.sh` and close/publish on the sonatype console
- Post to http://notes.implicit.ly/ by using [herald](https://github.com/n8han/herald)
- Post to [ScalikeJDBC Users Group](https://groups.google.com/forum/#!forum/scalikejdbc-users-group)
- Twitter account: @scalikejdbc

