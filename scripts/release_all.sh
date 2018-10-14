#!/bin/sh

cd `dirname $0`/..
rm -rf */target

sbt 'set scalaVersion := "2.13.0-M5"' \
  clean \
  "project core" 'set scalaVersion := "2.13.0-M5"' publishSigned \
  "project config" 'set scalaVersion := "2.13.0-M5"' publishSigned \
  "project interpolation-macro" 'set scalaVersion := "2.13.0-M5"' publishSigned \
  "project interpolation" 'set scalaVersion := "2.13.0-M5"' publishSigned \
  "project library" 'set scalaVersion := "2.13.0-M5"' publishSigned \
  "project joda-time" 'set scalaVersion := "2.13.0-M5"' publishSigned \
  "project streams" 'set scalaVersion := "2.13.0-M5"' publishSigned \
  "project syntax-support-macro" 'set scalaVersion := "2.13.0-M5"' publishSigned \
  "project test" 'set scalaVersion := "2.13.0-M5"' publishSigned \
  "project mapper-generator-core" 'set scalaVersion := "2.13.0-M5"' publishSigned \
  'set scalaVersion := "2.12.7"' \
  clean \
  "project core" 'set scalaVersion := "2.12.7"' publishSigned \
  "project config" 'set scalaVersion := "2.12.7"' publishSigned \
  "project interpolation-macro" 'set scalaVersion := "2.12.7"' publishSigned \
  "project interpolation" 'set scalaVersion := "2.12.7"' publishSigned \
  "project library" 'set scalaVersion := "2.12.7"' publishSigned \
  "project joda-time" 'set scalaVersion := "2.12.7"' publishSigned \
  "project streams" 'set scalaVersion := "2.12.7"' publishSigned \
  "project syntax-support-macro" 'set scalaVersion := "2.12.7"' publishSigned \
  "project test" 'set scalaVersion := "2.12.7"' publishSigned \
  "project mapper-generator-core" 'set scalaVersion := "2.12.7"' publishSigned \
  'set scalaVersion := "2.11.12"' \
  clean \
  "project core" 'set scalaVersion := "2.11.12"' publishSigned \
  "project config" 'set scalaVersion := "2.11.12"' publishSigned \
  "project interpolation-macro" 'set scalaVersion := "2.11.12"' publishSigned \
  "project interpolation" 'set scalaVersion := "2.11.12"' publishSigned \
  "project library" 'set scalaVersion := "2.11.12"' publishSigned \
  "project joda-time" 'set scalaVersion := "2.11.12"' publishSigned \
  "project streams" 'set scalaVersion := "2.11.12"' publishSigned \
  "project syntax-support-macro" 'set scalaVersion := "2.11.12"' publishSigned \
  "project test" 'set scalaVersion := "2.11.12"' publishSigned \
  "project mapper-generator-core" 'set scalaVersion := "2.11.12"' publishSigned


`dirname $0`/release_sbt_plugins.sh

