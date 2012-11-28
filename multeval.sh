#!/usr/bin/env bash
set -euo pipefail
scriptDir=$(dirname $0)
source $scriptDir/constants

$scriptDir/get_deps.sh

heap="-Xmx2g" # start with meteor heap size
for arg in "$@"; do
    if [[ "$arg" == "--metrics" ]]; then
        # We might not be using METEOR, so don't request any additional heap
        heap=""
    fi
    # TODO: Check if this follows --metric
    if [[ "$arg" == "meteor" ]]; then
        # We're using meteor after all, use the big heap
        heap="-Xmx2g"
    fi
done

# Specify -Dfile.encoding so that the meteor paraphrase tables load properly
java -Dfile.encoding=UTF8 -XX:+UseCompressedOops $heap \
    -cp $terJar:$meteorJar:$scriptDir/multeval-${version}.jar \
    multeval.MultEval "$@"
