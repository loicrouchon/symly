#!/usr/bin/env sh
set -ex

cur_dir="$(dirname "$(realpath "$0")")"
. "${cur_dir}/common.sh"

echo "Building debian source package ${package_version_dir}"

rm -rf "${target_dir}"
mkdir -p "${target_dir}"
cd "${target_dir}" || exit 1

echo "Downloading upstream tarball from ${upstream_tarball_url}"
curl -sL "${upstream_tarball_url}" -o "${upstream_tarball}"

echo "Unpacking upstream tarball ${upstream_tarball}"
tar xzf "${upstream_tarball}"

echo "Repacking upstream tarball ${upstream_tarball} (get rid off root level directory)"
rm -f "${upstream_tarball}"
tar czf "${upstream_tarball}" "${package_version_dir}"

echo "Add debian dir"
cd "${package_version_dir}" || exit 1
cp -R "${cur_dir}/debian" "debian"

echo "Build source and binary package package"
pwd
# Building the deb for 'all' arch first
dpkg-buildpackage --sign-key="${GPG_KEY_FINGERPRINT}" --build=all
# Building the source package after so thAT THE .changes does not reference the binary package for upload via dput
dpkg-buildpackage --sign-key="${GPG_KEY_FINGERPRINT}" --build=source

display_artifacts
test_package
