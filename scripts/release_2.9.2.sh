#!/bin/sh

cd `dirname $0`/..

sbt \
  "g scct-removal" \
  "reload" \
  "project library" ++2.9.2 publish \
  "project config" ++2.9.2 publish \
  "project test" ++2.9.2 publish \
  "project mapper-generator-core" ++2.9.2 publish \
  "project mapper-generator" ++2.9.2 publish 

sbt "g scct-restoration"
