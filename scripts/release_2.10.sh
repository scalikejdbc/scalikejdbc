#!/bin/sh

cd `dirname $0`/..

sbt \
  "project library" 'set scalaVersion := "2.10.0"' publish \
  "project config" 'set scalaVersion := "2.10.0"' publish \
  "project interpolation-core" 'set scalaVersion := "2.10.0"' publish \
  "project interpolation-macro" 'set scalaVersion := "2.10.0"' publish \
  "project interpolation" 'set scalaVersion := "2.10.0"' publish \
  "project mapper-generator-core" 'set scalaVersion := "2.10.0"' publish \
  "project play-plugin" 'set scalaVersion := "2.10.0"' publish \
  "project play-fixture-plugin" 'set scalaVersion := "2.10.0"' publish \
  "project test" 'set scalaVersion := "2.10.0"' publish 

