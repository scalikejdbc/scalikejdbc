#!/bin/sh

cd `dirname $0`

echo
echo "-------------------"
echo " HSQLDB"
echo "-------------------"
echo "Also check logs/test_stdout.log"
echo 
./run_tests.sh hsqldb

echo
echo "-------------------"
echo " H2"
echo "-------------------"
echo "Also check logs/test_stdout.log"
echo 
./run_tests.sh h2

echo
echo "-------------------"
echo " MySQL"
echo "-------------------"
echo "Also check logs/test_stdout.log"
echo 
./run_tests.sh mysql

echo
echo "-------------------"
echo " PostgreSQL"
echo "-------------------"
echo "Also check logs/test_stdout.log"
echo 
./run_tests.sh postgresql

