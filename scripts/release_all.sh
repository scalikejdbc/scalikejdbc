#!/bin/sh

cd `dirname $0`/..
rm -rf */target

sbt \
  clean \
  "set sonatypeProfileName := \"org.scalikejdbc\"" \
  SetScala212 \
  publishSigned \
  SetScala3 \
  publishSigned \
  'project root213' \
  SetScala213 \
  publishSigned \
  sonatypeBundleRelease
