#!/bin/sh

cd `dirname $0`/..
rm -rf */target

sbt 'set scalaVersion := "2.13.0-M3"' \
  clean \
  "project core" 'set scalaVersion := "2.13.0-M3"' publishSigned \
  "project config" 'set scalaVersion := "2.13.0-M3"' publishSigned \
  "project interpolation-macro" 'set scalaVersion := "2.13.0-M3"' publishSigned \
  "project interpolation" 'set scalaVersion := "2.13.0-M3"' publishSigned \
  "project library" 'set scalaVersion := "2.13.0-M3"' publishSigned \
  "project joda-time" 'set scalaVersion := "2.13.0-M3"' publishSigned \
  "project streams" 'set scalaVersion := "2.13.0-M3"' publishSigned \
  "project syntax-support-macro" 'set scalaVersion := "2.13.0-M3"' publishSigned \
  "project test" 'set scalaVersion := "2.13.0-M3"' publishSigned \
  "project mapper-generator-core" 'set scalaVersion := "2.13.0-M3"' publishSigned \
  'set scalaVersion := "2.12.6"' \
  clean \
  "project core" 'set scalaVersion := "2.12.6"' publishSigned \
  "project config" 'set scalaVersion := "2.12.6"' publishSigned \
  "project interpolation-macro" 'set scalaVersion := "2.12.6"' publishSigned \
  "project interpolation" 'set scalaVersion := "2.12.6"' publishSigned \
  "project library" 'set scalaVersion := "2.12.6"' publishSigned \
  "project joda-time" 'set scalaVersion := "2.12.6"' publishSigned \
  "project streams" 'set scalaVersion := "2.12.6"' publishSigned \
  "project syntax-support-macro" 'set scalaVersion := "2.12.6"' publishSigned \
  "project test" 'set scalaVersion := "2.12.6"' publishSigned \
  "project mapper-generator-core" 'set scalaVersion := "2.12.6"' publishSigned \
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
