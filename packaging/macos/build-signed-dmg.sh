#!/usr/bin/env bash
#
# Local signed (and optionally notarized) macOS build.
#
# jpackage cannot sign the app itself: it ad-hoc signs the embedded runtime and
# then re-runs codesign on the same files without --force, which codesign
# rejects with "is already signed". So this builds an unsigned app-image, signs
# it from the inside out ourselves, and only then wraps it into a DMG.
#
# Required:
#   SIGNING_IDENTITY   The name part of the Developer ID Application certificate,
#                      without the "Developer ID Application: " prefix. Example:
#                        SIGNING_IDENTITY="Philipp Wagner (ABCDE12345)"
#                      List available ones with:
#                        security find-identity -v -p codesigning
#
# Optional:
#   Notarization, either as three separate values (what CI uses)...
#     NOTARY_KEY       Path to the App Store Connect .p8 private key
#     NOTARY_KEY_ID    The key's ID, also part of the .p8 filename
#     NOTARY_ISSUER    The issuer UUID, shown above the key list in the portal
#   ...or as a keychain profile previously created with
#     NOTARY_PROFILE     xcrun notarytool store-credentials <name>
#
#   With neither, the build is signed but not notarized -- enough to test
#   locally, not enough to distribute.
#
set -euo pipefail

cd "$(dirname "$0")/../.."
REPO_ROOT="$PWD"

BUNDLE_ID="de.x08.KST4Contest"
ENTITLEMENTS="packaging/macos/kst4contest.entitlements"

if [ -z "${SIGNING_IDENTITY:-}" ]; then
    echo "SIGNING_IDENTITY is not set. Available signing identities:" >&2
    security find-identity -v -p codesigning >&2 || true
    exit 1
fi
FULL_IDENTITY="Developer ID Application: $SIGNING_IDENTITY"

# notarytool takes either an API key triple or a stored keychain profile. The
# triple needs no keychain at all, which is why CI uses it.
NOTARY_ARGS=()
if [ -n "${NOTARY_KEY:-}" ] && [ -n "${NOTARY_KEY_ID:-}" ] && [ -n "${NOTARY_ISSUER:-}" ]; then
    NOTARY_ARGS=(--key "$NOTARY_KEY" --key-id "$NOTARY_KEY_ID" --issuer "$NOTARY_ISSUER")
elif [ -n "${NOTARY_PROFILE:-}" ]; then
    NOTARY_ARGS=(--keychain-profile "$NOTARY_PROFILE")
fi

echo "==> Building JAR and collecting runtime dependencies"
chmod +x mvnw
./mvnw -B -DskipTests package \
    dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/dist-libs
JAR="$(ls -t target/praktiKST-*.jar | head -n 1)"
cp "$JAR" target/dist-libs/app.jar

# jpackage only accepts a numeric major[.minor[.patch]] as the macOS bundle
# version, so a Maven qualifier like "-nightly" has to be trimmed off.
POM_VERSION="${JAR##*/praktiKST-}"
POM_VERSION="${POM_VERSION%.jar}"
APP_VERSION="$(printf '%s' "$POM_VERSION" | sed -e 's/[^0-9.].*$//' -e 's/\.*$//')"
[ -n "$APP_VERSION" ] || { echo "Could not derive app version from $JAR" >&2; exit 1; }
echo "==> Version: $POM_VERSION -> bundle version $APP_VERSION"

echo "==> Step 1/4: jpackage app-image (unsigned)"
rm -rf dist
mkdir -p dist
ADD_MODULES="$(java packaging/AddModules.java)"

MACOSX_DEPLOYMENT_TARGET="13.0" jpackage \
    --type app-image \
    --name KST4Contest \
    --app-version "$APP_VERSION" \
    --icon packaging/icons/kst4contest.icns \
    --input target/dist-libs \
    --main-jar app.jar \
    --main-class kst4contest.view.Kst4ContestApplication \
    --module-path target/dist-libs \
    --add-modules "$ADD_MODULES" \
    --mac-package-identifier "$BUNDLE_ID" \
    --mac-package-name KST4Contest \
    --dest dist/appimage

APP="dist/appimage/KST4Contest.app"
[ -d "$APP" ] || { echo "jpackage produced no app image" >&2; exit 1; }

echo "==> Step 2/4: signing bundle contents (this takes a few minutes)"

# Apple's notary service unpacks JARs and checks the native libraries inside
# them. sqlite-jdbc ships libsqlitejdbc.dylib for both architectures that way,
# and an unsigned binary in there fails the whole submission. So sign those
# first: the app bundle's seal covers Contents/app, and rewriting a JAR
# afterwards would invalidate it.
echo "    scanning jars for native libraries"
find "$APP/Contents/app" -name '*.jar' -type f | while read -r JARPATH; do
    # Only unpack jars that can plausibly hold a native library. Note the
    # plain grep: "grep -q" exits at the first match, which hands unzip a
    # SIGPIPE, and under "set -o pipefail" that failure becomes the pipeline's
    # status -- inverting this very test.
    if ! unzip -l "$JARPATH" | grep -E '\.(dylib|jnilib|so)$' >/dev/null; then
        continue
    fi

    JARABS="$(cd "$(dirname "$JARPATH")" && pwd)/$(basename "$JARPATH")"
    JARTMP="$(mktemp -d)"
    unzip -q "$JARABS" -d "$JARTMP"

    NATIVES="$(mktemp)"
    ( cd "$JARTMP" && find . -type f \( -name '*.dylib' -o -name '*.jnilib' -o -name '*.so' \) \
        | while read -r n; do
            if [ "$(file --mime-type -b "$n")" = "application/x-mach-binary" ]; then
                printf '%s\n' "${n#./}"
            fi
          done ) > "$NATIVES"

    if [ -s "$NATIVES" ]; then
        echo "    $(basename "$JARPATH"): $(wc -l < "$NATIVES" | tr -d ' ') native lib(s)"
        ( cd "$JARTMP" && xargs -I {} codesign --force --timestamp --options runtime \
            --sign "$FULL_IDENTITY" {} < "$NATIVES" )
        # Update in place rather than repacking, so the rest of the jar --
        # manifest, module descriptor, entry order -- stays byte for byte.
        ( cd "$JARTMP" && xargs jar --update --file "$JARABS" < "$NATIVES" )
    fi

    rm -rf "$JARTMP" "$NATIVES"
done

# Every Mach-O file has to carry its own signature before the enclosing bundle
# can be sealed, so collect them first. jpackage leaves them ad-hoc signed,
# hence --force on every call.
# file(1) pads its output into columns when given several arguments at once,
# so ask it one file at a time with -b and get an unambiguous answer.
MACHO_LIST="$(mktemp)"
find "$APP" -type f -print0 | while IFS= read -r -d '' f; do
    case "$(file --mime-type -b "$f")" in
        application/x-mach-binary) printf '%s\n' "$f" ;;
    esac
done > "$MACHO_LIST"

COUNT="$(wc -l < "$MACHO_LIST" | tr -d ' ')"
echo "    $COUNT Mach-O files to sign"

# codesign contacts Apple's timestamp server on every call, so run a handful in
# parallel or this takes far longer than it needs to.
xargs -P 8 -I {} codesign --force --timestamp --options runtime \
    --sign "$FULL_IDENTITY" {} < "$MACHO_LIST"
rm -f "$MACHO_LIST"

# The embedded JDK is a bundle in its own right and must be sealed before the
# app that contains it.
echo "    sealing embedded runtime"
codesign --force --timestamp --options runtime \
    --sign "$FULL_IDENTITY" "$APP/Contents/runtime"

# Entitlements go on the outermost bundle: the hardened runtime derives the
# process's entitlements from the main executable's signature.
echo "    sealing app bundle"
codesign --force --timestamp --options runtime \
    --entitlements "$ENTITLEMENTS" \
    --sign "$FULL_IDENTITY" "$APP"

# Apple rejects the whole submission over a single unsigned native library, and
# a round trip to the notary service costs minutes. Check its two criteria --
# a Developer ID authority and a secure timestamp -- locally first.
echo "    preflight: verifying native libraries inside jars"
PREFLIGHT_ERRORS="$(mktemp)"
find "$APP/Contents/app" -name '*.jar' -type f | while read -r JARPATH; do
    if ! unzip -l "$JARPATH" | grep -E '\.(dylib|jnilib|so)$' >/dev/null; then
        continue
    fi
    CHECKTMP="$(mktemp -d)"
    unzip -q "$JARPATH" -d "$CHECKTMP"
    find "$CHECKTMP" -type f \( -name '*.dylib' -o -name '*.jnilib' -o -name '*.so' \) \
        | while read -r NATIVE; do
            [ "$(file --mime-type -b "$NATIVE")" = "application/x-mach-binary" ] || continue
            INFO="$(codesign -dv --verbose=2 "$NATIVE" 2>&1 || true)"
            LABEL="$(basename "$JARPATH")/${NATIVE#"$CHECKTMP"/}"
            printf '%s' "$INFO" | grep -q "Authority=Developer ID Application" \
                || echo "$LABEL: not signed with a Developer ID certificate" >> "$PREFLIGHT_ERRORS"
            printf '%s' "$INFO" | grep -q "Timestamp=" \
                || echo "$LABEL: signature has no secure timestamp" >> "$PREFLIGHT_ERRORS"
          done
    rm -rf "$CHECKTMP"
done
if [ -s "$PREFLIGHT_ERRORS" ]; then
    echo "ERROR: these would fail notarization:" >&2
    sed 's/^/    /' "$PREFLIGHT_ERRORS" >&2
    rm -f "$PREFLIGHT_ERRORS"
    exit 1
fi
rm -f "$PREFLIGHT_ERRORS"
echo "    preflight ok"

echo "==> Step 3/4: building the dmg"

# Not with jpackage: "jpackage --type dmg --app-image" re-signs the app it is
# handed, replacing our Developer ID signature with an ad-hoc one and dropping
# the hardened runtime flag. hdiutil copies the bundle verbatim instead.
DMG="dist/KST4Contest-${APP_VERSION}.dmg"
STAGE="$(mktemp -d)"
# ditto rather than cp -R: it preserves the extended attributes the code
# signature depends on.
ditto "$APP" "$STAGE/KST4Contest.app"
ln -s /Applications "$STAGE/Applications"

hdiutil create -volname "KST4Contest" -srcfolder "$STAGE" \
    -ov -format UDZO -quiet "$DMG"
rm -rf "$STAGE"
[ -f "$DMG" ] || { echo "hdiutil produced no DMG" >&2; exit 1; }

# Signing the DMG itself is not what Gatekeeper judges -- that is the .app
# inside -- but Apple expects the container to be signed too.
codesign --force --timestamp --sign "$FULL_IDENTITY" "$DMG"
echo "==> Built $DMG"

echo "==> Step 4/4: verification"

if [ ${#NOTARY_ARGS[@]} -gt 0 ]; then
    echo "    submitting for notarization (waits for Apple's verdict)"
    # Without a timeout a stalled submission would hang a CI job forever.
    xcrun notarytool submit "$DMG" "${NOTARY_ARGS[@]}" --wait --timeout 30m
    echo "    stapling ticket"
    xcrun stapler staple "$DMG"
else
    echo "    no notarization credentials set, skipping notarization"
fi

# Everything below inspects the app as it actually ships, mounted from the DMG,
# rather than the staging copy on disk.
MOUNT_POINT="$(mktemp -d)"
hdiutil attach "$DMG" -nobrowse -quiet -mountpoint "$MOUNT_POINT"
trap 'hdiutil detach "$MOUNT_POINT" -quiet 2>/dev/null || hdiutil detach "$MOUNT_POINT" -force -quiet 2>/dev/null || true' EXIT
SHIPPED_APP="$MOUNT_POINT/KST4Contest.app"

echo "--- codesign --verify on the app inside the DMG ---"
codesign --verify --deep --strict --verbose=2 "$SHIPPED_APP"

echo "--- app identity ---"
codesign -dv --verbose=2 "$SHIPPED_APP" 2>&1 | grep -iE "identifier|authority|teamidentifier|flags"

# An ad-hoc signature here means something along the way re-signed the bundle.
if codesign -dv "$SHIPPED_APP" 2>&1 | grep -q "adhoc"; then
    echo "ERROR: the app inside the DMG is ad-hoc signed, not Developer ID signed" >&2
    exit 1
fi

echo "--- entitlements as signed ---"
codesign -d --entitlements - --xml "$SHIPPED_APP" 2>/dev/null | plutil -convert xml1 -o - - | grep -E "key|true|false"

echo "--- dmg identity ---"
codesign -dv --verbose=2 "$DMG" 2>&1 | grep -iE "authority|teamidentifier" | head -2

echo "--- spctl assessment ---"
# Without notarization this reports "rejected"; that is expected.
spctl --assess --type execute --verbose=4 "$SHIPPED_APP" || true

if [ ${#NOTARY_ARGS[@]} -gt 0 ]; then
    echo "--- stapler validate ---"
    xcrun stapler validate "$DMG"
fi

echo
echo "Done: $REPO_ROOT/$DMG"
