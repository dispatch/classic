#!/bin/sh
# Run `mvn javadoc:jar` in all project sub-dirs 
# since it fails to work when run against the parent
find . -name pom.xml -mindepth 2 -execdir mvn javadoc:jar deploy \;