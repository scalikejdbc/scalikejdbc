## ScalikeJDBC Contributors' Guide

### Versioning Policy

"{major}.{minor}.{fix}"

- major: Change only when the supported Scala major version changes:
  - Version 1 supports Scala 2.9 & 2.10
  - Version 2 supports Scala 2.10, 2.11, and 2.12
  - Version 3 supports Scala 2.11, 2.12, and 2.13
  - Version 4 supports Scala 2.12, 2.13, and 3
- minor: Change to indicate functionality and API compatibility changes, the same minor version must provide the same functionality and APIs
- fix: For releasing smaller improvements, bug fixes and new features

### Coding Rules for Project Members

- Push "feature/xxx" branch first
- Create a "feature/xxx" branch and create a pull request when you need code review
- Compatibility always must be kept as far back as possible
- scalafmt must be applied to all Scala source code
- Prefer creating separate Scala source files for each class/object/trait (except, of course, for sealed traits)

### Testing

- Testing with default settings is required when pushing changes:

```sh
sbt library/test
sbt interpolation/test

./scripts/run_tests.sh h2
```

- Testing with H2/HSQLDB/MySQL/PostgreSQL (latest versions) is required before release:

```sh
./scripts/run_all_tests.sh
```

See the required settings here:

scalikejdbc-library/src/test/resources/*.properties

### Release

#### Required

- Sonatype release account

How to add a new Sonatype account for release maintainers:

https://issues.sonatype.org/browse/OSSRH-14350

If you're already allowed to release ScalikeJDBC libs, use your own Sonatype account:

- $HOME/.sbt/1.0/sonatype.sbt

``` scala
credentials += Credentials("Sonatype Central", "central.sonatype.com", "xxx", "yyy")
```

#### Operations

The release manager is @seratch. Currently, all the following operations should be done by @seratch.

- Create release note under notes.
- Fix _version in build.sbt
- Run all the `./scripts/release_*.sh` and close/publish on the sonatype console
- Post to [ScalikeJDBC Users Group](https://groups.google.com/forum/#!forum/scalikejdbc-users-group)
- Twitter account: @scalikejdbc

