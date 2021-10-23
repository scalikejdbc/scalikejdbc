#!/bin/bash

set -eux

sbt -v \
  '++ 2.12.15! -v' \
  "test" \
  mimaReportBinaryIssues \
  checkScalariform \
  '++ 2.13.6! -v' \
  "project root213" \
  "test" \
  mimaReportBinaryIssues
