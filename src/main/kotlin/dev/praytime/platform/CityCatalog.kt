package dev.praytime.platform

import dev.praytime.domain.Region
import java.io.File
import java.sql.DriverManager

/**
 * Worldwide city list stored in SQLite, extracted to the state folder on first use.
 * Queries run entirely inside SQLite; the database is never held in memory.
 */
object CityCatalog {
    private const val RESOURCE = "/data/cities.db"
    private const val LEGACY_TSV = "cities_world.tsv"
    private const val DEFAULT_LIMIT = 50
    private val file: File get() = File(AppState.dir, "cities.db")

    fun search(query: String, limit: Int = DEFAULT_LIMIT): List<Region> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        ensureExtracted()
        if (!file.exists()) return emptyList()
        val contains = "%${escapeLike(q)}%"
        val prefix = "${escapeLike(q)}%"
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { connection ->
            connection.prepareStatement(
                """
                SELECT name, admin, country, latitude, longitude, timezone
                FROM cities
                WHERE lower(name) LIKE ?1 ESCAPE '\' OR lower(admin) LIKE ?1 ESCAPE '\' OR lower(country) LIKE ?1 ESCAPE '\'
                ORDER BY (lower(name) LIKE ?2 ESCAPE '\') DESC, lower(name)
                LIMIT ?3
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, contains)
                statement.setString(2, prefix)
                statement.setInt(3, limit)
                statement.executeQuery().use { rows ->
                    val regions = ArrayList<Region>(limit)
                    while (rows.next()) {
                        regions.add(
                            Region(
                                name = rows.getString(1),
                                adminName = rows.getString(2),
                                country = rows.getString(3),
                                latitude = rows.getDouble(4),
                                longitude = rows.getDouble(5),
                                timezoneId = rows.getString(6),
                            ),
                        )
                    }
                    return regions
                }
            }
        }
    }

    private fun ensureExtracted() {
        if (file.exists()) {
            File(AppState.dir, LEGACY_TSV).delete()
            return
        }
        runCatching {
            val tmp = File(AppState.dir, file.name + ".tmp")
            CityCatalog::class.java.getResourceAsStream(RESOURCE)?.use { input ->
                tmp.outputStream().use { input.copyTo(it) }
            } ?: return
            if (tmp.length() > 0L) {
                if (!tmp.renameTo(file)) {
                    file.writeBytes(tmp.readBytes())
                    tmp.delete()
                }
            } else {
                tmp.delete()
            }
        }
        File(AppState.dir, LEGACY_TSV).delete()
    }

    private fun escapeLike(value: String): String =
        value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}
