#!/bin/sh
rm project/gpg.sbt
rm -f ./sbt
wget https://raw.github.com/paulp/sbt-extras/master/sbt &&
chmod u+x ./sbt &&
./sbt -sbt-version 0.12.1 -mem 512 \
  "project library" test 
  #"project library" test \
  #"project play-plugin" test \ # StackOverFlowError
  #"project mapper-generator" test scripted # Failure

