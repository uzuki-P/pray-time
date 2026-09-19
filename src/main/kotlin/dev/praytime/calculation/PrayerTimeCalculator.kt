package dev.praytime.calculation

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import dev.praytime.domain.Region
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class DayPrayerTimes(
    val date: LocalDate,
    val region: Region,
    val times: List<PrayerTime>,
) {
    fun next(from: Instant): PrayerTime? =
        times.firstOrNull { it.isPrayer && it.instant.isAfter(from) }
}

data class PrayerTime(
    val name: String,
    val instant: Instant,
    val timezoneId: String,
) {
    val localTime: LocalTime
        get() = LocalTime.ofInstant(instant, ZoneId.of(timezoneId))

    val displayTime: String
        get() = localTime.toString().take(5)

    val isPrayer: Boolean
        get() = name != "Sunrise"
}

object PrayerTimeCalculator {
    fun calculate(date: LocalDate, region: Region): DayPrayerTimes {
        val coordinates = Coordinates(region.latitude, region.longitude)
        val dateComponents = DateComponents(date.year, date.monthValue, date.dayOfMonth)
        val params = CalculationMethod.SINGAPORE.getParameters().apply {
            madhab = Madhab.SHAFI
        }
        val times = PrayerTimes(coordinates, dateComponents, params)
        val zone = ZoneId.of(region.timezoneId)

        val entries = listOf(
            "Fajr" to times.fajr,
            "Sunrise" to times.sunrise,
            "Dhuhr" to times.dhuhr,
            "Asr" to times.asr,
            "Maghrib" to times.maghrib,
            "Isha" to times.isha,
        )
        return DayPrayerTimes(
            date = date,
            region = region,
            // adhan's instants carry millisecond jitter between runs, so truncate to
            // minutes - completed-prayer identity must survive restarts.
            times = entries.map { (name, javaDate) ->
                PrayerTime(name, javaDate.toInstant().truncatedTo(ChronoUnit.MINUTES), region.timezoneId)
            }.sortedBy { it.instant },
        )
    }
}
