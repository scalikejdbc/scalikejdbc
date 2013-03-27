#!/bin/sh

cd `dirname $0`/..

sbt \
  "project library" ++2.10.1 publish \
  "project config" ++2.10.1 publish \
  "project interpolation-core" ++2.10.1 publish \
  "project interpolation-macro" ++2.10.1 publish \
  "project interpolation" ++2.10.1 publish \
  "project play-plugin" ++2.10.1 publish \
  "project test" ++2.10.1 publish 

