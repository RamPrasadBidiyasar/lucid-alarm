#!/usr/bin/env bash
#
# Builds the WakeUp APK with Maven and copies it to dist/WakeUp.apk.
#
# Usage: ./build-apk.sh [--install] [--skip-tests] [--yes]
#   --install      install the APK on a connected phone with adb afterwards
#   --skip-tests   skip the unit tests
#   --yes          if the Android SDK is missing, install it without asking
#
# The APK is a release build (not debuggable), signed with a key created on first use in ~/.config/wakeup.

set -euo pipefail
cd "$(dirname "$0")"

INSTALL=0
SKIP_TESTS=0
ASSUME_YES=0
for arg in "$@"; do
  case "$arg" in
    --install) INSTALL=1 ;;
    --skip-tests) SKIP_TESTS=1 ;;
    --yes|-y) ASSUME_YES=1 ;;
    -h|--help) sed -n '2,11p' "$0"; exit 0 ;;
    *) echo "Unknown option: $arg" >&2; exit 1 ;;
  esac
done

case "$(uname -s)" in
  Darwin) DEFAULT_SDK="$HOME/Library/Android/sdk" ;;
  *)      DEFAULT_SDK="$HOME/Android/Sdk" ;;
esac
export ANDROID_HOME="${ANDROID_HOME:-$DEFAULT_SDK}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"

command -v java >/dev/null || { echo "Java (JDK 11+) is required." >&2; exit 1; }
command -v mvn >/dev/null || { echo "Maven is required (https://maven.apache.org)." >&2; exit 1; }

if [[ ! -d "$ANDROID_HOME/platforms/android-30" || ! -d "$ANDROID_HOME/build-tools/30.0.3" ]]; then
  echo "Android SDK pieces are missing in $ANDROID_HOME"
  if [[ $ASSUME_YES -eq 1 ]]; then
    scripts/setup-sdk.sh -y
  else
    scripts/setup-sdk.sh
  fi
fi

# The SDK build-tools are Intel binaries. Apple Silicon Macs need Rosetta to run them.
if [[ "$(uname -s)" == "Darwin" && "$(uname -m)" == "arm64" ]] && ! arch -x86_64 /usr/bin/true 2>/dev/null; then
  echo "Rosetta is not installed, and the Android build tools need it on Apple Silicon." >&2
  echo "Install it once (it asks for your password), then run this script again:" >&2
  echo "  softwareupdate --install-rosetta --agree-to-license" >&2
  exit 1
fi

# Also repair an SDK that was installed before the setup script wrote this file.
if [[ ! -f "$ANDROID_HOME/tools/source.properties" ]]; then
  mkdir -p "$ANDROID_HOME/tools"
  printf 'Pkg.Revision=26.1.1\n' > "$ANDROID_HOME/tools/source.properties"
fi

PLATFORM=30
ANDROID_JAR="$ANDROID_HOME/platforms/android-$PLATFORM/android.jar"
LOCAL_COPY="$HOME/.m2/repository/com/google/android/android/$PLATFORM/android-$PLATFORM.jar"
if [[ ! -f "$LOCAL_COPY" ]]; then
  echo "==> Installing android.jar (platform $PLATFORM) into the local Maven repository"
  mvn -B -q install:install-file -Dfile="$ANDROID_JAR" -DgroupId=com.google.android \
    -DartifactId=android -Dversion="$PLATFORM" -Dpackaging=jar -DgeneratePom=true
fi

MVN_ARGS=(-B clean package)
[[ $SKIP_TESTS -eq 1 ]] && MVN_ARGS+=(-DskipTests)

echo "==> mvn ${MVN_ARGS[*]}"
mvn "${MVN_ARGS[@]}"

# Align, then sign with a key of your own. It is created on first use and kept outside the project.
# Keep a backup of it: a later version of the app can only update this install if it uses the same key.
BUILD_TOOLS="$ANDROID_HOME/build-tools/30.0.3"
KEY_DIR="${WAKEUP_KEY_DIR:-$HOME/.config/wakeup}"
KEYSTORE="$KEY_DIR/wakeup.keystore"
PASS_FILE="$KEY_DIR/wakeup.password"
if [[ ! -f "$KEYSTORE" ]]; then
  echo "==> Creating your signing key in $KEY_DIR"
  mkdir -p "$KEY_DIR"
  chmod 700 "$KEY_DIR"
  openssl rand -hex 16 > "$PASS_FILE"
  chmod 600 "$PASS_FILE"
  keytool -genkeypair -keystore "$KEYSTORE" -storepass "$(cat "$PASS_FILE")" \
    -keypass "$(cat "$PASS_FILE")" -alias wakeup -dname "CN=WakeUp,O=Personal,C=US" \
    -keyalg RSA -keysize 2048 -validity 10000 >/dev/null
  chmod 600 "$KEYSTORE"
fi
mkdir -p dist
"$BUILD_TOOLS/zipalign" -f -p 4 target/wakeup.apk target/wakeup-aligned.apk
"$BUILD_TOOLS/apksigner" sign --ks "$KEYSTORE" --ks-pass "file:$PASS_FILE" \
  --ks-key-alias wakeup --v4-signing-enabled false --out dist/WakeUp.apk target/wakeup-aligned.apk
"$BUILD_TOOLS/apksigner" verify dist/WakeUp.apk
echo
echo "APK ready: $(pwd)/dist/WakeUp.apk ($(du -h dist/WakeUp.apk | cut -f1))"

if [[ $INSTALL -eq 1 ]]; then
  ADB="$ANDROID_HOME/platform-tools/adb"
  [[ -x "$ADB" ]] || ADB="$(command -v adb)"
  echo "==> Installing on the connected device"
  "$ADB" install -r dist/WakeUp.apk
fi
