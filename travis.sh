#!/bin/sh
rm -f ./sbt
wget https://raw.github.com/paulp/sbt-extras/master/sbt &&
chmod u+x ./sbt &&
./sbt -sbt-version 0.13.2 \
  ++2.10.3 \
  clean \
  library/test \
  interpolation-core/test \
  interpolation/test \
  config/test \
  test/test \
  play-plugin/test \
  play-fixture-plugin/test

