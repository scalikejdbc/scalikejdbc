#!/bin/sh

cd `dirname $0`/..

sbt 'set scalaVersion := "2.9.2"' 'set scalaBinaryVersion := "2.9.2"' \
  "project library" 'set scalaVersion := "2.9.2"' 'set scalaBinaryVersion := "2.9.2"' publish-signed \
  "project config" 'set scalaVersion := "2.9.2"' 'set scalaBinaryVersion := "2.9.2"' publish-signed \
  "project test" 'set scalaVersion := "2.9.2"' 'set scalaBinaryVersion := "2.9.2"' publish-signed \
  "project mapper-generator-core" 'set scalaVersion := "2.9.2"' 'set scalaBinaryVersion := "2.9.2"' publish-signed 

