#!/usr/bin/env bash
# Publish only a distinct, explicitly authorized release from the exact reviewed tag.
# Historical release assets must not be replaced or supplemented by ordinary CI.
set -euo pipefail

fail() {
    printf '::error::%s\n' "$*" >&2
    exit 1
}

: "${ARTIFACT_VERSION:?ARTIFACT_VERSION must be set}"
: "${OUTPUT_NAME:?OUTPUT_NAME must be set}"
: "${GITHUB_SHA:?GITHUB_SHA must be set}"
: "${EXPECTED_SOURCE_SHA:?EXPECTED_SOURCE_SHA must be set}"

[[ "${GITHUB_REPOSITORY:-}" == 'wickidcow/SF_Magic8Ball' ]] || fail 'Unexpected repository identity'
[[ "${GITHUB_EVENT_NAME:-}" == 'workflow_dispatch' ]] || fail 'Only an explicit workflow dispatch can publish'
[[ "${RELEASE_CONFIRMED:-}" == 'true' ]] || fail 'Missing affirmative release confirmation'
[[ "$GITHUB_SHA" =~ ^[0-9a-f]{40}$ ]] || fail 'Source SHA must be 40 lowercase hex characters'
[[ "$EXPECTED_SOURCE_SHA" == "$GITHUB_SHA" ]] || fail 'Reviewed source SHA does not match the checked-out source'
[[ "$ARTIFACT_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || fail 'Unexpected version syntax'
[[ "$OUTPUT_NAME" == "SF_Magic8Ball${ARTIFACT_VERSION}.jar" ]] || fail 'Canonical artifact name mismatch'
[[ "${GITHUB_REF:-}" == "refs/tags/v${ARTIFACT_VERSION}" ]] || fail 'Release must run on the existing exact version tag'
[[ "${GITHUB_REF_NAME:-}" == "v${ARTIFACT_VERSION}" ]] || fail 'Release tag name mismatch'

JAR="dist/${OUTPUT_NAME}"
[[ -s "$JAR" ]] || fail "No nonempty canonical JAR: $JAR"
TAG="v${ARTIFACT_VERSION}"

# Fail closed on an existing GitHub Release. This never edits an asset or
# metadata and can never republish or overwrite previously published bytes.
if gh release view "$TAG" --repo "$GITHUB_REPOSITORY" >/dev/null 2>&1; then
    fail "Release $TAG already exists; refusing to alter it"
fi

# An already-published tag must exist; do not let gh create tags on demand.
# A simultaneous creator races harmlessly: gh release create fails if it exists.
gh release create "$TAG" "$JAR" --repo "$GITHUB_REPOSITORY" --verify-tag \
    --title "SF_Magic8Ball ${ARTIFACT_VERSION}" \
    --notes "New immutable Magic8Ball ${ARTIFACT_VERSION} release. Tested Java 21 plugin package for Minecraft 1.21.11 and compatible newer Paper servers."
