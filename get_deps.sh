#!/usr/bin/env bash
set -eo pipefail

version=1.2
scriptDir=$(dirname $0)
libDir=$scriptDir/lib
meteorPath=$libDir/meteor-$version
meteorUrl=http://www.cs.cmu.edu/~alavie/METEOR/download/meteor-${version}.tgz
if [ ! -e $meteorPath ]; then
    mkdir -p $libDir
    cd $libDir
    echo >&2 "Existing METEOR installation not found. Performing first-run setup: Downloading and installing METEOR 1.2..."
    curl -L $meteorUrl | tar -xz
else
    echo >&2 "Found existing METEOR installation at $meteorPath"
fi
