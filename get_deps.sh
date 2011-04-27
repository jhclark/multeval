#!/usr/bin/env bash
set -eo pipefail

version=1.2
libDir=lib
meteorPath=$libDir/meteor-$version
meteorUrl=http://www.cs.cmu.edu/~alavie/METEOR/download/meteor-${version}.tgz
if [ ! -e $meteorPath ]; then
    cd $libDir
    curl -L $meteorUrl | tar -xvz
else
    echo "Found existing METEOR installation at $meteorPath"
fi
