#!/bin/bash

set -eux

sbt -v \
  SetScala212 \
  "test" \
  SetScala213 \
  "project root213" \
  "test"
