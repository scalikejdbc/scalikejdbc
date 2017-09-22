#!/bin/sh

cd `dirname $0`/..
rm -rf */target

sbt clean \
    'project mapper-generator' \
    '++2.10.6' '^^ 0.13.16' publishSigned \
    '++2.12.3' '^^ 1.0.2' publishSigned
