#!/bin/bash

set -eux

sbt -v \
  SetScala212 \
  publishLocal \
  SetScala213 \
  "project root213" \
  publishLocal \
  SetScala3 \
  publishLocal

sbt -v \
  -J-XX:+CMSClassUnloadingEnabled \
  -J-Xmx512M \
  -J-Xms512M \
  "project mapper-generator" \
  scripted
