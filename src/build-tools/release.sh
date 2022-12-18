#!/usr/bin/env sh
set -eu
MAIN_BRANCH="main"
DIR="$(dirname "$0")"

format_red() {
    printf "\e[1;31m"
}
format_clear() {
    printf "\e[0m"
}

echo "Checking origin branch is ${MAIN_BRANCH}"
CURRENT_BRANCH="$(git rev-parse --verify --abbrev-ref HEAD)"
if [ ! "${CURRENT_BRANCH}" = "${MAIN_BRANCH}" ]; then
    format_red
    echo "Releases can only be created from branch ${MAIN_BRANCH}" >&2
    echo "Current branch is ${CURRENT_BRANCH}" >&2
    echo "" >&2
    echo "  git switch ${MAIN_BRANCH}" >&2
    format_clear
    exit 1
fi

echo "Checking for divergence with origin/${MAIN_BRANCH}"
git fetch origin
CURRENT_MAIN_COMMIT="$(git rev-parse --verify --short HEAD)"
REMOTE_MAIN_COMMIT="$(git rev-parse --verify --short "origin/${MAIN_BRANCH}")"
if [ ! "${CURRENT_MAIN_COMMIT}" = "${REMOTE_MAIN_COMMIT}" ]; then
    format_red
    echo "Current commit is ${CURRENT_MAIN_COMMIT} but latest remote is ${REMOTE_MAIN_COMMIT}" >&2
    echo "Pull remote changes and push local ones to origin/${MAIN_BRANCH} first" >&2
    echo "" >&2
    echo "  git pull && git push" >&2
    format_clear
    exit 1
fi

echo "Checking application tests"
./gradlew clean check --console=plain > /dev/null

echo "Checking for uncommitted changes"
UNCOMMITTED_CHANGES="$(git status --porcelain)"
if [ ! "${UNCOMMITTED_CHANGES}" = "" ]; then
    format_red
    echo "There are uncommitted changes, commit them before releasing" >&2
    echo "${UNCOMMITTED_CHANGES}"
    format_clear
    exit 1
fi

echo "Checking last release branches"
LATEST_RELEASE_BRANCH="$(git branch -r --list "origin/release/*" | sort -V | tail -n 1 | sed -r 's#^ *origin/(.+)$#origin/\1#')"
LATEST_RELEASE_BASE_VERSION="$(echo "${LATEST_RELEASE_BRANCH}" | sed -r 's#^origin/release/(.+)$#\1#')"

echo "Latest release branch ${LATEST_RELEASE_BRANCH} base version is ${LATEST_RELEASE_BASE_VERSION}"
echo "Enter next release base version:"
IFS= read -r NEXT_RELEASE_BASE_VERSION
NEXT_RELEASE="release/${NEXT_RELEASE_BASE_VERSION}"
echo "Creating branch ${NEXT_RELEASE} from ${MAIN_BRANCH}"
git branch -c "${NEXT_RELEASE}"
git switch "${NEXT_RELEASE}"

NEXT_RELEASE_VERSION="$("./${DIR}/compute-next-version.sh")"
echo "Changelog for branch ${NEXT_RELEASE} and version ${NEXT_RELEASE_VERSION}"
echo "-------------------------------------------------------------------"
"./${DIR}/jreleaser-dry-run.sh" "${NEXT_RELEASE_VERSION}"
echo "-------------------------------------------------------------------"

echo "Proceed with the release (y/n)"
IFS= read -r PROCEED_WITH_RELEASE
if [ ! "${PROCEED_WITH_RELEASE}" = "y" ]; then
    echo "Aborting the release"
    exit 1
fi

git push -u origin "${NEXT_RELEASE}"
echo "Release branch pushed to GitHub, CI will be triggered soon"
