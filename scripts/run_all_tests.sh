#!/bin/sh

cd `dirname $0`

echo
echo "-------------------"
echo " HSQLDB"
echo "-------------------"
echo 
./run_tests.sh hsqldb
./run_interpolation_tests.sh hsqldb

echo
echo "-------------------"
echo " H2"
echo "-------------------"
echo 
./run_tests.sh h2
./run_interpolation_tests.sh h2

echo
echo "-------------------"
echo " MySQL"
echo "-------------------"
echo 
./run_tests.sh mysql
./run_interpolation_tests.sh mysql

echo
echo "-------------------"
echo " PostgreSQL"
echo "-------------------"
echo 
./run_tests.sh postgresql
./run_interpolation_tests.sh postgresql

