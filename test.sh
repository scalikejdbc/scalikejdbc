#!/bin/bash

set -eux

sbt -v \
  '++ 2.12.12! -v' \
  "test" \
  checkScalariform \
  '++ 2.13.3! -v' \
  "project root213" \
  "test"
