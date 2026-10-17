# On Windows, backticks and recipes run in Windows PowerShell. (`bash` on PATH
# there is often WSL's, so Windows recipes never rely on it.)
set windows-shell := ["powershell.exe", "-NoLogo", "-NoProfile", "-Command"]

gradlew := if os_family() == "windows" { ".\\gradlew.bat" } else { "./gradlew" }
app_version := replace_regex(read("build.gradle.kts"), '(?s).*\n\s*packageVersion = "([^"]*)".*', '$1')
# The real binary, not the npm shim on PATH: Windows PowerShell blocks its just.ps1.
just := if os_family() == "windows" { "& '" + just_executable() + "'" } else { quote(just_executable()) }
exe_output := "build/compose/binaries/main/exe/pray-time-" + app_version + ".exe"

# List available recipes.
default:
    @{{just}} --list

# Run the desktop app in development mode.
dev:
    {{gradlew}} run

# Build a native package for the current operating system.
build:
    {{gradlew}} packageDistributionForCurrentOS

# Run the JVM test task.
test:
    {{gradlew}} test

# Build the Linux AppImage package.
[unix]
build-appimage:
    ./gradlew packageAppImageFile

# Install the newest AppImage to ~/apps with a desktop entry, so KDE/vicinae
# launchers can start it by name regardless of build file names.
[unix]
install:
    scripts/install-appimage

# Remove the installed AppImage, desktop entry and icon.
[unix]
uninstall:
    scripts/install-appimage --uninstall

# One-shot release flow: build, stage a dated copy in _apk/, refresh the
# ~/apps install and the KDE/vicinae menu entry.
[unix]
update: build-appimage install

# On Windows: build the portable zip (app folder with bundled runtime) into _apk/.
[windows]
build-windows-portable:
    .\gradlew.bat packageAppImage stageWindowsPortable

# On Windows: build the EXE installer and stage it in _apk/.
[windows]
[script("powershell.exe", "-NoLogo", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File")]
[extension(".ps1")]
build-exe:
    $ErrorActionPreference = "Stop"
    # packageExe alone runs jpackage from the jars; update-exe swaps in the
    # app image that only createDistributable writes.
    .\gradlew.bat createDistributable packageExe
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    $stamp = (Get-Date).ToString("dd-MMM_HH-mm", [Globalization.CultureInfo]::InvariantCulture).ToLower()
    New-Item -ItemType Directory -Force _apk | Out-Null
    $target = "_apk/pray-time-{{app_version}}_$stamp.exe"
    Copy-Item -LiteralPath "{{exe_output}}" -Destination $target -Force
    (Resolve-Path $target).Path

# On Windows: build, stage, and swap the installed app's files in place (installs once if missing).
[windows]
[script("powershell.exe", "-NoLogo", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File")]
[extension(".ps1")]
update-exe:
    $ErrorActionPreference = "Stop"
    {{just}} build-exe | Tee-Object -Variable buildOutput
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    $staged = @($buildOutput)[-1]
    & .\scripts\update-installed-exe.ps1 -Installer $staged -Version "{{app_version}}"

# Bump the app version: just bump [major|minor|patch] (default patch).
[unix]
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

# Bump the app version: just bump [major|minor|patch] (default patch).
[windows]
[script("powershell.exe", "-NoLogo", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File")]
[extension(".ps1")]
bump part="patch":
    $ErrorActionPreference = "Stop"
    $file = "build.gradle.kts"
    $text = [IO.File]::ReadAllText((Resolve-Path $file))
    $current = [regex]::Match($text, 'packageVersion = "(\d+\.\d+\.\d+)"').Groups[1].Value
    [int[]]$v = $current.Split('.')
    switch ("{{part}}") {
      "major" { $v = @(($v[0] + 1), 0, 0) }
      "minor" { $v = @($v[0], ($v[1] + 1), 0) }
      "patch" { $v = @($v[0], $v[1], ($v[2] + 1)) }
      default { Write-Host "unknown part '{{part}}' (use major, minor or patch)"; exit 1 }
    }
    $next = $v -join '.'
    $text = $text.Replace("packageVersion = `"$current`"", "packageVersion = `"$next`"")
    $text = [regex]::Replace($text, "(?m)^version = `"$([regex]::Escape($current))`"", "version = `"$next`"")
    [IO.File]::WriteAllText((Resolve-Path $file), $text)
    Write-Host "version: $current -> $next"
