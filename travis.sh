#!/bin/bash

rm scalikejdbc-core/src/test/resources/jdbc.properties &&
cp -p scalikejdbc-core/src/test/resources/jdbc_$SCALIKEJDBC_DATABASE.properties scalikejdbc-core/src/test/resources/jdbc.properties &&

if [[ ${TRAVIS_SCALA_VERSION} = "scripted-test" ]]; then
  cd scalikejdbc-mapper-generator/src/sbt-test/scalikejdbc-mapper-generator/ &&
  cp $SCALIKEJDBC_DATABASE.properties gen/test.properties &&
  cp $SCALIKEJDBC_DATABASE.properties twenty-three/test.properties &&
  cd $TRAVIS_BUILD_DIR &&
  sbt '++ 2.11.4' root211/publishLocal '++ 2.10.4' publishLocal mapper-generator/scripted
else
  sbt ++${TRAVIS_SCALA_VERSION} "project root211" test:compile testSequential
fi

