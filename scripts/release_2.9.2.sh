#!/bin/sh

cd `dirname $0`/..

sbt \
  "project library" \
  "set scalaVersion := \"2.9.2\"" \
  "set scalaBinaryVersion := \"2.9.2\"" \
  publish \
  "project config" \
  "set scalaVersion := \"2.9.2\"" \
  "set scalaBinaryVersion := \"2.9.2\"" \
  publish \
  "project test" \
  "set scalaVersion := \"2.9.2\"" \
  "set scalaBinaryVersion := \"2.9.2\"" \
  publish \
  "project mapper-generator" \
  "set scalaVersion := \"2.9.2\"" \
  "set scalaBinaryVersion := \"2.9.2\"" \
  publish 

