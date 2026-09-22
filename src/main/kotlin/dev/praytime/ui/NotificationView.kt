package dev.praytime.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.praytime.calculation.PrayerTime
import kotlinx.coroutines.launch

private val dueAmber = Color(0xFFFFC24D)
private val prayedGreen = Color(0xFF3EDC81)
private val prayedGreenText = Color(0xFF0B2E1B)

@Composable
fun PrayerNotification(
    prayer: PrayerTime,
    onPrayed: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    val scope = rememberCoroutineScope()
    var celebrating by remember { mutableStateOf(false) }
    var shaking by remember { mutableStateOf(false) }
    val pop = remember { Animatable(1f) }
    val shake = remember { Animatable(0f) }
    val burst = remember { Animatable(1f) }
    val busy = celebrating || shaking

    fun celebrate() {
        if (busy) return
        celebrating = true
        scope.launch {
            burst.snapTo(0f)
            launch { burst.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) }
            pop.animateTo(1.08f, tween(110, easing = FastOutSlowInEasing))
            pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
            // Reset before the callback: the dialog keeps this composition alive
            // while hidden, so leftover state would dead-lock the next reminder.
            celebrating = false
            onPrayed()
        }
    }

    fun reluctantDismiss() {
        if (busy) return
        shaking = true
        scope.launch {
            shake.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 420
                    0f at 0
                    -7f at 70
                    7f at 140
                    -5f at 210
                    5f at 280
                    -2f at 350
                    0f at 420
                },
            )
            shaking = false
            onDismiss()
        }
    }

    val closeTint by animateColorAsState(
        targetValue = if (shaking) dueAmber else Color.White.copy(alpha = 0.75f),
        label = "closeTint",
    )

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .width(272.dp)
                .graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                    translationX = shake.value.dp.toPx()
                }
                .shadow(6.dp, shape)
                .clip(shape)
                .background(cardSurface)
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(prayerColors.getValue(prayer.name)),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    prayer.name,
                    style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold),
                    color = Color.White,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "DUE",
                    style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Black),
                    color = dueAmber,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    prayer.displayTime,
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                    color = Color.White.copy(alpha = 0.6f),
                )
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable(onClick = ::reluctantDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "✕",
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Black),
                        color = closeTint,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Time to pray ${prayer.name}.",
                style = TextStyle(fontSize = 11.sp),
                color = Color.White.copy(alpha = 0.65f),
            )
            Spacer(Modifier.height(12.dp))
            Box {
                if (burst.value > 0f && burst.value < 1f) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer {
                                val grow = 1f + 0.22f * burst.value
                                scaleX = grow
                                scaleY = grow
                                alpha = 1f - burst.value
                            }
                            .border(2.dp, prayedGreenBright, RoundedCornerShape(12.dp)),
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (celebrating) prayedGreenBright else prayedGreen)
                        .clickable(onClick = ::celebrate)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "✓",
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Black),
                        color = prayedGreenText,
                    )
                    Text(
                        "Mark prayed",
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                        color = prayedGreenText,
                    )
                }
            }
        }
    }
}

private val prayedGreenBright = Color(0xFF5CEBA0)
