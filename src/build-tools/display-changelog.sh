#!/bin/sh

set -eu

DIR=$(dirname $0)

if [ "$#" = "1" ]; then
    export JRELEASER_PROJECT_VERSION="$1"
else
    git fetch --tags
    LAST_VERSION=$(git tag -l | sort -V | tail -n 1 | tr -d 'v')
    export JRELEASER_PROJECT_VERSION=$(git tag -l | java --source 17 ${DIR}/VersionBumper.java ${LAST_VERSION})
fi
export JRELEASER_GITHUB_TOKEN="none"

echo "Computing changelog for version ${JRELEASER_PROJECT_VERSION}"
jreleaser changelog > /dev/null

CHANGELOG=out/jreleaser/release/CHANGELOG.md

bat ${CHANGELOG}
rm ${CHANGELOG}
