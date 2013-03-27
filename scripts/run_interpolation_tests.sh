#!/bin/sh 
if [ $# -ne 1 ]; then
  echo "Please specify db name (i.e. postgresql)"
  exit 1
fi
cd `dirname $0`
cd ..
cp -p scalikejdbc-library/src/test/resources/jdbc_$1.properties scalikejdbc-library/src/test/resources/jdbc.properties

sbt clean ++2.10.1 \
  interpolation-core/test \ 
  interpolation-macro/compile \ 
  interpolation/test

cp -p scalikejdbc-library/src/test/resources/jdbc_hsqldb.properties scalikejdbc-library/src/test/resources/jdbc.properties

