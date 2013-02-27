#!/bin/sh
rm -f ./sbt
wget https://raw.github.com/paulp/sbt-extras/master/sbt &&
chmod u+x ./sbt &&
./sbt -sbt-version 0.12.1 -mem 1024 \
  clean \
  ++2.9.2  test \
  ++2.10.0 test \
  ++2.9.2  config/test \
  ++2.10.0 interpolation/test \
  ++2.9.1  play-plugin/test \
  ++2.10.0 play-plugin/test \
  ++2.9.2  "project test" test \
  ++2.10.0 "project test" test 

