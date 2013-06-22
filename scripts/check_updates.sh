#!/bin/sh 

sbt \
  library/dependency-updates \
  config/dependency-updates \
  interpolation-core/dependency-updates \
  interpolation/dependency-updates \
  mapper-generator-core/dependency-updates \
  mapper-generator/dependency-updates \
  play-plugin/dependency-updates \
  play-fixture-plugin/dependency-updates \
  "project test" dependency-updates

