#!/bin/sh

if [ $# -lt 2 ]; then
  echo "usage: $0 project, version"
  exit 1
fi

project=$1
version=$2

sbt "project ${project}" \
  "set scalaBinaryVersion := \"${version}\"" \
  "set scalaVersion := \"${version}\"" \
  publish

