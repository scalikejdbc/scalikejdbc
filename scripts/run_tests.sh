#!/bin/sh 
if [ $# -ne 1 ]; then
  echo "Please specify db name (i.e. postgresql)"
  exit 1
fi
cd `dirname $0`
cd ..
cp -p scalikejdbc-library/src/test/resources/jdbc_$1.properties scalikejdbc-library/src/test/resources/jdbc.properties

sbt \
  ++2.9.2 \
  clean \
  library/test \
  ++2.10.2 \
  clean \
  interpolation-core/test \
  interpolation/test > logs/test_stdout.log

cp -p scalikejdbc-library/src/test/resources/jdbc_hsqldb.properties scalikejdbc-library/src/test/resources/jdbc.properties

grep "31merror"                 logs/test_stdout.log
grep -A 5 "FAILED"              logs/test_stdout.log
grep -A 5 "Error during tests:" logs/test_stdout.log
grep -A 5 "Failed tests:"       logs/test_stdout.log

