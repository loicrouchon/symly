#!/bin/sh

DIR=$(dirname $0)

COMMIT_SHA=$(git rev-parse HEAD)
GIT_REF=$(git show-ref --heads | grep ${COMMIT_SHA} | cut -d' ' -f2)

BASE_VERSION=$(java --source 17 ${DIR}/BaseVersionFromGitRef.java "${GIT_REF}" "${COMMIT_SHA}")
VERSION=$(git tag -l "v${BASE_VERSION}.*" | java --source 17 ${DIR}/VersionBumper.java ${BASE_VERSION})

echo ${VERSION}
