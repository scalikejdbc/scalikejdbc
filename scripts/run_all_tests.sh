#!/bin/sh

cd `dirname $0`
./run_tests.sh hsqldb
./run_tests.sh derby
./run_tests.sh mysql
./run_tests.sh postgresql
./run_tests.sh sqlite


