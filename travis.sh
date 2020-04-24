#!/bin/bash

set -e

if [[ ${TRAVIS_SCALA_VERSION} = "scripted-test" ]]; then
  sbt \
  '++ 2.12.11! -v' \
  publishLocal \
  '++ 2.13.2! -v' \
  root213/publishLocal \
  checkScalariform

  sbt \
  -J-XX:+CMSClassUnloadingEnabled \
  -J-Xmx512M \
  -J-Xms512M \
  "project mapper-generator" \
  scripted
else
  sbt "project root213" "++ ${TRAVIS_SCALA_VERSION}! -v" test:compile checkScalariform testSequential
fi

