#!/bin/sh

cd `dirname $0`/..

sbt \
  "project library" ++2.9.3 publish \
  "project config" ++2.9.3 publish \
  "project mapper-generator-core" ++2.9.3 publish \
  "project test" ++2.9.3 publish 

