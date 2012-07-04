#!/bin/sh -x
if [ $# -ne 1 ]; then
  echo "Please specify version"
  exit 1
fi
curl -X POST http://ls.implicit.ly/api/1/libraries -d 'user=seratch&repo=scalikejdbc&version='$1
curl -X POST http://ls.implicit.ly/api/1/libraries -d 'user=seratch&repo=scalikejdbc-mapper-generator&version='$1
curl -X POST http://ls.implicit.ly/api/1/libraries -d 'user=seratch&repo=scalikejdbc-play-plugin&version='$1

