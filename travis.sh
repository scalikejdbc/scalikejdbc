#!/bin/bash

if [[ ${TRAVIS_SCALA_VERSION} = "2.10.3" ]]; then
  sbt ++${TRAVIS_SCALA_VERSION} test
else
  sbt ++${TRAVIS_SCALA_VERSION} root211/test
fi

