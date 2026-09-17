package dev.praytime.ui

import dev.praytime.domain.medanJohor
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class MutableClock(
    private var current: Instant,
    private val zoneId: ZoneId,
) : Clock() {
    override fun getZone(): ZoneId = zoneId
    override fun withZone(zone: ZoneId): Clock = MutableClock(current, zone)
    override fun instant(): Instant = current
    fun set(value: Instant) {
        current = value
    }
}

private val Duration.isPositive: Boolean
    get() = !isNegative && !isZero

class ScheduleViewModelTest {

    private val zone = ZoneId.of(medanJohor.timezoneId)

    // 2026-09-17 09:00 WIB: morning, after Fajr and Sunrise.
    private val morning = Instant.parse("2026-09-17T02:00:00Z")
    // 2026-09-18 01:00 WIB: past Isha of Sep 17.
    private val lateNight = Instant.parse("2026-09-17T18:00:00Z")

    private fun viewModel(at: Instant): Pair<MutableClock, ScheduleViewModel> {
        val clock = MutableClock(at, zone)
        return clock to ScheduleViewModel(clock)
    }

    @Test
    fun countdownTargetsNextPrayerWithPositiveRemaining() {
        val (_, viewModel) = viewModel(morning)
        val state = viewModel.countdown.value
        assertEquals("Dhuhr", state.nextPrayer?.name)
        assertTrue(state.timeRemaining!!.isPositive)
    }

    @Test
    fun countdownSkipsSunrise() {
        val (_, viewModel) = viewModel(morning)
        assertTrue(viewModel.countdown.value.nextPrayer!!.isPrayer)
    }

    @Test
    fun afterIshaNextIsTomorrowFajr() {
        val (_, viewModel) = viewModel(lateNight)
        val state = viewModel.countdown.value
        assertEquals("Fajr", state.nextPrayer?.name)
        assertTrue(state.timeRemaining!!.isPositive)
    }

    @Test
    fun displayRemainingFormatsHoursMinutesSeconds() {
        val (_, viewModel) = viewModel(morning)
        val display = viewModel.countdown.value.displayRemaining
        assertTrue(Regex("""\d+h \d{2}m \d{2}s""").matches(display!!), display)
    }

    @Test
    fun tickRollsOverToNextDay() {
        val (clock, viewModel) = viewModel(morning)
        val firstDate = viewModel.dayTimes.value.date
        clock.set(lateNight)
        viewModel.tick()
        assertEquals(firstDate.plusDays(1), viewModel.dayTimes.value.date)
        assertEquals("Fajr", viewModel.countdown.value.nextPrayer?.name)
    }

    @Test
    fun tickRollsBackwardsAcrossDays() {
        val (clock, viewModel) = viewModel(lateNight)
        clock.set(morning)
        viewModel.tick()
        assertEquals(
            lateNight.atZone(zone).toLocalDate().minusDays(1),
            viewModel.dayTimes.value.date,
        )
    }

    @Test
    fun markPrayedAcceptsPastPrayer() {
        val (_, viewModel) = viewModel(morning)
        val fajr = viewModel.dayTimes.value.times.first { it.name == "Fajr" }
        viewModel.markPrayed(fajr, true)
        assertTrue(fajr.instant in viewModel.completed.value)
        viewModel.markPrayed(fajr, false)
        assertFalse(fajr.instant in viewModel.completed.value)
    }

    @Test
    fun markPrayedRejectsFuturePrayer() {
        val (_, viewModel) = viewModel(morning)
        val isha = viewModel.dayTimes.value.times.first { it.name == "Isha" }
        viewModel.markPrayed(isha, true)
        assertFalse(isha.instant in viewModel.completed.value)
    }

    @Test
    fun markPrayedRejectsSunrise() {
        val (_, viewModel) = viewModel(morning)
        val sunrise = viewModel.dayTimes.value.times.first { it.name == "Sunrise" }
        viewModel.markPrayed(sunrise, true)
        assertFalse(sunrise.instant in viewModel.completed.value)
    }

    @Test
    fun completedClearsOnDateRollover() {
        val (clock, viewModel) = viewModel(morning)
        val fajr = viewModel.dayTimes.value.times.first { it.name == "Fajr" }
        viewModel.markPrayed(fajr, true)
        clock.set(lateNight)
        viewModel.tick()
        assertTrue(viewModel.completed.value.isEmpty())
    }

    @Test
    fun regionDefaultsToMedanJohor() {
        val (_, viewModel) = viewModel(morning)
        assertEquals(medanJohor, viewModel.region)
        assertEquals(medanJohor, viewModel.dayTimes.value.region)
    }
}
