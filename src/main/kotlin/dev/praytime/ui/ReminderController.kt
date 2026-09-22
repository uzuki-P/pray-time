package dev.praytime.ui

import dev.praytime.calculation.DayPrayerTimes
import dev.praytime.calculation.PrayerTime
import java.time.Clock
import java.time.Duration
import java.time.Instant

internal fun latestDuePrayer(day: DayPrayerTimes, completed: Set<Instant>, now: Instant): PrayerTime? =
    day.times
        .filter { it.isPrayer && it.instant <= now && it.instant !in completed }
        .maxByOrNull { it.instant }

class ReminderController(
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    var visible: Boolean = false
        private set
    var current: PrayerTime? = null
        private set

    private var dismissedPermanently: Set<Instant> = emptySet()
    private var snoozedUntil: Instant = Instant.EPOCH

    fun onTick(latestDue: PrayerTime?, reminderEnabled: Boolean) {
        val now = clock.instant()
        if (latestDue == null) {
            visible = false
            current = null
            dismissedPermanently = emptySet()
            snoozedUntil = Instant.EPOCH
            return
        }
        if (current != latestDue) {
            current = latestDue
            visible = true
            return
        }
        if (!visible &&
            reminderEnabled &&
            latestDue.instant !in dismissedPermanently &&
            !now.isBefore(snoozedUntil)
        ) {
            visible = true
        }
    }

    fun dismiss(reminderEnabled: Boolean, reminderMinutes: Int = DEFAULT_REMINDER_MINUTES) {
        visible = false
        if (reminderEnabled) {
            snoozedUntil = clock.instant().plus(Duration.ofMinutes(reminderMinutes.toLong()))
        } else {
            current?.let { dismissedPermanently += it.instant }
        }
    }

    companion object {
        const val DEFAULT_REMINDER_MINUTES = 10
    }
}
