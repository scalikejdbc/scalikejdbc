#!/bin/sh -x
if [ $# -ne 1 ]; then
  echo "Please specify db name (i.e. postgresql)"
  exit 1
fi
cp -p src/test/resources/jdbc_$1.properties src/test/resources/jdbc.properties
sbt test
cp -p src/test/resources/jdbc_hsqldb.properties src/test/resources/jdbc.properties

