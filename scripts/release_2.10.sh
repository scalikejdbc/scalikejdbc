#!/bin/sh

cd `dirname $0`/..

sbt 'set scalaVersion := "2.10.3"' \
  "project library" 'set scalaVersion := "2.10.3"' publishSigned \
  "project config" 'set scalaVersion := "2.10.3"' publishSigned \
  "project interpolation-core" 'set scalaVersion := "2.10.3"' publishSigned \
  "project interpolation-macro" 'set scalaVersion := "2.10.3"' publishSigned \
  "project interpolation" 'set scalaVersion := "2.10.3"' publishSigned \
  "project mapper-generator-core" 'set scalaVersion := "2.10.3"' publishSigned \
  "project play-plugin" 'set scalaVersion := "2.10.3"' publishSigned \
  "project play-fixture-plugin" 'set scalaVersion := "2.10.3"' publishSigned \
  "project test" 'set scalaVersion := "2.10.3"' publishSigned 

