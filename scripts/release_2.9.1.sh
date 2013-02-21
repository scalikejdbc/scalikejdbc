#!/bin/sh

cd `dirname $0`/..

sbt \
  "project library" \
  "set scalaVersion := \"2.9.1\"" \
  "set scalaBinaryVersion := \"2.9.1\"" \
  publish \
  "project config" \
  "set scalaVersion := \"2.9.1\"" \
  "set scalaBinaryVersion := \"2.9.1\"" \
  publish \
  "project play-plugin" \
  "set scalaVersion := \"2.9.1\"" \
  "set scalaBinaryVersion := \"2.9.1\"" \
  publish \
  "project test" \
  "set scalaVersion := \"2.9.1\"" \
  "set scalaBinaryVersion := \"2.9.1\"" \
  publish 

