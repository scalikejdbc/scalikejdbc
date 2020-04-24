#!/bin/sh

cd `dirname $0`/..
rm -rf */target

sbt 'set scalaVersion := "2.13.2"' \
  clean \
  "project core" 'set scalaVersion := "2.13.2"' publishSigned \
  "project config" 'set scalaVersion := "2.13.2"' publishSigned \
  "project interpolation-macro" 'set scalaVersion := "2.13.2"' publishSigned \
  "project interpolation" 'set scalaVersion := "2.13.2"' publishSigned \
  "project library" 'set scalaVersion := "2.13.2"' publishSigned \
  "project joda-time" 'set scalaVersion := "2.13.2"' publishSigned \
  "project streams" 'set scalaVersion := "2.13.2"' publishSigned \
  "project syntax-support-macro" 'set scalaVersion := "2.13.2"' publishSigned \
  "project test" 'set scalaVersion := "2.13.2"' publishSigned \
  "project mapper-generator-core" 'set scalaVersion := "2.13.2"' publishSigned \
  'set scalaVersion := "2.12.11"' \
  clean \
  "project core" 'set scalaVersion := "2.12.11"' publishSigned \
  "project config" 'set scalaVersion := "2.12.11"' publishSigned \
  "project interpolation-macro" 'set scalaVersion := "2.12.11"' publishSigned \
  "project interpolation" 'set scalaVersion := "2.12.11"' publishSigned \
  "project library" 'set scalaVersion := "2.12.11"' publishSigned \
  "project joda-time" 'set scalaVersion := "2.12.11"' publishSigned \
  "project streams" 'set scalaVersion := "2.12.11"' publishSigned \
  "project syntax-support-macro" 'set scalaVersion := "2.12.11"' publishSigned \
  "project test" 'set scalaVersion := "2.12.11"' publishSigned \
  "project mapper-generator-core" 'set scalaVersion := "2.12.11"' publishSigned \
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

