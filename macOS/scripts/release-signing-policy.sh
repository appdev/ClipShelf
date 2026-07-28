#!/usr/bin/env bash

require_developer_id_application_identity() {
    local identity="${1:-}"
    case "$identity" in
        "Developer ID Application: "*) return 0 ;;
        *)
            echo "release requires CODESIGN_IDENTITY='Developer ID Application: …'" >&2
            return 1
            ;;
    esac
}

verify_release_team_identifier() {
    local app_path="$1"
    local codesign_tool="${CODESIGN_TOOL:-codesign}"
    local team_identifier

    team_identifier="$($codesign_tool -dv --verbose=4 "$app_path" 2>&1 \
        | awk -F= '/^TeamIdentifier=/{print $2; exit}')"
    if [[ -z "$team_identifier" || "$team_identifier" == "not set" ]]; then
        echo "release app has no Team Identifier: $app_path" >&2
        return 1
    fi
}
