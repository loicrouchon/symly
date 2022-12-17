#!/usr/bin/env sh
set -e

DIRS="docs src/docs"
DIRTY_CHECK="git --no-pager diff --name-status --no-color ${DIRS}"

if [ "$(${DIRTY_CHECK} | wc -l)" -gt 0 ]; then
    echo "Dirty documentation is present."
    echo "It is generated as part of the build."
    echo "Please re-generate it and add it to source control"
    echo ""
    echo "Dirty files:"
    ${DIRTY_CHECK}
    exit 1
fi
