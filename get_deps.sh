#!/usr/bin/env bash
set -euo pipefail
scriptDir=$(dirname $0)
source $scriptDir/constants

if [ ! -e $meteorPath ]; then
    mkdir -p $libDir
    cd $libDir
    echo >&2 "Existing METEOR installation not found. Performing first-run setup: Downloading and installing METEOR ${meteor_version}..."
    curl -L $meteorUrl | tar -xz
else
    echo >&2 "Found existing METEOR installation at $meteorPath"
fi
