#!/bin/bash

DIR="${0%/*}"
EXTRA_ARGS=(JVM-ARGS)
export CDS_JVM_OPTS="${EXTRA_ARGS[*]}"

"$DIR/../../runtime/Contents/Home/bin/xpiped" "$@"

read -rsp "Press any key to close" -n 1 key
