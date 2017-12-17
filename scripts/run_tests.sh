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
  ++2.10.6 \
  clean \
  core/test \
  joda-time/test \
  interpolation/test \
  syntax-support-macro/test \
  config/test \
  streams/test \
  test/test > logs/test_stdout.log

cp -p scalikejdbc-core/src/test/resources/jdbc_hsqldb.properties scalikejdbc-core/src/test/resources/jdbc.properties

grep "31merror"                 logs/test_stdout.log
grep -A 5 "FAILED"              logs/test_stdout.log
grep -A 5 "Error during tests:" logs/test_stdout.log
grep -A 5 "Failed tests:"       logs/test_stdout.log

