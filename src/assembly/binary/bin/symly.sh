#!/bin/sh
lib_dir="$(dirname $(dirname "$(readlink -f "$0")"))/lib"
exec java \
    --class-path "$lib_dir/*" \
    ${symly.java.options} \
    ${symly.main.class} \
    "$@"

