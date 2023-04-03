#!/bin/sh 
if [ $# -ne 1 ]; then
  echo "Please specify db name (i.e. postgresql)"
  exit 1
fi
cd `dirname $0`
cd ..
cp -p scalikejdbc-core/src/test/resources/jdbc_$1.properties scalikejdbc-core/src/test/resources/jdbc.properties

sbt SetScala212 clean                        > logs/test_stdout.log
sbt SetScala212 interpolation-macro/compile >> logs/test_stdout.log
sbt SetScala212 interpolation/test          >> logs/test_stdout.log

cp -p scalikejdbc-core/src/test/resources/jdbc_hsqldb.properties scalikejdbc-core/src/test/resources/jdbc.properties

grep "31merror"                 logs/test_stdout.log
grep -A 5 "FAILED"              logs/test_stdout.log
grep -A 5 "Error during tests:" logs/test_stdout.log
grep -A 5 "Failed tests:"       logs/test_stdout.log


