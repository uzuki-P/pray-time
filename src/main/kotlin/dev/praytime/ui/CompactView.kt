package dev.praytime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.praytime.calculation.DayPrayerTimes
import java.time.Duration
import java.time.Instant

internal fun compactRemaining(remaining: Duration?): String {
    val total = (remaining?.seconds ?: 0).coerceAtLeast(0)
    val h = total / 3600
    val m = total % 3600 / 60
    val s = total % 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m ${s}s"
        else -> "${s}s"
    }
}

/**
 * Where `now` sits along the schedule, in row units: 0 is the first row's centre,
 * 1.5 is halfway between the second and third rows. Clamped to the first and last rows.
 */
internal fun railPosition(day: DayPrayerTimes, now: Instant): Float {
    val times = day.times
    if (now <= times.first().instant) return 0f
    if (now >= times.last().instant) return (times.size - 1).toFloat()
    val index = times.indexOfLast { it.instant <= now }
    val start = times[index].instant.epochSecond
    val end = times[index + 1].instant.epochSecond
    return index + (now.epochSecond - start).toFloat() / (end - start)
}

private val RowHeight = 34.dp
private val RailInset = 11.dp
private val ListPadding = 6.dp

@Composable
fun CompactView(
    viewModel: ScheduleViewModel,
    headerDrag: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
) {
    val dayTimes by viewModel.dayTimes.collectAsState()
    val countdown by viewModel.countdown.collectAsState()
    val completed by viewModel.completed.collectAsState()
    val now = viewModel.now()
    val next = countdown.nextPrayer
    val latestDue = latestDuePrayer(dayTimes, completed, now)
    val palette = LocalAppPalette.current
    val type = LocalAppType.current
    val shape = RoundedCornerShape(18.dp)

    Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Column(
            modifier = Modifier
                .width(272.dp)
                .shadow(8.dp, shape)
                .clip(shape)
                .background(palette.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(headerDrag)
                    .background(Brush.verticalGradient(listOf(skyColors.getValue(next?.name ?: "Isha").copy(alpha = 0.2f), palette.surface)))
                    .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 12.dp),
            ) {
                if (next != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Next prayer", style = type.body(12.sp, FontWeight.Medium), color = palette.muted)
                        Spacer(Modifier.weight(1f))
                        Text(arabicNames.getValue(next.name), style = type.display(15.sp, FontWeight.Medium), color = palette.muted)
                    }
                    Text(next.name, style = type.display(32.sp), color = palette.onSurface)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "in ${compactRemaining(countdown.timeRemaining)}",
                            style = type.figures(15.sp, FontWeight.SemiBold),
                            color = palette.accent,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("at ${next.displayTime}", style = type.figures(13.sp), color = palette.muted)
                    }
                }
            }

            val railPos = railPosition(dayTimes, now)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = ListPadding)
                    .drawWithContent {
                        drawContent()
                        drawRail(dayTimes, railPos, palette)
                    },
            ) {
                dayTimes.times.forEach { prayer ->
                    val done = prayer.instant in completed
                    val isNext = prayer == next
                    val isDue = prayer == latestDue
                    val arrived = prayer.instant <= now
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(RowHeight)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDue) palette.due.copy(alpha = 0.12f) else Color.Transparent)
                            .padding(start = RailInset * 2 + 6.dp, end = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (prayer.isPrayer) {
                            Text(
                                prayer.name,
                                style = type.display(16.sp, if (isNext || isDue) FontWeight.Bold else FontWeight.Medium),
                                color = when {
                                    done -> palette.faint
                                    isNext -> palette.accent
                                    else -> palette.onSurface
                                },
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            Text(prayer.name, style = type.body(12.sp), color = palette.faint, modifier = Modifier.weight(1f))
                        }
                        if (isDue) {
                            Text("Due", style = type.body(11.sp, FontWeight.Bold), color = palette.due)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            prayer.displayTime,
                            style = type.figures(if (prayer.isPrayer) 14.sp else 12.sp, if (isNext || isDue) FontWeight.SemiBold else FontWeight.Medium),
                            color = when {
                                !prayer.isPrayer || done -> palette.faint
                                isNext -> palette.accent
                                else -> palette.muted
                            },
                        )
                        Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.CenterEnd) {
                            if (prayer.isPrayer) {
                                PrayerCheck(
                                    done = done,
                                    due = isDue,
                                    enabled = arrived,
                                    onClick = { viewModel.markPrayed(prayer, !done) },
                                )
                            }
                        }
                    }
                }
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(palette.divider))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pressable(onClick = onOpenSettings)
                    .padding(start = 18.dp, end = 18.dp, top = 9.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    dayTimes.region.displayName,
                    style = type.body(12.sp),
                    color = palette.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text("Settings", style = type.body(12.sp, FontWeight.SemiBold), color = palette.accent)
            }
        }
    }
}

/**
 * The schedule rail: a line through every row, coloured with the sky of each period up to
 * `now` and faint after it, with a node per schedule time and a marker at `now`.
 */
private fun DrawScope.drawRail(day: DayPrayerTimes, railPos: Float, palette: AppPalette) {
    val x = RailInset.toPx()
    val rowHeight = RowHeight.toPx()
    fun y(position: Float) = (position + 0.5f) * rowHeight
    val colors = day.times.map { skyColors.getValue(it.name) }
    val nowY = y(railPos)

    drawLine(palette.divider, Offset(x, nowY), Offset(x, y(day.times.lastIndex.toFloat())), 2.dp.toPx(), StrokeCap.Round)
    for (i in 0 until day.times.lastIndex) {
        if (railPos <= i) break
        val top = y(i.toFloat())
        val bottom = y((i + 1).toFloat())
        drawLine(
            brush = Brush.verticalGradient(listOf(colors[i], colors[i + 1]), startY = top, endY = bottom),
            start = Offset(x, top),
            end = Offset(x, minOf(bottom, nowY)),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
    day.times.forEachIndexed { index, prayer ->
        val center = Offset(x, y(index.toFloat()))
        val radius = (if (prayer.isPrayer) 5.dp else 3.5.dp).toPx()
        if (index <= railPos) {
            drawCircle(colors[index], radius, center)
        } else {
            drawCircle(palette.surface, radius, center)
            drawCircle(colors[index], radius - 1.dp.toPx(), center, style = Stroke(2.dp.toPx()))
        }
    }
    val atNode = railPos == railPos.toInt().toFloat()
    if (!atNode) {
        val half = 5.dp.toPx()
        drawLine(palette.surface, Offset(x - half - 1.dp.toPx(), nowY), Offset(x + half + 1.dp.toPx(), nowY), 4.dp.toPx(), StrokeCap.Round)
        drawLine(palette.onSurface, Offset(x - half, nowY), Offset(x + half, nowY), 2.dp.toPx(), StrokeCap.Round)
    }
}
