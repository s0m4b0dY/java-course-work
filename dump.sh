#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="${1:-.}"

if [[ ! -d "$ROOT_DIR" ]]; then
    echo "Error: '$ROOT_DIR' is not a directory" >&2
    exit 1
fi

cd "$ROOT_DIR"

find . \
    -type f \
    ! -path "./.git/*" \
    ! -path "./target/*" \
    ! -path "./build/*" \
    ! -path "./out/*" \
    ! -path "./.idea/*" \
    | sort \
    | while read -r file; do
        relative_path="${file#./}"

        echo
        echo "============================================================"
        echo "FILE: $relative_path"
        echo "============================================================"
        echo

        cat "$file"
        echo
    done