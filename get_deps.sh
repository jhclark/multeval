#!/usr/bin/env bash
set -eo pipefail

version=1.2
libDir=lib
meteorPath=$libDir/meteor-$version
meteorUrl=http://www.cs.cmu.edu/~alavie/METEOR/download/meteor-${version}.tgz
if [ ! -e $meteorPath ]; then
    cd $libDir
    echo "Existing METEOR installation not found. Downloading and installing METEOR 1.2..."
    curl -L $meteorUrl | tar -xvz
else
    echo "Found existing METEOR installation at $meteorPath"
fi
