#!/bin/sh

cd `dirname $0`/..
rm -rf */target

sbt \
  clean \
  SetScala212 \
  publishSigned \
  'project root213' \
  SetScala3 \
  publishSigned \
  SetScala213 \
  publishSigned \
  "project mapper-generator" \
  "++ 3.6.3!" \
  publishSigned \
  sonaRelease
