#!/bin/sh

set -eu

DIR="$(dirname "$0")"
ROOT_DIR="${DIR}/../.."
cd "${ROOT_DIR}"

if [ "$#" = "1" ]; then
    export JRELEASER_PROJECT_VERSION="$1"
else
    git fetch --tags
    LAST_VERSION=$(git tag -l | sort -V | tail -n 1 | tr -d 'v')
    export JRELEASER_PROJECT_VERSION=$(git tag -l | java --source 17 "${DIR}/VersionBumper.java" "${LAST_VERSION}")
fi
export JRELEASER_GITHUB_TOKEN="none"

echo "Computing release for version ${JRELEASER_PROJECT_VERSION}"
rm -rf "out"
./gradlew clean build -Pversion="${JRELEASER_PROJECT_VERSION}" --console=plain
jreleaser full-release --dryrun

CHANGELOG=out/jreleaser/release/CHANGELOG.md

bat ${CHANGELOG}
