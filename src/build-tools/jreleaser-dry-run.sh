#!/bin/sh

set -eu

DIR="$(dirname "$0")"
ROOT_DIR="${DIR}/../.."
cd "${ROOT_DIR}"

echo "Enter GitHub public repositories read-only token:"
IFS= read -r GITHUB_TOKEN
export JRELEASER_GITHUB_TOKEN="${GITHUB_TOKEN}"

if [ "$#" = "1" ]; then
    PROJECT_VERSION="$1"
else
    echo "Computing release for version ${PROJECT_VERSION}"
    git fetch --tags
    LAST_VERSION=$(git tag -l | sort -V | tail -n 1 | tr -d 'v')
    PROJECT_VERSION=$(git tag -l | java --source 17 "${DIR}/VersionBumper.java" "${LAST_VERSION}")
fi
export JRELEASER_PROJECT_VERSION="${PROJECT_VERSION}"

echo "Performing build verification"
rm -rf "out"
./gradlew clean build -Pversion="${JRELEASER_PROJECT_VERSION}" --console=plain > /dev/null

echo "Performing jreleaser releasing verification"
jreleaser full-release --dry-run

echo "Changelog"
CHANGELOG=out/jreleaser/release/CHANGELOG.md

bat ${CHANGELOG}
