#!/bin/bash

set -eux

sbt -v \
  SetScala212 \
  publishLocal \
  "project root213" \
  SetScala3 \
  publishLocal \
  SetScala213 \
  publishLocal \
  "project mapper-generator" \
  "++ 3.x" \
  publishLocal

sbt -v \
  -J-XX:+CMSClassUnloadingEnabled \
  -J-Xmx512M \
  -J-Xms512M \
  "project mapper-generator" \
  scripted \
  "++ 3.x" \
  scripted
