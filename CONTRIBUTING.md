## ScalikeJDBC Contributors' Guide

### Issues

- Questions should be posted to the [ScalikeJDBC Users Group](https://groups.google.com/forum/#!forum/scalikejdbc-users-group)
- Please describe your issue in detail (version, situation, examples)
- We may close your issue if we have no plan to take action on it. We appreciate your understanding.

### Pull Requests

- Pull requests should be sent to the "master" branch
- Source and binary compatibility must always be kept
- scalafmt must be applied to all Scala source code
- Prefer creating separate Scala source files for each class/object/trait (except, of course, for sealed traits)
- The ScalikeJDBC build checks for binary compatibility using the [mima](https://github.com/lightbend/mima). After a 2.x.0 release, binary compatibility must be maintained for subsequent 2.x series releases.

#### Branches

##### master (will be the next major/minor version, the default branch)

- Latest stable version
- Breaking source compatibility is not acceptable
- Changes that bring binary incompatibility with reasonable reasons are **allowed**
- Must build against Scala 2.12, 2.13 and 3
- Requires Java 8 or higher

##### 4.*.x

- Version 4.0 series maintenance branch
- Changes that bring binary/source incompatibility are not allowed
- Must build against Scala 2.12, 2.13 and 3
- Requires Java 8 or higher

##### 3.5.x

- Version 3.5 series maintenance branch
- Changes that bring binary/source incompatibility are not allowed
- Must build against Scala 2.12, and 2.13
- Requires Java 8 or higher

##### 3.4.x

- Version 3.4 series maintenance branch
- Changes that bring binary/source incompatibility are not allowed
- Must build against Scala 2.11, 2.12, and 2.13
- Requires Java 8 or higher

##### 3.3.x

- Version 3.3 series maintenance branch
- Changes that bring binary/source incompatibility are not allowed
- Must build against Scala 2.11, 2.12, and 2.13
- Requires Java 8 or higher

##### 3.2.x

- Version 3.2 series maintenance branch
- Changes that bring binary/source incompatibility are not allowed
- Must build against Scala 2.10, 2.11 and 2.12
- Requires Java 8 or higher

##### 3.1.x

- Version 3.1 series maintenance branch
- Changes that bring binary/source incompatibility are not allowed
- Must build against Scala 2.10, 2.11 and 2.12
- Requires Java 8 or higher

##### 3.0.x

- Version 3.0 series maintenance branch
- Changes that bring binary/source incompatibility are not allowed
- Must build against Scala 2.10, 2.11 and 2.12
- Requires Java 8 or higher

##### 2.5.x

- Version 2.5 series maintenance branch
- Changes that bring binary/source incompatibility are not allowed
- Must build against Scala 2.10, 2.11 and 2.12

##### 2.4.x

- Version 2.4 series maintenance branch
- Changes that bring binary/source incompatibility are not allowed
- Must build against Scala 2.10, 2.11 and 2.12

##### 2.3.x

- Version 2.3 series maintenance branch
- Only security fixes and critical bug fixes to the master branch will be backported
- Must build against Scala 2.10 and 2.11

##### 2.2.x

- Version 2.2 series maintenance branch
- Only security fixes and critical bug fixes to the master branch will be backported
- Must build against Scala 2.10 and 2.11

##### 2.1.x

- Version 2.1 series maintenance branch
- Only security fixes and critical bug fixes to the master branch will be backported
- Must build against Scala 2.10 and 2.11

##### 2.0.x

- Version 2.0 series maintenance branch
- Only security fixes and critical bug fixes to the master branch will be backported
- Must build against Scala 2.10 and 2.11

##### 1.8.x

- Must build against Scala 2.10 
- Must be compatible with ScalaTest 1.9
- Backports from the master branch which don't work on Scala 2.9 can be merged
- Source code compatibility should be kept with 1.7.x

##### 1.7.x

- Must build against Scala 2.9.1, 2.9.2, 2.9.3 and 2.10
- Must be compatible with ScalaTest 1.9
- Backports from the master branch which don't work on Scala 2.9 can NOT be merged

#### Testing your pull request

All pull requests should pass the CI jobs before they can be merged:

Testing with default settings is required when pushing changes:

```sh
sbt library/test
sbt interpolation/test
```

If your change needs testing with MySQL/PostgreSQL:

```sh
./scripts/run_tests.sh mysql
./scripts/run_tests.sh postgresql
```

See the required JDBC settings here:

scalikejdbc-core/src/test/resources/*.properties

