#!/bin/bash

set -eux

sbt -v publishLocal

sbt -v \
  -J-Xmx512M \
  -J-Xms512M \
  mapper-generator2_12/scripted \
  mapper-generator3/scripted
