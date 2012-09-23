#!/bin/sh
rm project/gpg.sbt
rm -f ./sbt
wget https://raw.github.com/paulp/sbt-extras/master/sbt &&
chmod u+x ./sbt &&
./sbt -sbt-version 0.12.0 -mem 512 \
  "project library" test \
  "project mapper-generator" test scripted \
  "project play-plugin" test 

