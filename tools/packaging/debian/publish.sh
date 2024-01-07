#!/usr/bin/env sh
set -ex

cur_dir="$(dirname "$(realpath "$0")")"
. "${cur_dir}/common.sh"

display_artifacts
test_package

echo "Publishing debian source package ${package_version_dir} to ${PPA_URL}"
dput "${PPA_URL}" "${target_dir}/${package_version_name}-"*_source.changes
