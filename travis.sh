#!/bin/bash

set -e

if [[ ${TRAVIS_SCALA_VERSION} = "scripted-test" ]]; then
  if [[ "${TEST_SBT_VERSION}" = "0.13.16" ]]; then
    sbt \
    '++ 2.12.4! -v' \
    root211/publishLocal \
    '++ 2.11.12! -v' \
    root211/publishLocal \
    '++ 2.10.6! -v' \
    "^^ $TEST_SBT_VERSION" \
    publishLocal \
    checkScalariform

    sbt \
    -J-XX:+CMSClassUnloadingEnabled \
    -J-Xmx512M \
    -J-Xms512M \
    "project mapper-generator" \
    "^^ $TEST_SBT_VERSION" \
    "scripted scalikejdbc-mapper-generator/no-database" \
    "scripted scalikejdbc-mapper-generator/twenty-three"
  else
    sbt \
    "^^ $TEST_SBT_VERSION" \
    '++ 2.12.4! -v' \
    publishLocal \
    '++ 2.11.12! -v' \
    root211/publishLocal \
    '++ 2.10.6! -v' \
    root211/publishLocal \
    checkScalariform

    sbt \
    -J-XX:+CMSClassUnloadingEnabled \
    -J-Xmx512M \
    -J-Xms512M \
    "project mapper-generator" \
    "^^ $TEST_SBT_VERSION" \
    scripted
  fi
else
  sbt "project root211" "++ ${TRAVIS_SCALA_VERSION}! -v" test:compile checkScalariform testSequential
fi

