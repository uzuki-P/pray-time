package dev.praytime.calculation

import dev.praytime.domain.medanJohor
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PrayerTimeCalculatorTest {

    private val zone = ZoneId.of(medanJohor.timezoneId)
    private val day = PrayerTimeCalculator.calculate(LocalDate.of(2026, 9, 17), medanJohor)

    @Test
    fun returnsSixEntriesInOrder() {
        assertEquals(
            listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"),
            day.times.map { it.name },
        )
        assertEquals(day.times.sortedBy { it.instant }, day.times)
    }

    @Test
    fun timesFallOnRequestedLocalDate() {
        day.times.forEach { time ->
            assertEquals(LocalDate.of(2026, 9, 17), time.instant.atZone(zone).toLocalDate())
        }
    }

    @Test
    fun displayTimeIsHourMinute() {
        day.times.forEach { time ->
            assertTrue(Regex("""\d{2}:\d{2}""").matches(time.displayTime), time.displayTime)
        }
    }

    @Test
    fun nextSkipsSunrise() {
        val sunrise = day.times.first { it.name == "Sunrise" }
        assertEquals("Dhuhr", day.next(sunrise.instant)?.name)
    }

    @Test
    fun nextAfterIshaIsNull() {
        val isha = day.times.first { it.name == "Isha" }
        assertNull(day.next(isha.instant))
    }

    @Test
    fun fajrIsBeforeSunriseAndMaghribIsBeforeIsha() {
        val names = day.times.map { it.name }
        assertTrue(names.indexOf("Fajr") < names.indexOf("Sunrise"))
        assertTrue(names.indexOf("Maghrib") < names.indexOf("Isha"))
    }
}
