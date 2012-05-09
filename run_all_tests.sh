#!/bin/sh
cp -p src/test/resources/jdbc_$1.properties src/test/resources/jdbc.properties
sbt test
cp -p src/test/resources/jdbc_h2.properties src/test/resources/jdbc.properties

