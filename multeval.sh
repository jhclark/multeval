#!/usr/bin/env bash
set -eo pipefail

version=0.1
scriptDir=$(dirname $0)
$scriptDir/get_deps.sh
java -Xmx2g -jar multeval-${version}.jar $@
