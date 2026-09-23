package dev.praytime.platform

import dev.praytime.domain.Region
import dev.praytime.ui.ReminderController
import dev.praytime.ui.ThemeMode
import java.io.File
import java.time.Instant
import java.time.LocalDate

/**
 * Persists app state as a simple key=value file at $XDG_CONFIG_HOME/praytime/state.properties.
 * Values are region names, ISO dates, and comma-separated ISO instants - no quoting needed.
 */
object AppState {
    val dir: File by lazy {
        val configHome = System.getenv("XDG_CONFIG_HOME")?.takeIf { it.isNotBlank() }
            ?: (System.getProperty("user.home") + "/.config")
        File(configHome, "praytime").apply { mkdirs() }
    }
    private val file: File get() = File(dir, "state.properties")

    private var cached: Map<String, String>? = null

    fun loadRegion(regions: List<Region>): Region? {
        val name = readField("region") ?: return null
        return regions.firstOrNull { it.name == name }
    }

    fun saveRegion(region: Region) = writeField("region", region.name)

    fun loadCompleted(today: LocalDate): Set<Instant> {
        if (readField("completedDate") != today.toString()) return emptySet()
        return readField("completed").orEmpty()
            .split(',')
            .filter { it.isNotBlank() }
            .mapNotNull { runCatching { Instant.parse(it) }.getOrNull() }
            .toSet()
    }

    fun saveCompleted(today: LocalDate, instants: Set<Instant>) {
        val updated = read().toMutableMap()
        updated["completedDate"] = today.toString()
        updated["completed"] = instants.joinToString(",") { it.toString() }
        commit(updated)
    }

    fun recentRegions(): List<Region> =
        (0 until RECENT_LIMIT).mapNotNull { parseRegion(readField("recent.$it")) }

    fun loadReminderEnabled(): Boolean = readField("reminder.enabled") != "false"

    fun saveReminderEnabled(enabled: Boolean) = writeField("reminder.enabled", enabled.toString())

    fun loadReminderMinutes(): Int =
        readField("reminder.minutes")?.toIntOrNull()?.takeIf { it in MIN_REMINDER_MINUTES..MAX_REMINDER_MINUTES }
            ?: ReminderController.DEFAULT_REMINDER_MINUTES

    fun saveReminderMinutes(minutes: Int) =
        writeField("reminder.minutes", minutes.coerceIn(MIN_REMINDER_MINUTES, MAX_REMINDER_MINUTES).toString())

    fun loadMainAnchor(): WindowAnchor = loadAnchor("main.anchor")

    fun saveMainAnchor(anchor: WindowAnchor) = writeField("main.anchor", anchor.name)

    fun loadReminderAnchor(): WindowAnchor = loadAnchor("reminder.anchor")

    fun saveReminderAnchor(anchor: WindowAnchor) = writeField("reminder.anchor", anchor.name)

    fun loadThemeMode(): ThemeMode =
        readField("theme")?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM

    fun saveThemeMode(mode: ThemeMode) = writeField("theme", mode.name)

    private fun loadAnchor(key: String): WindowAnchor =
        readField(key)?.let { runCatching { WindowAnchor.valueOf(it) }.getOrNull() } ?: WindowAnchor.TOP_RIGHT

    fun saveRecentRegion(region: Region) {
        val current = recentRegions()
            .filterNot { it.name == region.name && it.country == region.country }
        val updated = read().toMutableMap()
        updated.keys.removeAll { it.startsWith("recent.") }
        (listOf(region) + current).take(RECENT_LIMIT)
            .forEachIndexed { index, r -> updated["recent.$index"] = encodeRegion(r) }
        commit(updated)
    }

    private fun read(): Map<String, String> {
        cached?.let { return it }
        val map = if (file.exists()) {
            file.readLines()
                .mapNotNull { line ->
                    val idx = line.indexOf('=')
                    if (idx <= 0) null else line.take(idx) to line.substring(idx + 1)
                }
                .toMap()
        } else {
            emptyMap()
        }
        cached = map
        return map
    }

    private fun readField(key: String): String? = read()[key]

    private fun writeField(key: String, value: String) {
        val updated = read().toMutableMap()
        updated[key] = value
        commit(updated)
    }

    private fun commit(updated: Map<String, String>) {
        cached = updated
        runCatching {
            val tmp = File(dir, file.name + ".tmp")
            tmp.writeText(updated.entries.joinToString("\n") { "${it.key}=${it.value}" })
            if (!tmp.renameTo(file)) {
                file.writeText(tmp.readText())
                tmp.delete()
            }
        }
    }

    private fun encodeRegion(region: Region): String = listOf(
        region.name.replace('|', ' '),
        region.adminName.replace('|', ' '),
        region.country.replace('|', ' '),
        region.latitude,
        region.longitude,
        region.timezoneId,
    ).joinToString("|")

    private fun parseRegion(value: String?): Region? {
        if (value == null) return null
        val fields = value.split('|')
        if (fields.size < 6) return null
        val latitude = fields[3].toDoubleOrNull() ?: return null
        val longitude = fields[4].toDoubleOrNull() ?: return null
        return Region(fields[0], fields[1], fields[2], latitude, longitude, fields[5])
    }

    private const val RECENT_LIMIT = 10

    const val MIN_REMINDER_MINUTES = 1
    const val MAX_REMINDER_MINUTES = 120
}
