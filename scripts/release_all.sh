#!/bin/sh

cd `dirname $0`/..
rm -rf */target

sbt \
  clean \
  publishSigned \
  sonaRelease
