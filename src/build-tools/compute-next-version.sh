#!/usr/bin/env sh

DIR="$(dirname "$0")"

CURRENT_BRANCH="$(git rev-parse --verify --abbrev-ref HEAD)"
COMMIT_SHA=$(git rev-parse HEAD)
GIT_REF=$(git show-ref --heads | grep "${CURRENT_BRANCH}" | grep "${COMMIT_SHA}" | cut -d' ' -f2)

BASE_VERSION=$(java --source 17 "${DIR}/BaseVersionFromGitRef.java" "${GIT_REF}" "${COMMIT_SHA}")
VERSION=$(git tag -l "v${BASE_VERSION}.*" | java --source 17 "${DIR}/VersionBumper.java" "${BASE_VERSION}")

echo "${VERSION}"
