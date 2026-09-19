import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.net.URI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

plugins {
    kotlin("jvm") version "2.4.20"
    id("org.jetbrains.compose") version "1.11.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20"
}

group = "dev.praytime"
version = "0.1.1"

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
    implementation("org.xerial:sqlite-jdbc:3.53.4.0")
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
            modules("jdk.security.auth", "java.sql")
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.AppImage)
            packageName = "pray-time"
            packageVersion = "0.1.1"
            description = "Prayer time reminders for Windows and Linux"
            copyright = "Copyright © 2026 Uzuki-P"

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

val stagedArtifactsDir = layout.projectDirectory.dir("_apk")

fun timestamp(): String =
    LocalDateTime.now()
        .format(DateTimeFormatter.ofPattern("dd-MMM_HH-mm", Locale.ROOT))
        .lowercase()

fun registerStagingTask(
    taskName: String,
    binaryDir: String,
    extension: String,
    insertBeforeArchitecture: Boolean,
) = tasks.register(taskName) {
    val sources = layout.projectDirectory.dir("build/compose/binaries/main/$binaryDir")
    val outputDir = stagedArtifactsDir
    doLast {
        val artifact = sources.asFileTree.files.firstOrNull { it.extension == extension } ?: return@doLast
        val base = artifact.nameWithoutExtension
        val stagedName = if (insertBeforeArchitecture) {
            val architectureStart = base.lastIndexOf('.')
            "${base.substring(0, architectureStart)}_${timestamp()}.${base.substring(architectureStart + 1)}.$extension"
        } else {
            "${base}_${timestamp()}.$extension"
        }
        outputDir.asFile.mkdirs()
        val staged = outputDir.file(stagedName).asFile
        artifact.copyTo(staged, overwrite = true)
        staged.setExecutable(artifact.canExecute())
        println("Staged artifact: ${staged.absolutePath}")
    }
}

val stageAppImage = registerStagingTask("stageAppImage", "appimage", "AppImage", insertBeforeArchitecture = false)

val appImageToolFile = File(System.getProperty("user.home"), ".cache/pray-time/tools/appimagetool-x86_64.AppImage")
val appImageSourceDir = layout.projectDirectory.dir("build/compose/binaries/main/app/pray-time")
val appImageOutputDir = layout.projectDirectory.dir("build/compose/binaries/main/appimage")
val appImageIconFile = layout.projectDirectory.file("src/main/resources/icons/pray-time.png").asFile

// Windows portable build: the jpackage app-image folder (pray-time.exe plus bundled runtime),
// zipped as-is - no installer, runs from anywhere it is extracted. No-op on other platforms.
val stageWindowsPortable = tasks.register("stageWindowsPortable") {
    val sourceDir = appImageSourceDir
    val outputDir = stagedArtifactsDir
    val version = project.version.toString()
    doLast {
        val dir = sourceDir.asFile
        if (!File(dir, "pray-time.exe").exists()) return@doLast
        outputDir.asFile.mkdirs()
        val staged = File(outputDir.asFile, "pray-time-$version-windows-portable.zip")
        ZipOutputStream(staged.outputStream()).use { zip ->
            dir.walkTopDown().filter { it.isFile }.forEach { file ->
                val entry = ZipEntry("pray-time/" + file.toRelativeString(dir).replace('\\', '/'))
                zip.putNextEntry(entry)
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        println("Staged artifact: ${staged.absolutePath}")
    }
}

tasks.register("packageAppImageFile") {
    dependsOn("packageAppImage")
    val sourceDir = appImageSourceDir
    val outputDir = appImageOutputDir
    val iconFile = appImageIconFile
    val toolFile = appImageToolFile
    val version = project.version.toString()
    doLast {
        if (!toolFile.exists()) {
            toolFile.parentFile.mkdirs()
            println("Downloading appimagetool to ${toolFile.absolutePath}")
            URI("https://github.com/AppImage/AppImageKit/releases/download/continuous/appimagetool-x86_64.AppImage")
                .toURL()
                .openStream()
                .use { input -> toolFile.outputStream().use { input.copyTo(it) } }
            toolFile.setExecutable(true)
        }
        val appDir = File(outputDir.asFile, "pray-time.AppDir")
        appDir.deleteRecursively()
        val usrDir = File(appDir, "usr")
        usrDir.mkdirs()
        val copy = ProcessBuilder("cp", "-a", "${sourceDir.asFile.absolutePath}/.", usrDir.absolutePath)
            .redirectErrorStream(true)
            .start()
        copy.inputStream.bufferedReader().forEachLine(::println)
        check(copy.waitFor() == 0) { "failed to copy app image into AppDir" }
        File(appDir, "AppRun").apply {
            writeText(
                """
                #!/bin/sh
                HERE="$(dirname "$(readlink -f "$0")")"
                exec "${'$'}HERE/usr/bin/pray-time" "$@"
                """.trimIndent() + "\n",
            )
            setExecutable(true)
        }
        File(appDir, "pray-time.desktop").writeText(
            """
            [Desktop Entry]
            Type=Application
            Name=Pray Time
            Comment=Prayer time reminders for Windows and Linux
            Exec=pray-time
            Icon=pray-time
            Categories=Utility;
            Terminal=false
            """.trimIndent() + "\n",
        )
        iconFile.copyTo(File(appDir, "pray-time.png"), overwrite = true)
        outputDir.asFile.mkdirs()
        outputDir.asFile.listFiles { file -> file.isFile && file.extension == "AppImage" }?.forEach { it.delete() }
        val appImageFile = File(outputDir.asFile, "pray-time-$version.AppImage")
        if (appImageFile.exists()) {
            appImageFile.delete()
        }
        val process = ProcessBuilder(
            toolFile.absolutePath,
            "--appimage-extract-and-run",
            "--no-appstream",
            appDir.absolutePath,
            appImageFile.absolutePath,
        ).apply {
            environment()["ARCH"] = "x86_64"
            redirectErrorStream(true)
        }.start()
        process.inputStream.bufferedReader().forEachLine(::println)
        val exitCode = process.waitFor()
        check(exitCode == 0) { "appimagetool failed with exit code $exitCode" }
        println("AppImage written to ${appImageFile.absolutePath}")
    }
}

tasks.matching { it.name == "packageAppImageFile" }.configureEach { finalizedBy(stageAppImage) }
tasks.matching { it.name == "packageAppImage" }.configureEach { finalizedBy(stageWindowsPortable) }
