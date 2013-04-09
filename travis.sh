#!/bin/sh
rm -f ./sbt
wget https://raw.github.com/paulp/sbt-extras/master/sbt &&
chmod u+x ./sbt &&
./sbt -sbt-version 0.12.2 \
  clean \
  ++2.9.2  test \
  ++2.10.0 test \
  ++2.10.0 interpolation/test

