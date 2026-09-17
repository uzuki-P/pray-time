import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.2.20"
    id("org.jetbrains.compose") version "1.11.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20"
}

group = "dev.praytime"
version = "0.1.0"

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.compose.material3:material3:1.9.0-alpha04")
    implementation("com.batoulapps.adhan:adhan:1.2.1")
    testImplementation(kotlin("test"))
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
