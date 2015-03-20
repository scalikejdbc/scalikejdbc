## ScalikeJDBC Contributers' Guide

### Issues

- Questions should be posted to [ScalikeJDBC Users Group](https://groups.google.com/forum/#!forum/scalikejdbc-users-group)
- Please describe about your issue in detail (verison, situation, examples)
- We may close your issue when we have no plan to take action right now. We appreciate your understanding.

### Pull Requests

- Pull requests basically should be sent toward "master" branch
- Compatibility always must be kept as far as possible 
- scalariform must be applied to all Scala source code
- Prefer creating scala source code for each class/object/trait (of course, except for sealed trait)
- ScalikeJDBC build checks binary compatibility by using [MiMa](https://github.com/typesafehub/migration-manager/wiki/Sbt-plugin). Breaking source code compatibility of existing APIs are not allowed. Adding new methods, fields or any other things are allowed. If you have binary imcompatible changes, please explicitly add exclusions to mimaProblemFilters in `project/MimaSettings.scala`.

#### Branches

##### master (the default branch)

- the latest stable version
- This branch must be able to build against Scala 2.10 and 2.11

##### 2.1.x

- the version 2.1 series maintainance branch
- Only security fixes and critical bug fixes in master branch will be backported
- This branch must be able to build against Scala 2.10 and 2.11

##### 2.0.x

- the version 2.0 series maintainance branch
- Only security fixes and critical bug fixes in master branch will be backported
- This branch must be able to build against Scala 2.10 and 2.11

##### 1.8.x

- This branch must be able to build against Scala 2.10 
- This branch must be compatible with ScalaTest 1.9
- Backport from master branch which doesn't work on Scala 2.9 can be merged
- Source code compatibility should be kept with 1.7.x

##### 1.7.x

- This branch must be able to build against Scala 2.9.1, 2.9.2, 2.9.3 and 2.10
- This branch must be compatible with ScalaTest 1.9
- Backport from master branch which doesn't work on Scala 2.9 can NOT be merged

#### Testing your pull request

All the pull requests should pass the Travis CI jobs before merging them.

https://travis-ci.org/scalikejdbc/scalikejdbc

Testing with default settings is required when push changes:

```sh
sbt library/test
sbt interpolation/test
```

If your change needs testing with MySQL/PostgreSQL

```sh
./scripts/run_tests.sh mysql
./scripts/run_tests.sh postgresql
```

See the required JDBC settings here.

scalikejdbc-core/src/test/resources/*.properties

