#!/bin/sh

cd `dirname $0`/..

sbt 'set scalaVersion := "2.9.2"' 'set scalaBinaryVersion := "2.9.2"' \
  "project library" 'set scalaVersion := "2.9.2"' 'set scalaBinaryVersion := "2.9.2"' publishSigned \
  "project config" 'set scalaVersion := "2.9.2"' 'set scalaBinaryVersion := "2.9.2"' publishSigned \
  "project test" 'set scalaVersion := "2.9.2"' 'set scalaBinaryVersion := "2.9.2"' publishSigned \
  "project mapper-generator-core" 'set scalaVersion := "2.9.2"' 'set scalaBinaryVersion := "2.9.2"' publishSigned 

