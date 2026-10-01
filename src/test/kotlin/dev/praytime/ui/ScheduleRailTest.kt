package dev.praytime.ui

import dev.praytime.calculation.PrayerTimeCalculator
import dev.praytime.domain.medanJohor
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ScheduleRailTest {
    private val day = PrayerTimeCalculator.calculate(LocalDate.of(2026, 9, 17), medanJohor)
    private fun at(name: String) = day.times.first { it.name == name }

    @Test
    fun railClampsBeforeFirstAndAfterLastTime() {
        assertEquals(0f, railPosition(day, at("Fajr").instant.minusSeconds(3_600)))
        assertEquals(day.times.lastIndex.toFloat(), railPosition(day, at("Isha").instant.plusSeconds(3_600)))
    }

    @Test
    fun railSitsOnANodeAtItsTime() {
        assertEquals(day.times.indexOf(at("Asr")).toFloat(), railPosition(day, at("Asr").instant))
    }

    @Test
    fun railInterpolatesBetweenTimes() {
        val asr = at("Asr").instant
        val maghrib = at("Maghrib").instant
        val midway = asr.plusSeconds((maghrib.epochSecond - asr.epochSecond) / 2)
        assertEquals(day.times.indexOf(at("Asr")) + 0.5f, railPosition(day, midway), 0.01f)
    }

    @Test
    fun dueDetailDescribesElapsedTime() {
        val asr = at("Asr")
        assertEquals("Due since ${asr.displayTime} · just now", dueDetail(asr, asr.instant.plusSeconds(30)))
        assertEquals("Due since ${asr.displayTime} · 48 min ago", dueDetail(asr, asr.instant.plusSeconds(48 * 60)))
        assertEquals("Due since ${asr.displayTime} · 2h 5m ago", dueDetail(asr, asr.instant.plusSeconds(125 * 60)))
        assertEquals("Starts at ${asr.displayTime}", dueDetail(asr, asr.instant.minusSeconds(60)))
    }
}
