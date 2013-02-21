#!/bin/sh

cd `dirname $0`/..

sbt \
  "project library" \
  "set scalaVersion := \"2.10.0\"" \
  "set scalaBinaryVersion := \"2.10\"" \
  publish \
  "project config" \
  "set scalaVersion := \"2.10.0\"" \
  "set scalaBinaryVersion := \"2.10\"" \
  publish \
  "project interpolation" \
  "set scalaVersion := \"2.10.0\"" \
  "set scalaBinaryVersion := \"2.10\"" \
  publish \
  "project play-plugin" \
  "set scalaVersion := \"2.10.0\"" \
  "set scalaBinaryVersion := \"2.10\"" \
  publish \
  "project test" \
  "set scalaVersion := \"2.10.0\"" \
  "set scalaBinaryVersion := \"2.10\"" \
  publish 

