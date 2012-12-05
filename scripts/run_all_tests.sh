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


