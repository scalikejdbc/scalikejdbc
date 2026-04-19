#!/bin/sh
if [ $# -ne 1 ]; then
  echo "Please specify db name (i.e. postgresql)"
  exit 1
fi
cd `dirname $0`
cd ..
cp -p scalikejdbc-core/src/test/resources/jdbc_$1.properties scalikejdbc-core/src/test/resources/jdbc.properties

mkdir -p logs

sbt \
  clean \
  core3/test \
  joda-time3/test \
  interpolation3/test \
  syntax-support-macro3/test \
  config3/test \
  streams3/test \
  test3/test > logs/test_stdout.log

cp -p scalikejdbc-core/src/test/resources/jdbc_hsqldb.properties scalikejdbc-core/src/test/resources/jdbc.properties

grep "31merror"                 logs/test_stdout.log
grep -A 5 "FAILED"              logs/test_stdout.log
grep -A 5 "Error during tests:" logs/test_stdout.log
grep -A 5 "Failed tests:"       logs/test_stdout.log

