#!/bin/sh

cd `dirname $0`/..
rm -rf */target

sbt clean "project mapper-generator" 'set scalaVersion := "2.10.4"' 'set scalaBinaryVersion := "2.10"' publishSigned

