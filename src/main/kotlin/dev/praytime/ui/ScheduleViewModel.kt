package dev.praytime.ui

import dev.praytime.calculation.DayPrayerTimes
import dev.praytime.calculation.PrayerTime
import dev.praytime.calculation.PrayerTimeCalculator
import dev.praytime.domain.Region
import dev.praytime.domain.medanJohor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

data class CountdownState(
    val nextPrayer: PrayerTime?,
    val timeRemaining: Duration?,
) {
    val displayRemaining: String?
        get() = timeRemaining?.let {
            val seconds = it.seconds.coerceAtLeast(0)
            String.format(
                Locale.ROOT,
                "%dh %02dm %02ds",
                seconds / 3600,
                seconds / 60 % 60,
                seconds % 60,
            )
        }
}

class ScheduleViewModel(
    private val clock: Clock = Clock.systemDefaultZone(),
    region: Region = medanJohor,
) {
    var region: Region = region
        private set
    private val zone: ZoneId get() = ZoneId.of(region.timezoneId)
    private val _dayTimes = MutableStateFlow(PrayerTimeCalculator.calculate(localDate(), region))
    val dayTimes = _dayTimes.asStateFlow()
    private var tomorrow = PrayerTimeCalculator.calculate(localDate().plusDays(1), region)
    private val _countdown = MutableStateFlow(CountdownState(null, null))
    val countdown = _countdown.asStateFlow()
    private val _completed = MutableStateFlow<Set<Instant>>(emptySet())
    val completed = _completed.asStateFlow()

    init {
        tick()
    }

    fun now(): Instant = clock.instant()

    fun restoreCompleted(instants: Set<Instant>) {
        val todayInstants = _dayTimes.value.times.map { it.instant }.toSet()
        _completed.value = instants.filterTo(mutableSetOf()) { it in todayInstants }
    }

    fun setRegion(value: Region) {
        if (value == region) return
        region = value
        val date = localDate()
        _dayTimes.value = PrayerTimeCalculator.calculate(date, region)
        tomorrow = PrayerTimeCalculator.calculate(date.plusDays(1), region)
        _completed.value = emptySet()
        tick()
    }

    suspend fun run() {
        while (true) {
            tick()
            delay(1_000)
        }
    }

    fun tick() {
        val now = clock.instant()
        val date = LocalDate.ofInstant(now, zone)
        if (_dayTimes.value.date != date) {
            _dayTimes.value = PrayerTimeCalculator.calculate(date, region)
            tomorrow = PrayerTimeCalculator.calculate(date.plusDays(1), region)
            _completed.value = emptySet()
        }
        val next = _dayTimes.value.next(now) ?: tomorrow.next(now)
        _countdown.value = CountdownState(next, next?.let { Duration.between(now, it.instant) })
    }

    fun markPrayed(prayer: PrayerTime, prayed: Boolean) {
        if (!prayer.isPrayer || prayer.instant.isAfter(clock.instant()) || prayer !in _dayTimes.value.times) return
        _completed.value = if (prayed) _completed.value + prayer.instant else _completed.value - prayer.instant
    }

    private fun localDate(): LocalDate = LocalDate.ofInstant(clock.instant(), zone)
}
