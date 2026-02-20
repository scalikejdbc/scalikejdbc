#!/bin/bash

set -eux

sbt -v --server \
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

sbt -v --server \
  -J-Xmx512M \
  -J-Xms512M \
  "project mapper-generator" \
  scripted \
  "++ 3.x" \
  scripted
