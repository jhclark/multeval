#!/usr/bin/env bash
set -eo pipefail

version=0.1
scriptDir=$(dirname $0)
$scriptDir/get_deps.sh
java -XX:UseCompressedOops -Xmx2g -cp $scriptDir/lib/tercom-0.7.26.jar:$scriptDir/lib/meteor-1.2/meteor-1.2.jar:$scriptDir/multeval-${version}.jar multeval.MultEval $@
