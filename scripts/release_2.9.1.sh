#!/bin/sh

cd `dirname $0`/..

sbt \
  "project library" ++2.9.1 publish \
  "project config" ++2.9.1 publish \
  "project play-plugin" ++2.9.1 publish \
  "project test" ++2.9.1 publish 

