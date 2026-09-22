package dev.praytime.ui

import dev.praytime.calculation.DayPrayerTimes
import dev.praytime.calculation.PrayerTime
import dev.praytime.domain.Region
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class TickClock(
    private var current: Instant,
    private val zoneId: ZoneId,
) : Clock() {
    override fun getZone(): ZoneId = zoneId
    override fun withZone(zone: ZoneId): Clock = TickClock(current, zone)
    override fun instant(): Instant = current
    fun advanceBy(duration: Duration) {
        current = current.plus(duration)
    }
}

class ReminderControllerTest {

    private val clock = TickClock(Instant.parse("2026-09-21T10:00:00Z"), ZoneId.of("UTC"))
    private val asr = PrayerTime("Asr", Instant.parse("2026-09-21T06:30:00Z"), "UTC")
    private val maghrib = PrayerTime("Maghrib", Instant.parse("2026-09-21T12:00:00Z"), "UTC")

    private fun controller(): ReminderController = ReminderController(clock = clock)

    @Test
    fun showsNotificationWhenPrayerBecomesDue() {
        val controller = controller()
        controller.onTick(asr, reminderEnabled = true)
        assertTrue(controller.visible)
        assertEquals(asr, controller.current)
    }

    @Test
    fun hidesAndResetsWhenPrayerMarkedPrayed() {
        val controller = controller()
        controller.onTick(asr, reminderEnabled = true)
        controller.onTick(null, reminderEnabled = true)
        assertFalse(controller.visible)
        assertNull(controller.current)
    }

    @Test
    fun dismissUsesDefaultTenMinuteSnooze() {
        val controller = controller()
        controller.onTick(asr, reminderEnabled = true)
        controller.dismiss(reminderEnabled = true)
        assertFalse(controller.visible)
        clock.advanceBy(Duration.ofMinutes(9))
        controller.onTick(asr, reminderEnabled = true)
        assertFalse(controller.visible)
        clock.advanceBy(Duration.ofMinutes(1))
        controller.onTick(asr, reminderEnabled = true)
        assertTrue(controller.visible)
    }

    @Test
    fun dismissUsesCustomMinutesSnooze() {
        val controller = controller()
        controller.onTick(asr, reminderEnabled = true)
        controller.dismiss(reminderEnabled = true, reminderMinutes = 2)
        assertFalse(controller.visible)
        clock.advanceBy(Duration.ofMinutes(1))
        controller.onTick(asr, reminderEnabled = true)
        assertFalse(controller.visible)
        clock.advanceBy(Duration.ofMinutes(1))
        controller.onTick(asr, reminderEnabled = true)
        assertTrue(controller.visible)
    }

    @Test
    fun dismissStaysHiddenWhenReminderDisabled() {
        val controller = controller()
        controller.onTick(asr, reminderEnabled = true)
        controller.dismiss(reminderEnabled = false)
        clock.advanceBy(Duration.ofHours(1))
        controller.onTick(asr, reminderEnabled = true)
        assertFalse(controller.visible)
    }

    @Test
    fun newDuePrayerShowsImmediatelyWhileSnoozed() {
        val controller = controller()
        controller.onTick(asr, reminderEnabled = true)
        controller.dismiss(reminderEnabled = true)
        controller.onTick(maghrib, reminderEnabled = true)
        assertTrue(controller.visible)
        assertEquals(maghrib, controller.current)
    }

    @Test
    fun unmarkingPrayedPrayerShowsNotificationAgain() {
        val controller = controller()
        controller.onTick(asr, reminderEnabled = true)
        controller.onTick(null, reminderEnabled = true)
        controller.onTick(asr, reminderEnabled = true)
        assertTrue(controller.visible)
        assertEquals(asr, controller.current)
    }

    @Test
    fun reminderSettingChangeAppliesOnNextTick() {
        val controller = controller()
        controller.onTick(asr, reminderEnabled = false)
        controller.dismiss(reminderEnabled = false)
        controller.onTick(asr, reminderEnabled = false)
        assertFalse(controller.visible)
    }

    @Test
    fun latestDuePrayerSkipsSunriseCompletedAndFuture() {
        val region = Region("Test", "Test", "Test", 0.0, 0.0, "UTC")
        val fajr = PrayerTime("Fajr", Instant.parse("2026-09-21T00:00:00Z"), "UTC")
        val sunrise = PrayerTime("Sunrise", Instant.parse("2026-09-21T01:00:00Z"), "UTC")
        val dhuhr = PrayerTime("Dhuhr", Instant.parse("2026-09-21T03:00:00Z"), "UTC")
        val asrLater = PrayerTime("Asr", Instant.parse("2026-09-21T06:30:00Z"), "UTC")
        val day = DayPrayerTimes(LocalDate.parse("2026-09-21"), region, listOf(fajr, sunrise, dhuhr, asrLater))
        val now = Instant.parse("2026-09-21T04:00:00Z")
        assertEquals(dhuhr, latestDuePrayer(day, completed = emptySet(), now = now))
        assertEquals(fajr, latestDuePrayer(day, completed = setOf(dhuhr.instant), now = now))
        assertNull(latestDuePrayer(day, completed = setOf(fajr.instant, dhuhr.instant), now = now))
        assertEquals(asr, latestDuePrayer(day, completed = setOf(fajr.instant, dhuhr.instant), now = asr.instant))
    }
}
