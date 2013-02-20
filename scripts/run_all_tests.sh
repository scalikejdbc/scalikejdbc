#!/bin/sh

cd `dirname $0`

echo
echo "-------------------"
echo " HSQLDB"
echo "-------------------"
echo 
./run_tests.sh hsqldb

echo
echo "-------------------"
echo " H2"
echo "-------------------"
echo 
./run_tests.sh h2

echo
echo "-------------------"
echo " MySQL"
echo "-------------------"
echo 
./run_tests.sh mysql

echo
echo "-------------------"
echo " PostgreSQL"
echo "-------------------"
echo 
./run_tests.sh postgresql

sbt "project library" \
    "set scalaVersion := \"2.9.2\"" \
    "set scalaBinaryVersion := \"2.9.2\"" \
    "project mapper-generator" test \
    "project play-plugin" test \
    "project config" test \
    "project test" test \
    "project library" \
    "set scalaVersion := \"2.10.0\"" \
    "set scalaBinaryVersion := \"2.10\"" \
    "project interpolation" test

