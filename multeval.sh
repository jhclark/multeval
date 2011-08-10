#!/usr/bin/env bash
set -euo pipefail
scriptDir=$(dirname $0)
source $scriptDir/constants

$scriptDir/get_deps.sh

# Specify -Dfile.encoding so that the meteor paraphrase tables load properly
java -Dfile.encoding=UTF8 -XX:+UseCompressedOops -Xmx2g \
    -cp $terJar:$meteorJar:$scriptDir/multeval-${version}.jar \
    multeval.MultEval $@
