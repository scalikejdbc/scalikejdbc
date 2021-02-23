#!/bin/bash

set -eux

sbt -v \
  '++ 2.12.13! -v' \
  "test" \
  checkScalariform \
  '++ 2.13.5! -v' \
  "project root213" \
  "test"
