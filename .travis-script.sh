#!/bin/bash
set -x
set -e

# Run standard Travis script
cd officefloor/bom
echo "Standard Travis script stage"
mvn test -B -e

# Ensure backwards compatibility for Eclipse
cd ../editor
echo "Backwards compatibility for Eclipse"
# Latest already tested by default
mvn clean install -q -B -e -P PHOTON.target
mvn clean install -q -B -e -P OXYGEN.target
