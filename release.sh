#!/bin/sh

cd `dirname $0`

sbt +publish \
  "project interpolation" publish \
  "project mapper-generator" publish \
  "project test" +publish \
  "project play-plugin" +publish

#sbt_0.11.3 "project mapper-generator" publish

