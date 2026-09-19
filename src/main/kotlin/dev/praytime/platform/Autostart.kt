package dev.praytime.platform

import java.io.File

/**
 * Start-on-login support. Linux uses an XDG autostart entry pointing at the running
 * AppImage (or installed launcher); Windows uses the HKCU Run registry key. The entry
 * is refreshed on every launch while enabled, so replacing the AppImage with a new
 * version re-points the entry automatically on the next manual start.
 */
object Autostart {
    private val isWindows = System.getProperty("os.name").lowercase().contains("windows")
    private val linuxFile: File by lazy {
        val configHome = System.getenv("XDG_CONFIG_HOME")?.takeIf { it.isNotBlank() }
            ?: (System.getProperty("user.home") + "/.config")
        File(File(configHome, "autostart"), "pray-time.desktop")
    }

    fun isEnabled(): Boolean = if (isWindows) windowsQuery() else linuxFile.exists()

    fun setEnabled(value: Boolean) {
        runCatching {
            if (isWindows) windowsSet(value) else linuxSet(value)
        }
    }

    fun refresh() {
        if (isEnabled() && !isWindows) {
            val path = executablePath() ?: return
            writeLinuxEntry(path)
        }
    }

    private fun executablePath(): String? {
        System.getenv("APPIMAGE")?.takeIf { it.isNotBlank() }?.let { return it }
        System.getProperty("jpackage.app-path")?.takeIf { it.isNotBlank() }?.let { return it }
        return runCatching { File("/proc/self/exe").canonicalPath }.getOrNull()
    }

    private fun linuxSet(value: Boolean) {
        if (!value) {
            linuxFile.delete()
            return
        }
        val path = executablePath() ?: return
        writeLinuxEntry(path)
    }

    private fun writeLinuxEntry(path: String) {
        linuxFile.parentFile.mkdirs()
        val content = """
            [Desktop Entry]
            Type=Application
            Name=Pray Time
            Comment=Prayer time reminders for Linux
            Exec=$path
            Terminal=false
            Categories=Utility;
            X-GNOME-Autostart-enabled=true
        """.trimIndent() + "\n"
        val tmp = File(linuxFile.parentFile, linuxFile.name + ".tmp")
        tmp.writeText(content)
        if (!tmp.renameTo(linuxFile)) {
            linuxFile.writeText(tmp.readText())
            tmp.delete()
        }
    }

    private fun windowsQuery(): Boolean {
        val process = ProcessBuilder(
            "reg", "query", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run", "/v", "Pray Time",
        ).start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        return output.contains("Pray Time")
    }

    private fun windowsSet(value: Boolean) {
        val key = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run"
        if (!value) {
            ProcessBuilder("reg", "delete", key, "/v", "Pray Time", "/f").start().waitFor()
            return
        }
        val path = executablePath() ?: return
        ProcessBuilder("reg", "add", key, "/v", "Pray Time", "/t", "REG_SZ", "/d", path, "/f").start().waitFor()
    }
}
