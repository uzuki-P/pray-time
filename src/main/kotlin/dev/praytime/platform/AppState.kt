package dev.praytime.platform

import dev.praytime.domain.Region
import java.io.File
import java.time.Instant
import java.time.LocalDate

/**
 * Persists app state as a simple key=value file at $XDG_CONFIG_HOME/praytime/state.properties.
 * Values are region names, ISO dates, and comma-separated ISO instants - no quoting needed.
 */
object AppState {
    private val file: File by lazy {
        val configHome = System.getenv("XDG_CONFIG_HOME")?.takeIf { it.isNotBlank() }
            ?: (System.getProperty("user.home") + "/.config")
        val dir = File(configHome, "praytime")
        dir.mkdirs()
        File(dir, "state.properties")
    }

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
        writeField("completedDate", today.toString())
        writeField("completed", instants.joinToString(",") { it.toString() })
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
        cached = updated
        runCatching { file.writeText(updated.entries.joinToString("\n") { "${it.key}=${it.value}" }) }
    }
}
