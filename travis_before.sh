#!/bin/bash

if [[ ${TRAVIS_SCALA_VERSION} = "scripted-test" ]]; then
  cd scalikejdbc-mapper-generator/src/sbt-test/scalikejdbc-mapper-generator/ &&
  cp $SCALIKEJDBC_DATABASE.properties gen/test.properties &&
  cp $SCALIKEJDBC_DATABASE.properties twenty-three/test.properties &&
  cd $TRAVIS_BUILD_DIR
else
  rm scalikejdbc-core/src/test/resources/jdbc.properties &&
  cp -p scalikejdbc-core/src/test/resources/jdbc_$SCALIKEJDBC_DATABASE.properties scalikejdbc-core/src/test/resources/jdbc.properties
fi
