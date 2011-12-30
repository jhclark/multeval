#!/usr/bin/env bash
set -xeo pipefail
scriptDir=$(dirname $0)
source $scriptDir/constants

dist=multeval-$version
distDir=$scriptDir/dist
mkdir -p $distDir/$dist
mkdir -p $distDir/$dist/lib

cd $scriptDir
ant
cp -r multeval.sh \
    get_deps.sh \
    constants \
    LICENSE.txt \
    README.md \
    CHANGELOG \
    multeval-$version.jar \
    example \
    $distDir/$dist
cp -r $scriptDir/lib/guava-r09.jar \
    $scriptDir/lib/tercom-0.7.26.jar \
    $scriptDir/lib/jannopts.jar \
    $distDir/$dist/lib

cd $distDir
tar -cvzf $dist.tgz $dist
