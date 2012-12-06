#!/bin/sh

cd `dirname $0`

sbt +publish \
  "project mapper-generator" publish \
  "project play-plugin" +publish

sbt_0.11.3 "project mapper-generator" publish

./scripts/publish_2.10.RC.sh library 2.10.0-RC1
./scripts/publish_2.10.RC.sh library 2.10.0-RC2
./scripts/publish_2.10.RC.sh library 2.10.0-RC3

#./scripts/publish_2.10.RC.sh interpolation 2.10.0-RC3
#./scripts/publish_2.10.RC.sh play-plugin 2.10.0-RC1

