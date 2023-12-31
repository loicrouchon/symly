#!/bin/sh
set -e
set -x

VERSION="0.11.0r"

DIR=$(dirname "$0")
ROOT_DIR="$DIR/../../.."
ARCHIVE_NAME="symly-$VERSION"
ARCHIVE="$ARCHIVE_NAME.tar.gz"
SPEC="rpmbuild/SPECS/symly.spec"

cd "$ROOT_DIR"
SOURCE_DATE_EPOCH="$(git log -1 --pretty=%ct)"
export SOURCE_DATE_EPOCH
echo "SOURCE_DATE_EPOCH: $SOURCE_DATE_EPOCH"

rm -rf "$ROOT_DIR/.wip/$ARCHIVE_NAME"
mkdir "$ROOT_DIR/.wip/$ARCHIVE_NAME"
cp -R \
    $ROOT_DIR/docs \
    $ROOT_DIR/LICENSE \
    $ROOT_DIR/pom.xml \
    $ROOT_DIR/src \
    $ROOT_DIR/tools \
    "$ROOT_DIR/.wip/$ARCHIVE_NAME"

cd "$ROOT_DIR/.wip"
tar cvf "$ARCHIVE" "$ARCHIVE_NAME"

cd ~
rm -rf ~/rpmbuild
rpmdev-setuptree

sed "s/\${version}/$VERSION/" < "$ROOT_DIR/src/jreleaser/distributions/rpm-spec/symly.spec" > $SPEC
#cat "$ROOT_DIR/src/jreleaser/distributions/rpm-spec/symly.spec" | sed "s/\${version}/$VERSION/" > $SPEC
cp "$ROOT_DIR/.wip/symly-${VERSION}.tar.gz" "rpmbuild/SOURCES/v${VERSION}.tar.gz"

rm -rf /workspace/.wip/rpmbuild/
rpmbuild -bb "$SPEC" ; cp -R rpmbuild /workspace/.wip/rpmbuild

bash
