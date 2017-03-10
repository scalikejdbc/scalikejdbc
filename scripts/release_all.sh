#!/bin/sh

cd `dirname $0`/..
rm -rf */target

sbt 'set scalaVersion := "2.12.1"' \
  clean \
  "project core" 'set scalaVersion := "2.12.1"' publishSigned \
  "project config" 'set scalaVersion := "2.12.1"' publishSigned \
  "project interpolation-macro" 'set scalaVersion := "2.12.1"' publishSigned \
  "project interpolation" 'set scalaVersion := "2.12.1"' publishSigned \
  "project jsr310" 'set scalaVersion := "2.12.1"' publishSigned \
  "project library" 'set scalaVersion := "2.12.1"' publishSigned \
  "project syntax-support-macro" 'set scalaVersion := "2.12.1"' publishSigned \
  "project test" 'set scalaVersion := "2.12.1"' publishSigned \
  "project mapper-generator-core" 'set scalaVersion := "2.12.1"' publishSigned \
  'set scalaVersion := "2.11.8"' \
  clean \
  "project core" 'set scalaVersion := "2.11.8"' publishSigned \
  "project config" 'set scalaVersion := "2.11.8"' publishSigned \
  "project interpolation-macro" 'set scalaVersion := "2.11.8"' publishSigned \
  "project interpolation" 'set scalaVersion := "2.11.8"' publishSigned \
  "project jsr310" 'set scalaVersion := "2.11.8"' publishSigned \
  "project library" 'set scalaVersion := "2.11.8"' publishSigned \
  "project syntax-support-macro" 'set scalaVersion := "2.11.8"' publishSigned \
  "project test" 'set scalaVersion := "2.11.8"' publishSigned \
  "project mapper-generator-core" 'set scalaVersion := "2.11.8"' publishSigned \
  'set scalaVersion := "2.10.6"' \
  clean \
  "project core" 'set scalaVersion := "2.10.6"' publishSigned \
  "project config" 'set scalaVersion := "2.10.6"' publishSigned \
  "project interpolation-macro" 'set scalaVersion := "2.10.6"' publishSigned \
  "project interpolation" 'set scalaVersion := "2.10.6"' publishSigned \
  "project jsr310" 'set scalaVersion := "2.10.6"' publishSigned \
  "project library" 'set scalaVersion := "2.10.6"' publishSigned \
  "project syntax-support-macro" 'set scalaVersion := "2.10.6"' publishSigned \
  "project mapper-generator-core" 'set scalaVersion := "2.10.6"' publishSigned \
  "project test" 'set scalaVersion := "2.10.6"' publishSigned \
  clean \
  "project mapper-generator" \
  'set scalaVersion := "2.10.6"' \
  'set scalaBinaryVersion := "2.10"' \
  publishSigned
