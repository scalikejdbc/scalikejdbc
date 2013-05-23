#!/bin/sh 
if [ $# -ne 1 ]; then
  echo "Please specify db name (i.e. postgresql)"
  exit 1
fi
cd `dirname $0`
cd ..
cp -p scalikejdbc-library/src/test/resources/jdbc_$1.properties scalikejdbc-library/src/test/resources/jdbc.properties

sbt \
  ++2.9.3 \
  clean \
  library/test \
  ++2.10.1 \
  clean \
  library/test \
  interpolation-core/test \
  interpolation/test > logs/test_stdout.log

cp -p scalikejdbc-library/src/test/resources/jdbc_hsqldb.properties scalikejdbc-library/src/test/resources/jdbc.properties

grep -A 5 "Failed tests:" logs/test_stdout.log

