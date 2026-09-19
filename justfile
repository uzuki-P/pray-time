# List available recipes.
default:
    @just --list

# Run the desktop app in development mode.
dev:
    ./gradlew run

# Build a native package for the current operating system.
build:
    ./gradlew packageDistributionForCurrentOS

# Build the Linux AppImage package.
build-appimage:
    ./gradlew packageAppImageFile

# Build the Windows portable zip (requires building on Windows).
build-windows-portable:
    ./gradlew packageAppImage stageWindowsPortable

# Bump the app version: just bump [major|minor|patch] (default patch).
@bump part="patch":
    #!/usr/bin/env bash
    set -euo pipefail
    file="build.gradle.kts"
    current=$(grep -oP 'packageVersion = "\K[0-9]+\.[0-9]+\.[0-9]+' "$file")
    IFS='.' read -r major minor patch <<< "$current"
    case "{{part}}" in
      major) major=$((major+1)); minor=0; patch=0;;
      minor) minor=$((minor+1)); patch=0;;
      patch) patch=$((patch+1));;
      *) echo "unknown part '{{part}}' (use major, minor or patch)"; exit 1;;
    esac
    next="$major.$minor.$patch"
    sed -i "s/packageVersion = \"$current\"/packageVersion = \"$next\"/" "$file"
    sed -i "s/^version = \"$current\"/version = \"$next\"/" "$file"
    echo "version: $current -> $next"

# Run the JVM test task.
test:
    ./gradlew test
