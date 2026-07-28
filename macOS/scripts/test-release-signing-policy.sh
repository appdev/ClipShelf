#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
source scripts/release-signing-policy.sh

require_developer_id_application_identity \
    "Developer ID Application: ClipDock Test (ABCDE12345)"

for rejected_identity in "" "-" "Apple Development: ClipDock Test (ABCDE12345)"; do
    if require_developer_id_application_identity "$rejected_identity" >/dev/null 2>&1; then
        echo "unexpectedly accepted release identity: $rejected_identity" >&2
        exit 1
    fi
done

test_root="$(mktemp -d)"
trap 'rm -rf "$test_root"' EXIT

printf '%s\n' '#!/bin/sh' 'printf "TeamIdentifier=ABCDE12345\\n" >&2' \
    > "$test_root/codesign-with-team"
printf '%s\n' '#!/bin/sh' 'printf "TeamIdentifier=not set\\n" >&2' \
    > "$test_root/codesign-without-team"
chmod +x "$test_root/codesign-with-team" "$test_root/codesign-without-team"

CODESIGN_TOOL="$test_root/codesign-with-team" \
    verify_release_team_identifier "$test_root/ClipDock.app"
if CODESIGN_TOOL="$test_root/codesign-without-team" \
    verify_release_team_identifier "$test_root/ClipDock.app" >/dev/null 2>&1; then
    echo "unexpectedly accepted missing Team Identifier" >&2
    exit 1
fi
