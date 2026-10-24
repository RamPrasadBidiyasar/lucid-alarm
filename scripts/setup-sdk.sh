#!/usr/bin/env bash
#
# Installs the Android SDK pieces the Maven build needs:
#   command-line tools, platform-tools (adb), platform android-30, build-tools 30.0.3
#
# Usage: scripts/setup-sdk.sh [-y]
#   -y   do not ask for confirmation (this also accepts the Android SDK licences)
#
# The SDK goes to $ANDROID_HOME, or ~/Library/Android/sdk on macOS and ~/Android/Sdk on Linux.

set -euo pipefail

CMDLINE_TOOLS_VERSION="11076708"
PLATFORM="android-30"
BUILD_TOOLS="30.0.3"

case "$(uname -s)" in
  Darwin) OS="mac"; DEFAULT_SDK="$HOME/Library/Android/sdk" ;;
  Linux)  OS="linux"; DEFAULT_SDK="$HOME/Android/Sdk" ;;
  *) echo "Unsupported OS: $(uname -s)" >&2; exit 1 ;;
esac
SDK="${ANDROID_HOME:-$DEFAULT_SDK}"
URL="https://dl.google.com/android/repository/commandlinetools-${OS}-${CMDLINE_TOOLS_VERSION}_latest.zip"
SDKMANAGER="$SDK/cmdline-tools/latest/bin/sdkmanager"

if ! command -v java >/dev/null 2>&1; then
  echo "Java is required (JDK 11 or newer). Install one and run this again." >&2
  exit 1
fi

if [[ "${1:-}" != "-y" ]]; then
  echo "This will download about 300 MB into: $SDK"
  echo "and accept the Android SDK licences on your behalf."
  read -r -p "Continue? [y/N] " answer
  [[ "$answer" =~ ^[Yy]$ ]] || { echo "Cancelled."; exit 1; }
fi

if [[ ! -x "$SDKMANAGER" ]]; then
  echo "==> Downloading Android command-line tools"
  TMP="$(mktemp -d)"
  trap 'rm -rf "$TMP"' EXIT
  curl -fL --progress-bar -o "$TMP/cmdline-tools.zip" "$URL"
  unzip -q "$TMP/cmdline-tools.zip" -d "$TMP/unpacked"
  mkdir -p "$SDK/cmdline-tools"
  rm -rf "$SDK/cmdline-tools/latest"
  mv "$TMP/unpacked/cmdline-tools" "$SDK/cmdline-tools/latest"
fi

echo "==> Accepting licences"
# 'yes' is killed by SIGPIPE when sdkmanager stops reading, which is expected.
yes 2>/dev/null | "$SDKMANAGER" --sdk_root="$SDK" --licenses >/dev/null || true

echo "==> Installing platform-tools, $PLATFORM, build-tools $BUILD_TOOLS"
"$SDKMANAGER" --sdk_root="$SDK" "platform-tools" "platforms;$PLATFORM" "build-tools;$BUILD_TOOLS"

# The Maven Android plugin still reads the version of the retired "SDK Tools" package.
if [[ ! -f "$SDK/tools/source.properties" ]]; then
  mkdir -p "$SDK/tools"
  printf 'Pkg.Revision=26.1.1\n' > "$SDK/tools/source.properties"
fi

echo
echo "Android SDK ready at: $SDK"
echo "Optional: add this to your shell profile"
echo "  export ANDROID_HOME=\"$SDK\""
