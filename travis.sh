#!/bin/bash

if [[ ${TRAVIS_SCALA_VERSION} = "2.10.4" ]]; then
  sbt ++${TRAVIS_SCALA_VERSION} test
elif [[ ${TRAVIS_SCALA_VERSION} = "2.11.0" ]]; then
  sbt ++${TRAVIS_SCALA_VERSION} root211/test
else
  exit -1
fi

