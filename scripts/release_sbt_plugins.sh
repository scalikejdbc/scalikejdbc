#!/bin/sh

cd `dirname $0`/..
rm -rf */target

sbt clean "project mapper-generator" '^publishSigned'

