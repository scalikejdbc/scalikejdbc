#!/bin/sh

cd `dirname $0`/..
sbt "project mapper-generator" 'set scalaVersion := "2.10.2"' 'set scalaBinaryVersion := "2.10"' publishSigned

