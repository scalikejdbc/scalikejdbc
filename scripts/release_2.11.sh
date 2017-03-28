#!/bin/sh

cd `dirname $0`/..
rm -rf */target

sbt 'set scalaVersion := "2.11.9"' \
  clean \
  "project core" 'set scalaVersion := "2.11.9"' publishSigned \
  "project config" 'set scalaVersion := "2.11.9"' publishSigned \
  "project interpolation-macro" 'set scalaVersion := "2.11.9"' publishSigned \
  "project interpolation" 'set scalaVersion := "2.11.9"' publishSigned \
  "project library" 'set scalaVersion := "2.11.9"' publishSigned \
  "project streams" 'set scalaVersion := "2.11.9"' publishSigned \
  "project syntax-support-macro" 'set scalaVersion := "2.11.9"' publishSigned \
  "project test" 'set scalaVersion := "2.11.9"' publishSigned \
  "project mapper-generator-core" 'set scalaVersion := "2.11.9"' publishSigned 

