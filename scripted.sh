#!/bin/bash

set -eux

sbt -v --server publishLocal

sbt -v --server \
  -J-Xmx512M \
  -J-Xms512M \
  mapper-generator2_12/scripted \
  mapper-generator3/scripted
