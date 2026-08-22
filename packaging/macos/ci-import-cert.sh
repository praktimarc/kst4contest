#!/usr/bin/env bash
#
# Import the Developer ID certificate into a throwaway keychain on a CI runner.
#
# A runner cannot answer the keychain's authorization dialog, so the login
# keychain is unusable there. This creates a dedicated keychain instead, whose
# password is generated here and needed nowhere else -- it is discarded with the
# keychain at the end of the job.
#
# Reads from the environment:
#   MACOS_CERT_P12        base64 of the exported .p12
#   MACOS_CERT_PASSWORD   the password that .p12 was exported with
#
# Exports to $GITHUB_ENV:
#   SIGNING_IDENTITY      for packaging/macos/build-signed-dmg.sh
#   SIGNING_KEYCHAIN      so the cleanup step knows what to delete
#
set -euo pipefail

: "${MACOS_CERT_P12:?MACOS_CERT_P12 is not set}"
: "${MACOS_CERT_PASSWORD:?MACOS_CERT_PASSWORD is not set}"
: "${RUNNER_TEMP:?RUNNER_TEMP is not set}"
: "${GITHUB_ENV:?GITHUB_ENV is not set}"

KEYCHAIN="$RUNNER_TEMP/kst4contest-signing.keychain-db"
KEYCHAIN_PASSWORD="$(uuidgen)"
CERT="$RUNNER_TEMP/cert.p12"

printf '%s' "$MACOS_CERT_P12" | base64 --decode > "$CERT"

security create-keychain -p "$KEYCHAIN_PASSWORD" "$KEYCHAIN"
# Keychains re-lock after five minutes by default, which would strand a build
# halfway through signing.
security set-keychain-settings -lut 21600 "$KEYCHAIN"
security unlock-keychain -p "$KEYCHAIN_PASSWORD" "$KEYCHAIN"

security import "$CERT" -k "$KEYCHAIN" -P "$MACOS_CERT_PASSWORD" \
    -T /usr/bin/codesign -T /usr/bin/security
rm -f "$CERT"

# Lets codesign reach the private key without the UI prompt a runner has no way
# of answering.
security set-key-partition-list -S apple-tool:,apple:,codesign: \
    -s -k "$KEYCHAIN_PASSWORD" "$KEYCHAIN" >/dev/null

# codesign searches the keychain list, so the new keychain has to be on it --
# added to whatever the runner already had, not in place of it.
EXISTING_KEYCHAINS="$(security list-keychains -d user | sed -e 's/^[[:space:]]*"//' -e 's/"$//')"
# shellcheck disable=SC2086
security list-keychains -d user -s "$KEYCHAIN" $EXISTING_KEYCHAINS

IDENTITY="$(security find-identity -v -p codesigning "$KEYCHAIN" \
    | sed -n 's/.*"Developer ID Application: \(.*\)".*/\1/p' | head -n 1)"

if [ -z "$IDENTITY" ]; then
    echo "No 'Developer ID Application' identity found in the imported certificate." >&2
    echo "What the keychain does contain:" >&2
    security find-identity -v -p codesigning "$KEYCHAIN" >&2
    exit 1
fi

echo "Imported identity: Developer ID Application: $IDENTITY"
echo "SIGNING_IDENTITY=$IDENTITY" >> "$GITHUB_ENV"
echo "SIGNING_KEYCHAIN=$KEYCHAIN" >> "$GITHUB_ENV"
