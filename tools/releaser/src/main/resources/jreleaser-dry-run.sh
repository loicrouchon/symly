#!/usr/bin/env sh

set -eu

DIR="$(dirname "$0")"
ROOT_DIR="${DIR}/../../../../.."
cd "${ROOT_DIR}"

echo "Enter GitHub public repositories read-only token:"
IFS= read -r GITHUB_TOKEN
export JRELEASER_GITHUB_TOKEN="${GITHUB_TOKEN}"
export JRELEASER_PROJECT_VERSION="$1"

echo "Performing build verification"
rm -rf "out"

echo "Performing jreleaser releasing verification"
./mvnw -Pjreleaser jreleaser:assemble
./mvnw -Pjreleaser jreleaser:sign
./mvnw -Pjreleaser jreleaser:package
./mvnw -Pjreleaser jreleaser:changelog

echo "Changelog"
CHANGELOG=target/jreleaser/release/CHANGELOG.md

cat ${CHANGELOG}
