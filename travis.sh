#!/bin/bash

if [[ ${TRAVIS_SCALA_VERSION} = "2.10.4" ]]; then
  sbt ++${TRAVIS_SCALA_VERSION} test
elif [[ ${TRAVIS_SCALA_VERSION} = "scripted-test" ]]; then
  sbt '++ 2.11.2' root211/publishLocal '++ 2.10.4' publishLocal mapper-generator/scripted
elif [[ ${TRAVIS_SCALA_VERSION} = "2.11.2" ]]; then
  sbt ++${TRAVIS_SCALA_VERSION} root211/test
else
  exit -1
fi

