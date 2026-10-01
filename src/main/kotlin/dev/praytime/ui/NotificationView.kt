package dev.praytime.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.praytime.calculation.PrayerTime
import java.time.Duration
import java.time.Instant
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlinx.coroutines.launch

internal fun dueDetail(prayer: PrayerTime, now: Instant): String {
    if (now < prayer.instant) return "Starts at ${prayer.displayTime}"
    val minutes = Duration.between(prayer.instant, now).toMinutes()
    val ago = when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        else -> "${minutes / 60}h ${minutes % 60}m ago"
    }
    return "Due since ${prayer.displayTime} · $ago"
}

/** Waits on the animation clock, so holds stay in step with the motion around them. */
private suspend fun pause(millis: Int) = animate(0f, 1f, animationSpec = tween(millis)) { _, _ -> }

/** Extra room below the card so the snooze droop and sink are not cut off by the window edge. */
val NotificationDroopRoom = 28.dp

/**
 * The due reminder card. With `snoozeMinutes` set, the second button snoozes for that long;
 * with null it dismisses until the next prayer.
 *
 * Marking as prayed floods the card teal from the button with a confetti burst and a drawn
 * check before it lifts away. Snoozing shakes its head, drops on one corner, greys out, and sinks.
 */
@Composable
fun PrayerNotification(
    prayer: PrayerTime,
    now: Instant,
    snoozeMinutes: Int?,
    onPrayed: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    val palette = LocalAppPalette.current
    val type = LocalAppType.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var snoozed by remember { mutableStateOf(false) }

    val prayedPress = remember { Animatable(1f) }
    val snoozePress = remember { Animatable(1f) }
    // Mark as prayed.
    val flood = remember { Animatable(0f) }
    val confetti = remember { Animatable(0f) }
    val checkDraw = remember { Animatable(0f) }
    val bump = remember { Animatable(1f) }
    val lift = remember { Animatable(0f) }
    // Snooze.
    val shake = remember { Animatable(0f) }
    val droop = remember { Animatable(0f) }
    val grey = remember { Animatable(0f) }
    val sink = remember { Animatable(0f) }

    var cardOrigin by remember { mutableStateOf(Offset.Zero) }
    var burstCenter by remember { mutableStateOf(Offset.Zero) }

    val secondaryLabel = if (snoozeMinutes != null) "Snooze $snoozeMinutes min" else "Dismiss"
    val snoozedLabel = if (snoozeMinutes != null) "Okay… back in $snoozeMinutes min" else "Okay… until the next prayer"

    fun celebrate() {
        if (busy) return
        busy = true
        scope.launch {
            prayedPress.animateTo(0.92f, tween(80, easing = FastOutSlowInEasing))
            launch { prayedPress.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium)) }
            launch {
                pause(120)
                confetti.animateTo(1f, tween(900, easing = LinearOutSlowInEasing))
            }
            flood.animateTo(1f, tween(360, easing = FastOutSlowInEasing))
            launch {
                bump.animateTo(1.04f, tween(90, easing = FastOutSlowInEasing))
                bump.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
            }
            checkDraw.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
            pause(560)
            lift.animateTo(1f, tween(260, easing = FastOutLinearInEasing))
            busy = false
            onPrayed()
        }
    }

    fun sigh() {
        if (busy) return
        busy = true
        scope.launch {
            snoozePress.animateTo(0.95f, tween(80))
            launch { snoozePress.animateTo(1f, tween(160)) }
            shake.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 360
                    0f at 0
                    -5f at 60
                    5f at 130
                    -3f at 200
                    3f at 270
                    0f at 360
                },
            )
            snoozed = true
            launch { grey.animateTo(1f, tween(420)) }
            // A sign losing one nail: swings down from its top-left corner and wobbles to rest.
            droop.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessLow))
            pause(250)
            sink.animateTo(1f, tween(420, easing = FastOutLinearInEasing))
            busy = false
            onDismiss()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    val scale = bump.value * (1f - 0.08f * lift.value)
                    scaleX = scale
                    scaleY = scale
                    translationX = shake.value.dp.toPx()
                    translationY = (-6f * lift.value + 22f * sink.value).dp.toPx()
                    rotationZ = 3.5f * droop.value + 2f * sink.value
                    alpha = 1f - maxOf(lift.value, sink.value)
                    transformOrigin = if (droop.value > 0f) TransformOrigin(0.04f, 0f) else TransformOrigin.Center
                }
                .onGloballyPositioned { cardOrigin = it.boundsInRoot().topLeft },
        ) {
            Row(
                modifier = Modifier
                    .width(304.dp)
                    .height(IntrinsicSize.Min)
                    .shadow(8.dp, shape)
                    .clip(shape)
                    .background(palette.surface)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        if (grey.value > 0f) {
                            drawRect(Color.Gray.copy(alpha = grey.value), blendMode = BlendMode.Saturation)
                            drawRect(palette.surface.copy(alpha = 0.25f * grey.value))
                        }
                        if (flood.value > 0f) {
                            val radius = hypot(size.width, size.height) * flood.value
                            drawCircle(palette.prayed, radius, burstCenter)
                        }
                    },
            ) {
                Box(Modifier.width(4.dp).fillMaxHeight().background(skyColors.getValue(prayer.name)))
                Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(prayer.name, style = type.display(24.sp), color = palette.onSurface)
                        Spacer(Modifier.weight(1f))
                        Text(arabicNames.getValue(prayer.name), style = type.display(17.sp, FontWeight.Medium), color = palette.muted)
                    }
                    AnimatedContent(
                        targetState = snoozed,
                        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                        label = "detail",
                    ) { isSnoozed ->
                        Text(
                            if (isSnoozed) snoozedLabel else dueDetail(prayer, now),
                            style = type.figures(12.sp, FontWeight.Normal),
                            color = palette.muted,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillButton(
                            ButtonKind.Prayed,
                            onClick = ::celebrate,
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = prayedPress.value
                                    scaleY = prayedPress.value
                                }
                                .onGloballyPositioned { burstCenter = it.boundsInRoot().center - cardOrigin },
                        ) { color ->
                            CheckIcon(color, Modifier.size(14.dp), strokeWidth = 2.dp)
                            ButtonLabel("Mark as prayed", color)
                        }
                        PillButton(
                            ButtonKind.Quiet,
                            onClick = ::sigh,
                            modifier = Modifier.graphicsLayer {
                                scaleX = snoozePress.value
                                scaleY = snoozePress.value
                            },
                        ) { color ->
                            ButtonLabel(secondaryLabel, color)
                        }
                    }
                }
            }
            if (flood.value > 0f) {
                Column(
                    modifier = Modifier.matchParentSize().graphicsLayer { alpha = checkDraw.value.coerceAtLeast(0.001f) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    DrawingCheck(checkDraw.value, palette.onPrayed, Modifier.size(40.dp))
                    Text("${prayer.name} marked as prayed", style = type.display(16.sp), color = palette.onPrayed)
                }
            }
            if (confetti.value > 0f && confetti.value < 1f) {
                val colors = listOf(palette.accent, Color.White, skyColors.getValue(prayer.name), palette.due)
                Canvas(Modifier.matchParentSize()) {
                    drawConfetti(confetti.value, burstCenter, colors)
                }
            }
        }
    }
}

/** A check mark whose stroke draws in from its short arm as `progress` goes 0 to 1. */
@Composable
private fun DrawingCheck(progress: Float, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val path = Path().apply {
            moveTo(size.width * 0.18f, size.height * 0.53f)
            lineTo(size.width * 0.42f, size.height * 0.76f)
            lineTo(size.width * 0.84f, size.height * 0.28f)
        }
        val measure = PathMeasure().apply { setPath(path, false) }
        val partial = Path()
        measure.getSegment(0f, measure.length * progress, partial, true)
        drawPath(partial, color, style = Stroke(3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

private const val CONFETTI_PIECES = 22

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawConfetti(t: Float, origin: Offset, colors: List<Color>) {
    val travel = 1f - (1f - t) * (1f - t)
    for (i in 0 until CONFETTI_PIECES) {
        // Fixed per-piece spread so every burst looks the same, but uneven enough to feel loose.
        val angle = Math.toRadians(i * 360.0 / CONFETTI_PIECES + (i * 37 % 23) - 11)
        val distance = (52 + (i * 53 % 48)).dp.toPx() * travel
        val fall = 22.dp.toPx() * t * t
        val center = Offset(
            origin.x + (cos(angle) * distance).toFloat(),
            origin.y + (sin(angle) * distance).toFloat() + fall,
        )
        val radius = (if (i % 3 == 0) 4.5f else 3f).dp.toPx() * (1f - 0.4f * t)
        val alpha = if (t < 0.6f) 1f else (1f - t) / 0.4f
        drawCircle(colors[i % colors.size].copy(alpha = alpha), radius, center)
    }
}
