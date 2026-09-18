import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.4.20"
    id("org.jetbrains.compose") version "1.11.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20"
}

group = "dev.praytime"
version = "0.1.0"

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.compose.components:components-resources:1.11.0")
    implementation("org.jetbrains.compose.material3:material3:1.9.0-alpha04")
    implementation("com.batoulapps.adhan:adhan:1.2.1")
    implementation("dev.nucleusframework:composenativetray:2.1.6")
    implementation("dev.nucleusframework:nucleus.darkmode-detector:2.5.5")
    implementation("com.github.hypfvieh:dbus-java-core:5.2.1")
    implementation("com.github.hypfvieh:dbus-java-transport-native-unixsocket:5.2.1")
    testImplementation(kotlin("test"))
}

compose {
    resources {
        packageOfResClass = "dev.praytime.resources"
    }
}

compose.desktop {
    application {
        mainClass = "dev.praytime.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "pray-time"
            packageVersion = "0.1.0"
            description = "Prayer time reminders for Windows and Linux"
            copyright = "Copyright © 2026"

            linux {
                iconFile.set(project.file("src/main/resources/icons/pray-time.png"))
            }

            windows {
                iconFile.set(project.file("src/main/resources/icons/pray-time.ico"))
                menuGroup = "Pray Time"
            }
        }
    }
}
