# List available recipes.
default:
    @just --list

# Run the desktop app in development mode.
dev:
    ./gradlew run

# Build a native package for the current operating system.
package:
    ./gradlew packageDistributionForCurrentOS

# Build the Linux RPM package.
package-rpm:
    ./gradlew packageRpm

# Build the Windows EXE package (requires building on Windows).
package-exe:
    ./gradlew packageExe

# Build the Windows MSI package (requires building on Windows).
package-msi:
    ./gradlew packageMsi

# Run the JVM test task.
test:
    ./gradlew test
