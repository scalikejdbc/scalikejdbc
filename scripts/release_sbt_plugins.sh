#!/bin/sh

cd `dirname $0`/..

sbt "g sbt012" 
sbt "project mapper-generator" 'set scalaVersion := "2.9.2"' 'set scalaBinaryVersion := "2.9.2"' publish

sbt "g sbt013" 
sbt "project mapper-generator" 'set scalaVersion := "2.10.2"' 'set scalaBinaryVersion := "2.10"' publishSigned

