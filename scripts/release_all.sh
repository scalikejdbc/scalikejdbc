#!/bin/sh

cd `dirname $0`/..
rm -rf */target

sbt 'set scalaVersion := "2.13.4"' \
  clean \
  "project core" 'set scalaVersion := "2.13.4"' publishSigned \
  "project config" 'set scalaVersion := "2.13.4"' publishSigned \
  "project interpolation-macro" 'set scalaVersion := "2.13.4"' publishSigned \
  "project interpolation" 'set scalaVersion := "2.13.4"' publishSigned \
  "project library" 'set scalaVersion := "2.13.4"' publishSigned \
  "project joda-time" 'set scalaVersion := "2.13.4"' publishSigned \
  "project streams" 'set scalaVersion := "2.13.4"' publishSigned \
  "project syntax-support-macro" 'set scalaVersion := "2.13.4"' publishSigned \
  "project test" 'set scalaVersion := "2.13.4"' publishSigned \
  "project mapper-generator-core" 'set scalaVersion := "2.13.4"' publishSigned \
  'set scalaVersion := "2.12.13"' \
  clean \
  "project core" 'set scalaVersion := "2.12.13"' publishSigned \
  "project config" 'set scalaVersion := "2.12.13"' publishSigned \
  "project interpolation-macro" 'set scalaVersion := "2.12.13"' publishSigned \
  "project interpolation" 'set scalaVersion := "2.12.13"' publishSigned \
  "project library" 'set scalaVersion := "2.12.13"' publishSigned \
  "project joda-time" 'set scalaVersion := "2.12.13"' publishSigned \
  "project streams" 'set scalaVersion := "2.12.13"' publishSigned \
  "project syntax-support-macro" 'set scalaVersion := "2.12.13"' publishSigned \
  "project test" 'set scalaVersion := "2.12.13"' publishSigned \
  "project mapper-generator-core" 'set scalaVersion := "2.12.13"' publishSigned

`dirname $0`/release_sbt_plugins.sh

