#!/bin/sh

cd `dirname $0`/..
sbt "project library" 'set scalaVersion := "2.9.3"' 'set scalaBinaryVersion := "2.9.3"' publishSigned \
  "project config" 'set scalaVersion := "2.9.3"' 'set scalaBinaryVersion := "2.9.3"' publishSigned \
  "project test" 'set scalaVersion := "2.9.3"' 'set scalaBinaryVersion := "2.9.3"' publishSigned \
  "project mapper-generator-core" 'set scalaVersion := "2.9.3"' 'set scalaBinaryVersion := "2.9.3"' publishSigned 

