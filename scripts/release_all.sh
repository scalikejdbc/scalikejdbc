#!/bin/sh

cd `dirname $0`/..
rm -rf */target

sbt 'set scalaVersion := "2.13.8"' \
  clean \
  "project core" 'set scalaVersion := "2.13.8"' publishSigned \
  "project config" 'set scalaVersion := "2.13.8"' publishSigned \
  "project interpolation-macro" 'set scalaVersion := "2.13.8"' publishSigned \
  "project interpolation" 'set scalaVersion := "2.13.8"' publishSigned \
  "project library" 'set scalaVersion := "2.13.8"' publishSigned \
  "project joda-time" 'set scalaVersion := "2.13.8"' publishSigned \
  "project streams" 'set scalaVersion := "2.13.8"' publishSigned \
  "project syntax-support-macro" 'set scalaVersion := "2.13.8"' publishSigned \
  "project test" 'set scalaVersion := "2.13.8"' publishSigned \
  "project mapper-generator-core" 'set scalaVersion := "2.13.8"' publishSigned \
  'set scalaVersion := "2.12.17"' \
  clean \
  "project core" 'set scalaVersion := "2.12.17"' publishSigned \
  "project config" 'set scalaVersion := "2.12.17"' publishSigned \
  "project interpolation-macro" 'set scalaVersion := "2.12.17"' publishSigned \
  "project interpolation" 'set scalaVersion := "2.12.17"' publishSigned \
  "project library" 'set scalaVersion := "2.12.17"' publishSigned \
  "project joda-time" 'set scalaVersion := "2.12.17"' publishSigned \
  "project streams" 'set scalaVersion := "2.12.17"' publishSigned \
  "project syntax-support-macro" 'set scalaVersion := "2.12.17"' publishSigned \
  "project test" 'set scalaVersion := "2.12.17"' publishSigned \
  "project mapper-generator-core" 'set scalaVersion := "2.12.17"' publishSigned

`dirname $0`/release_sbt_plugins.sh

