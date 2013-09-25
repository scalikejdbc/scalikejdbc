#!/bin/sh

cd `dirname $0`/..

sbt 'set scalaVersion := "2.10.0"' \
  "project library" 'set scalaVersion := "2.10.0"' publish-signed \
  "project config" 'set scalaVersion := "2.10.0"' publish-signed \
  "project interpolation-core" 'set scalaVersion := "2.10.0"' publish-signed \
  "project interpolation-macro" 'set scalaVersion := "2.10.0"' publish-signed \
  "project interpolation" 'set scalaVersion := "2.10.0"' publish-signed \
  "project mapper-generator-core" 'set scalaVersion := "2.10.0"' publish-signed \
  "project play-plugin" 'set scalaVersion := "2.10.0"' publish-signed \
  "project play-fixture-plugin" 'set scalaVersion := "2.10.0"' publish-signed \
  "project test" 'set scalaVersion := "2.10.0"' publish-signed 

