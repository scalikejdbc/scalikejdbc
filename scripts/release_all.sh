#!/bin/sh

cd `dirname $0`/..
rm -rf */target

sbt 'set scalaVersion := "2.12.2"' \
  clean \
  "project core" 'set scalaVersion := "2.12.2"' publishSigned \
  "project config" 'set scalaVersion := "2.12.2"' publishSigned \
  "project interpolation-macro" 'set scalaVersion := "2.12.2"' publishSigned \
  "project interpolation" 'set scalaVersion := "2.12.2"' publishSigned \
  "project library" 'set scalaVersion := "2.12.2"' publishSigned \
  "project streams" 'set scalaVersion := "2.12.2"' publishSigned \
  "project syntax-support-macro" 'set scalaVersion := "2.12.2"' publishSigned \
  "project test" 'set scalaVersion := "2.12.2"' publishSigned \
  "project mapper-generator-core" 'set scalaVersion := "2.12.2"' publishSigned \
  'set scalaVersion := "2.11.11"' \
  clean \
  "project core" 'set scalaVersion := "2.11.11"' publishSigned \
  "project config" 'set scalaVersion := "2.11.11"' publishSigned \
  "project interpolation-macro" 'set scalaVersion := "2.11.11"' publishSigned \
  "project interpolation" 'set scalaVersion := "2.11.11"' publishSigned \
  "project library" 'set scalaVersion := "2.11.11"' publishSigned \
  "project streams" 'set scalaVersion := "2.11.11"' publishSigned \
  "project syntax-support-macro" 'set scalaVersion := "2.11.11"' publishSigned \
  "project test" 'set scalaVersion := "2.11.11"' publishSigned \
  "project mapper-generator-core" 'set scalaVersion := "2.11.11"' publishSigned \
  'set scalaVersion := "2.10.6"' \
  clean \
  "project core" 'set scalaVersion := "2.10.6"' publishSigned \
  "project config" 'set scalaVersion := "2.10.6"' publishSigned \
  "project interpolation-macro" 'set scalaVersion := "2.10.6"' publishSigned \
  "project interpolation" 'set scalaVersion := "2.10.6"' publishSigned \
  "project library" 'set scalaVersion := "2.10.6"' publishSigned \
  "project streams" 'set scalaVersion := "2.10.6"' publishSigned \
  "project syntax-support-macro" 'set scalaVersion := "2.10.6"' publishSigned \
  "project mapper-generator-core" 'set scalaVersion := "2.10.6"' publishSigned \
  "project test" 'set scalaVersion := "2.10.6"' publishSigned \
  clean \
  "project mapper-generator" \
  'set scalaVersion := "2.10.6"' \
  'set scalaBinaryVersion := "2.10"' \
  publishSigned
