#!/bin/sh

cd `dirname $0`/..

sbt 'set scalaVersion := "2.9.1"' 'set scalaBinaryVersion := "2.9.1"' \
  "project library" 'set scalaVersion := "2.9.1"' 'set scalaBinaryVersion := "2.9.1"' publishSigned \
  "project config" 'set scalaVersion := "2.9.1"' 'set scalaBinaryVersion := "2.9.1"' publishSigned \
  "project test" 'set scalaVersion := "2.9.1"' 'set scalaBinaryVersion := "2.9.1"' publishSigned \
  "project mapper-generator-core" 'set scalaVersion := "2.9.1"' 'set scalaBinaryVersion := "2.9.1"' publishSigned \
  "project play-plugin" 'set scalaVersion := "2.9.1"' 'set scalaBinaryVersion := "2.9.1"' publishSigned

