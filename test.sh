#!/bin/bash

set -eux

sbt -v \
  '++ 2.12.12! -v' \
  "test" \
  mimaReportBinaryIssues \
  checkScalariform \
  '++ 2.13.4! -v' \
  "project root213" \
  "test" \
  mimaReportBinaryIssues
