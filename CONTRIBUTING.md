## ScalikeJDBC Contributers' Guide

### Issues

- Questions should be posted to [ScalikeJDBC Users Group](https://groups.google.com/forum/#!forum/scalikejdbc-users-group)
- Please describe about your issue in detail (verison, situation, examples)

### Pull Requests

- Send pull requests toward "develop" or "feature/xxx" branches
- Compatibility always must be kept as far as possible
- scalariform must be applied to all Scala source code
- Prefer creating scala source code for each class/object/trait (of course, except for sealed trait)

#### Testing your pull request

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

scalikejdbc-library/src/test/resources/*.properties

