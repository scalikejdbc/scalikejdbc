#!/bin/sh

cd `dirname $0`/..

sbt 'set scalaVersion := "2.9.1"' 'set scalaBinaryVersion := "2.9.1"' \
  "project library" 'set scalaVersion := "2.9.1"' 'set scalaBinaryVersion := "2.9.1"' publish-signed \
  "project config" 'set scalaVersion := "2.9.1"' 'set scalaBinaryVersion := "2.9.1"' publish-signed \
  "project test" 'set scalaVersion := "2.9.1"' 'set scalaBinaryVersion := "2.9.1"' publish-signed \
  "project mapper-generator-core" 'set scalaVersion := "2.9.1"' 'set scalaBinaryVersion := "2.9.1"' publish-signed \
  "project play-plugin" 'set scalaVersion := "2.9.1"' 'set scalaBinaryVersion := "2.9.1"' publish-signed

