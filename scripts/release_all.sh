#!/bin/sh

cd `dirname $0`/..
rm -rf */target

sbt \
  clean \
  "project mapper-generator" \
  "^publishSigned" \
  'project root213' \
  SetScala213 \
  publishSigned \
  SetScala212 \
  publishSigned \
  SetScala3 \
  publishSigned \
  sonatypeBundleRelease
