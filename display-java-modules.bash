#!/bin/bash
set -eu -o pipefail

echo "Display Java modules used to create a custom JRE"

# Need: build.gradle: plugin: "id 'java-library-distribution'"
./gradlew installDist >/dev/null

DEPS=$(jdeps --multi-release 9 --list-deps build/install/ids-backend/lib/*)$'\n'
DEPS+=$(jdeps --list-deps build/libs/ids-backend-0.0.0.jar)
UNIQ_DEPS=$(echo "$DEPS" | grep -v "JDK removed internal API.*" | sed "s|/|\n|" | grep -v "sun.rmi.registry" | sort | uniq)

echo $UNIQ_DEPS | sed "s| |,|g"
