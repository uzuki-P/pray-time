# On Windows, backticks and recipes run in Windows PowerShell. (`bash` on PATH
# there is often WSL's, so Windows recipes never rely on it.)
set windows-shell := ["powershell.exe", "-NoLogo", "-NoProfile", "-Command"]

gradlew := if os_family() == "windows" { ".\\gradlew.bat" } else { "./gradlew" }
# `<versionBase>.<commit count>`, matching build.gradle.kts.
app_version := replace_regex(read("build.gradle.kts"), '(?s).*\nval versionBase = "([^"]*)".*', '$1') + "." + `git rev-list --count HEAD`
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
    # Run build-exe without a pipe or capture: the Gradle and Kotlin daemons it starts
    # inherit the pipe's write end, so Tee-Object or $(...) would wait until they exit.
    {{just}} build-exe
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    # The newest staged copy of this version is the one build-exe just wrote.
    $staged = Get-ChildItem -LiteralPath _apk -Filter "pray-time-{{app_version}}_*.exe" | Sort-Object CreationTime | Select-Object -Last 1
    if (-not $staged) { throw "build-exe left no staged installer for {{app_version}} in _apk." }
    & .\scripts\update-installed-exe.ps1 -Installer $staged.FullName -Version "{{app_version}}"

# Wait for the GitHub Actions EXE build of the pushed HEAD, save it in _apk/, and share it via Gokapi.
[unix]
ci-exe:
    bash scripts/ci-exe

# Bump versionBase (the last field is the commit count): just bump [major|minor].
[unix]
@bump part="minor":
    #!/usr/bin/env bash
    set -euo pipefail
    file="build.gradle.kts"
    current=$(grep -oP '^val versionBase = "\K[0-9]+\.[0-9]+' "$file")
    IFS='.' read -r major minor <<< "$current"
    case "{{part}}" in
      major) major=$((major+1)); minor=0;;
      minor) minor=$((minor+1));;
      *) echo "unknown part '{{part}}' (use major or minor)"; exit 1;;
    esac
    next="$major.$minor"
    sed -i "s/^val versionBase = \"$current\"/val versionBase = \"$next\"/" "$file"
    echo "versionBase: $current -> $next"

# Bump versionBase (the last field is the commit count): just bump [major|minor].
[windows]
[script("powershell.exe", "-NoLogo", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File")]
[extension(".ps1")]
bump part="minor":
    $ErrorActionPreference = "Stop"
    $file = "build.gradle.kts"
    $text = [IO.File]::ReadAllText((Resolve-Path $file))
    $current = [regex]::Match($text, '(?m)^val versionBase = "(\d+\.\d+)"').Groups[1].Value
    [int[]]$v = $current.Split('.')
    switch ("{{part}}") {
      "major" { $v = @(($v[0] + 1), 0) }
      "minor" { $v = @($v[0], ($v[1] + 1)) }
      default { Write-Host "unknown part '{{part}}' (use major or minor)"; exit 1 }
    }
    $next = $v -join '.'
    $text = [regex]::Replace($text, "(?m)^val versionBase = `"$([regex]::Escape($current))`"", "val versionBase = `"$next`"")
    [IO.File]::WriteAllText((Resolve-Path $file), $text)
    Write-Host "versionBase: $current -> $next"
