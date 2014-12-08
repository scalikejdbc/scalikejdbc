#!/bin/bash
if [[ "$SCALIKEJDBC_DATABASE" == "h2" ]]; then
  sbt coveralls
fi

