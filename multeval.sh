#!/usr/bin/env bash
set -eo pipefail

version=0.2
scriptDir=$(dirname $0)
$scriptDir/get_deps.sh

# Specify -Dfile.encoding so that the meteor paraphrase tables load properly
java -Dfile.encoding=UTF8 -XX:+UseCompressedOops -Xmx2g \
    -cp $scriptDir/lib/tercom-0.7.26.jar:$scriptDir/lib/meteor-1.2/meteor-1.2.jar:$scriptDir/multeval-${version}.jar \
    multeval.MultEval $@
