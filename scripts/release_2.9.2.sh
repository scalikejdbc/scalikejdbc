#!/bin/sh

cd `dirname $0`/..

sbt \
  "project library" 'set scalaVersion := "2.9.2"' publish \
  "project config" 'set scalaVersion := "2.9.2"' publish \
  "project test" 'set scalaVersion := "2.9.2"' publish \
  "project mapper-generator-core" 'set scalaVersion := "2.9.2"' publish 

