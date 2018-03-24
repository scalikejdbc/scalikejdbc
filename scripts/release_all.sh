#!/bin/sh

cd `dirname $0`/..
rm -rf */target

sbt 'set scalaVersion := "2.12.5"' \
  clean \
  "project core" 'set scalaVersion := "2.12.5"' publishSigned \
  "project config" 'set scalaVersion := "2.12.5"' publishSigned \
  "project interpolation-macro" 'set scalaVersion := "2.12.5"' publishSigned \
  "project interpolation" 'set scalaVersion := "2.12.5"' publishSigned \
  "project library" 'set scalaVersion := "2.12.5"' publishSigned \
  "project joda-time" 'set scalaVersion := "2.12.5"' publishSigned \
  "project streams" 'set scalaVersion := "2.12.5"' publishSigned \
  "project syntax-support-macro" 'set scalaVersion := "2.12.5"' publishSigned \
  "project test" 'set scalaVersion := "2.12.5"' publishSigned \
  "project mapper-generator-core" 'set scalaVersion := "2.12.5"' publishSigned \
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
  "project mapper-generator-core" 'set scalaVersion := "2.11.12"' publishSigned \
  'set scalaVersion := "2.10.6"' \
  clean \
  "project core" 'set scalaVersion := "2.10.6"' publishSigned \
  "project config" 'set scalaVersion := "2.10.6"' publishSigned \
  "project interpolation-macro" 'set scalaVersion := "2.10.6"' publishSigned \
  "project interpolation" 'set scalaVersion := "2.10.6"' publishSigned \
  "project library" 'set scalaVersion := "2.10.6"' publishSigned \
  "project joda-time" 'set scalaVersion := "2.10.6"' publishSigned \
  "project streams" 'set scalaVersion := "2.10.6"' publishSigned \
  "project syntax-support-macro" 'set scalaVersion := "2.10.6"' publishSigned \
  "project mapper-generator-core" 'set scalaVersion := "2.10.6"' publishSigned \
  "project test" 'set scalaVersion := "2.10.6"' publishSigned
