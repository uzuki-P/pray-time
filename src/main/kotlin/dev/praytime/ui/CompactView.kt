package dev.praytime.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.praytime.calculation.DayPrayerTimes
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

internal val prayerColors = mapOf(
    "Fajr" to Color(0xFF9D8CFF),
    "Sunrise" to Color(0xFFFFC24D),
    "Dhuhr" to Color(0xFF4DA3FF),
    "Asr" to Color(0xFFFF8A5C),
    "Maghrib" to Color(0xFFFF5C8A),
    "Isha" to Color(0xFF5A6FE0),
)

internal fun minuteFraction(instant: Instant, zone: ZoneId): Float =
    LocalTime.ofInstant(instant, zone).toSecondOfDay() / 86_400f

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

internal data class SkyTheme(val colors: List<Color>)

internal fun skyTheme(day: DayPrayerTimes, now: Instant): SkyTheme {
    val byName = day.times.associateBy { it.name }
    fun before(name: String): Boolean = now < byName.getValue(name).instant
    return when {
        before("Fajr") || !before("Isha") -> SkyTheme(listOf(Color(0xFF12122E), Color(0xFF232052)))
        before("Sunrise") -> SkyTheme(listOf(Color(0xFF3A2B6E), Color(0xFFF98A5E)))
        before("Dhuhr") -> SkyTheme(listOf(Color(0xFF1F5FB0), Color(0xFF6FB1F2)))
        before("Maghrib") -> SkyTheme(listOf(Color(0xFF2E7BC8), Color(0xFFF7C46C)))
        else -> SkyTheme(listOf(Color(0xFF54277F), Color(0xFFF2665E)))
    }
}

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
    val sky = skyTheme(dayTimes, now)
    val latestDue = latestDuePrayer(dayTimes, completed, now)
    val palette = LocalAppPalette.current
    val shape = RoundedCornerShape(20.dp)

    Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Column(
            modifier = Modifier
                .width(272.dp)
                .shadow(6.dp, shape)
                .clip(shape)
                .background(palette.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(headerDrag)
                    .background(Brush.verticalGradient(sky.colors))
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 10.dp),
            ) {
                if (next != null) {
                    Text(
                        "NEXT PRAYER",
                        style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp),
                        color = Color.White.copy(alpha = 0.7f),
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            next.name,
                            style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold),
                            color = Color.White,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            compactRemaining(countdown.timeRemaining),
                            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.padding(bottom = 3.dp),
                        )
                    }
                }
                SunArcCanvas(
                    day = dayTimes,
                    now = now,
                    modifier = Modifier.fillMaxWidth().height(64.dp).padding(top = 10.dp),
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                dayTimes.times.forEach { prayer ->
                    val done = prayer.instant in completed
                    val isNext = prayer == next
                    Row(
                        modifier = Modifier.fillMaxWidth().height(26.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.size(8.dp).clip(CircleShape).background(prayerColors.getValue(prayer.name)),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            prayer.name,
                            style = TextStyle(fontSize = 13.sp, fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium),
                            color = palette.onSurface.copy(alpha = if (done) 0.4f else 1f),
                            textDecoration = if (done) TextDecoration.LineThrough else null,
                            modifier = Modifier.weight(1f),
                        )
                        if (prayer == latestDue) {
                            Text(
                                "DUE",
                                style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Black),
                                color = Color(0xFFFFC24D),
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            prayer.displayTime,
                            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                            color = palette.onSurface.copy(alpha = if (done) 0.4f else 0.9f),
                        )
                        Spacer(Modifier.width(8.dp))
                        CheckCircle(
                            done = done,
                            enabled = prayer.isPrayer,
                            size = 16.dp,
                            iconSize = 10.sp,
                        ) {
                            viewModel.markPrayed(prayer, !done)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenSettings)
                    .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    dayTimes.region.displayName,
                    style = TextStyle(fontSize = 10.sp),
                    color = palette.onSurface.copy(alpha = 0.45f),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "SETTINGS",
                    style = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                    color = palette.accent,
                )
            }
        }
    }
}

@Composable
internal fun CheckCircle(
    done: Boolean,
    size: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.TextUnit,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val prayedGreen = Color(0xFF3EDC81)
    val palette = LocalAppPalette.current
    val pop = remember { Animatable(1f) }
    val burst = remember { Animatable(0f) }
    var wasDone by remember { mutableStateOf(done) }
    LaunchedEffect(done) {
        if (done && !wasDone) {
            pop.snapTo(0.55f)
            burst.snapTo(0f)
            launch { pop.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMedium)) }
            burst.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
        }
        wasDone = done
    }
    val fill by animateColorAsState(
        targetValue = when {
            done -> prayedGreen
            !enabled -> palette.onSurface.copy(alpha = 0.06f)
            else -> Color.Transparent
        },
        label = "checkFill",
    )
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        if (burst.value > 0f && burst.value < 1f) {
            Box(
                modifier = Modifier
                    .size(size * (1f + 0.9f * burst.value))
                    .border(
                        width = (1.5f * (1f - burst.value)).dp,
                        color = prayedGreen.copy(alpha = 1f - burst.value),
                        shape = CircleShape,
                    ),
            )
        }
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                }
                .clip(CircleShape)
                .background(fill)
                .border(1.5.dp, palette.onSurface.copy(alpha = if (enabled) 0.3f else 0.12f), CircleShape)
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            when {
                done -> Text(
                    "✓",
                    style = TextStyle(fontSize = iconSize, fontWeight = FontWeight.Black),
                    color = Color(0xFF0B2E1B),
                )
                !enabled -> Box(
                    modifier = Modifier
                        .size(width = 6.dp, height = 1.5.dp)
                        .background(palette.onSurface.copy(alpha = 0.25f)),
                )
            }
        }
    }
}

@Composable
internal fun SunArcCanvas(day: DayPrayerTimes, now: Instant, modifier: Modifier = Modifier) {
    val zone = ZoneId.of(day.region.timezoneId)
    val byName = day.times.associateBy { it.name }
    Canvas(modifier) {
        val baseline = size.height * 0.84f
        val rx = size.width * 0.44f
        val ry = size.height * 0.68f
        val cx = size.width / 2f
        fun point(fraction: Float): Offset {
            val theta = Math.PI * (1.0 - fraction)
            return Offset(cx + (rx * cos(theta)).toFloat(), baseline - (ry * sin(theta)).toFloat())
        }
        drawLine(
            color = Color.White.copy(alpha = 0.35f),
            start = Offset(0f, baseline),
            end = Offset(size.width, baseline),
            strokeWidth = 1.dp.toPx(),
        )
        drawArc(
            color = Color.White.copy(alpha = 0.25f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(cx - rx, baseline - ry),
            size = Size(rx * 2f, ry * 2f),
            style = Stroke(width = 1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 6f))),
        )
        day.times.forEach { prayer ->
            drawCircle(
                color = prayerColors.getValue(prayer.name),
                radius = 3.dp.toPx(),
                center = point(minuteFraction(prayer.instant, zone)),
            )
        }
        val sunrise = byName.getValue("Sunrise").instant
        val sunset = byName.getValue("Maghrib").instant
        val sunColor = if (now >= sunrise && now <= sunset) Color(0xFFFFD54A) else Color(0xFFD9E2FF)
        val position = point(minuteFraction(now, zone))
        drawCircle(color = sunColor.copy(alpha = 0.22f), radius = 11.dp.toPx(), center = position)
        drawCircle(color = sunColor, radius = 5.dp.toPx(), center = position)
    }
}
