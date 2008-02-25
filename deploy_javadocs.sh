#!/bin/sh
# Run `mvn javadoc:jar` in all project sub-dirs 
# since it fails to work when run against the parent
find . -mindepth 2 -name pom.xml -execdir mvn javadoc:jar deploy \;
