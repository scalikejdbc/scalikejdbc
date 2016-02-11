#!/bin/sh

cd `dirname $0`/..
rm -rf */target

sbt 'set scalaVersion := "2.12.0-M3"' \
  clean \
  "project core" 'set scalaVersion := "2.12.0-M3"' publishSigned \
  "project config" 'set scalaVersion := "2.12.0-M3"' publishSigned \
  "project interpolation-macro" 'set scalaVersion := "2.12.0-M3"' publishSigned \
  "project interpolation" 'set scalaVersion := "2.12.0-M3"' publishSigned \
  "project library" 'set scalaVersion := "2.12.0-M3"' publishSigned \
  "project jsr310" 'set scalaVersion := "2.12.0-M3"' publishSigned \
  "project syntax-support-macro" 'set scalaVersion := "2.12.0-M3"' publishSigned \
  "project test" 'set scalaVersion := "2.12.0-M3"' publishSigned \
  "project mapper-generator-core" 'set scalaVersion := "2.12.0-M3"' publishSigned
