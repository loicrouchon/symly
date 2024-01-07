#!/usr/bin/env sh
set -ex

package_name="symly"
version="$1"
if [ "${version}" = "" ]; then
    echo "Missing version argument"
    exit 1
fi

if ! (cat "${cur_dir}/debian/changelog" | grep -E "^${package_name} " | head -n 1 | grep -q "${package_name} (${version}-"); then
    echo "Package ${package_name} does not have a changelog entry for version ${version}"
    exit 1
fi

target_dir="${cur_dir}/target/${package_name}"
package_version_dir="${package_name}-${version}"
package_version_name="${package_name}_${version}"
upstream_tarball="${package_name}_${version}.orig.tar.gz"
upstream_tarball_url="https://github.com/loicrouchon/symly/archive/refs/tags/v${version}.tar.gz"

cat "${cur_dir}/gpg/"*.key  | gpg --import

display_artifacts() {
    ls -l "${target_dir}"
    echo ".dsc content:"
    cat "${target_dir}/${package_version_name}-"*.dsc
    echo ".buildinfo content:"
    cat "${target_dir}/${package_version_name}-"*.buildinfo
    echo ".changes content:"
    cat "${target_dir}/${package_version_name}-"*.changes
}

test_package() {
    apt install -y "${target_dir}/${package_version_name}-"*_all.deb
    version_command_output="$(symly --version)"
    if ! (echo "${version_command_output}" | grep -Eq "^Symly version ${version}"); then
        echo "ERR: Failed to test 'symly --version'
ERR:   Expecting: Symly version ${version}
ERR:   But got:   ${version_command_output}"
        exit 1
    else
        echo "test successful: ${version_command_output}"
    fi
}
