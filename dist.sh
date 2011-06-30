#!/usr/bin/env bash
set -xeo pipefail
scriptDir=$(dirname $0)

dist=multeval-0.2
distDir=$scriptDir/dist
mkdir -p $distDir/$dist
mkdir -p $distDir/$dist/lib

cd $scriptDir
ant
cp -r multeval.sh \
    get_deps.sh \
    LICENSE.txt \
    README.md \
    multeval-0.1.jar \
    example \
    $distDir/$dist
cp -r $scriptDir/lib/guava-r09.jar \
    $scriptDir/lib/tercom-0.7.26.jar \
    $scriptDir/lib/jannopts.jar \
    $distDir/$dist/lib

cd $distDir
tar -cvzf $dist.tgz $dist
