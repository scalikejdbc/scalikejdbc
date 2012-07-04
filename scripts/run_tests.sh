#!/bin/sh -x
if [ $# -ne 1 ]; then
  echo "Please specify db name (i.e. postgresql)"
  exit 1
fi
cd `dirname $0`
cd ..
cp -p scalikejdbc/src/test/resources/jdbc_$1.properties scalikejdbc/src/test/resources/jdbc.properties
sbt test
cp -p scalikejdbc/src/test/resources/jdbc_hsqldb.properties scalikejdbc/src/test/resources/jdbc.properties

