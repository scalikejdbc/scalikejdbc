#!/bin/bash

rm scalikejdbc-core/src/test/resources/jdbc.properties &&
cp -p scalikejdbc-core/src/test/resources/jdbc_$SCALIKEJDBC_DATABASE.properties scalikejdbc-core/src/test/resources/jdbc.properties &&

if [[ ${TRAVIS_SCALA_VERSION} = "scripted-test" ]]; then
  cd scalikejdbc-mapper-generator/src/sbt-test/scalikejdbc-mapper-generator/ &&
  cp $SCALIKEJDBC_DATABASE.properties gen/test.properties &&
  cp $SCALIKEJDBC_DATABASE.properties twenty-three/test.properties &&
  cd $TRAVIS_BUILD_DIR &&
  git add . --all &&
  sbt '++ 2.11.6' root211/publishLocal '++ 2.10.5' publishLocal checkScalariform &&
  sbt -J-XX:+CMSClassUnloadingEnabled -J-Xmx512M -J-Xms512M mapper-generator/scripted
else
  git add . --all &&
  sbt ++${TRAVIS_SCALA_VERSION} "project root211" test:compile checkScalariform testSequential
fi

