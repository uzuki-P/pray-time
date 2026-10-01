package dev.praytime.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/** clickable plus the hand cursor desktop users expect over controls. */
fun Modifier.pressable(enabled: Boolean = true, role: Role = Role.Button, onClick: () -> Unit): Modifier =
    this
        .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
        .clickable(enabled = enabled, role = role, onClick = onClick)

@Composable
internal fun CheckIcon(color: Color, modifier: Modifier = Modifier, strokeWidth: Dp = 1.8.dp) {
    Canvas(modifier) {
        val path = Path().apply {
            moveTo(size.width * 0.18f, size.height * 0.53f)
            lineTo(size.width * 0.42f, size.height * 0.76f)
            lineTo(size.width * 0.84f, size.height * 0.28f)
        }
        drawPath(path, color, style = Stroke(strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
internal fun CloseIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = 1.6.dp.toPx()
        val inset = size.minDimension * 0.25f
        drawLine(color, Offset(inset, inset), Offset(size.width - inset, size.height - inset), stroke, StrokeCap.Round)
        drawLine(color, Offset(size.width - inset, inset), Offset(inset, size.height - inset), stroke, StrokeCap.Round)
    }
}

@Composable
internal fun CloseButton(onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        CloseIcon(palette.muted, Modifier.size(16.dp))
    }
}

/**
 * The mark-as-prayed control. Ringed while the prayer waits (coral when it is the due
 * prayer, dimmed and inert before its time), filled teal once completed, with a short
 * pop when it flips to completed.
 */
@Composable
internal fun PrayerCheck(done: Boolean, due: Boolean, onClick: () -> Unit, enabled: Boolean = true, size: Dp = 20.dp) {
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
    val fill by animateColorAsState(if (done) palette.prayed else Color.Transparent, label = "checkFill")
    val ring = when {
        done -> palette.prayed
        due -> palette.due
        enabled -> palette.faint
        else -> palette.onSurface.copy(alpha = 0.2f)
    }
    Box(modifier = Modifier.size(size + 8.dp), contentAlignment = Alignment.Center) {
        if (burst.value > 0f && burst.value < 1f) {
            Box(
                modifier = Modifier
                    .size(size * (1f + 0.9f * burst.value))
                    .border((1.5f * (1f - burst.value)).dp, palette.prayed.copy(alpha = 1f - burst.value), CircleShape),
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
                .border(1.6.dp, ring, CircleShape)
                .pressable(enabled = enabled, role = Role.Checkbox, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (done) CheckIcon(palette.onPrayed, Modifier.size(size * 0.7f))
        }
    }
}

@Composable
internal fun Switch(checked: Boolean, onToggle: () -> Unit, enabled: Boolean = true) {
    val palette = LocalAppPalette.current
    val track by animateColorAsState(if (checked) palette.accent else palette.onSurface.copy(alpha = 0.18f), label = "track")
    val knobOffset by animateDpAsState(if (checked) 16.dp else 0.dp, label = "knob")
    Box(
        modifier = Modifier
            .width(36.dp)
            .height(20.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .clip(CircleShape)
            .background(track)
            .pressable(enabled = enabled, role = Role.Switch, onClick = onToggle)
            .padding(3.dp),
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobOffset)
                .size(14.dp)
                .clip(CircleShape)
                .background(if (checked) palette.onAccent else palette.surface),
        )
    }
}

@Composable
internal fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    val palette = LocalAppPalette.current
    val type = LocalAppType.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(palette.raised)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) palette.surface else Color.Transparent)
                    .pressable(role = Role.RadioButton) { onSelect(option) }
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    style = type.body(12.sp, if (isSelected) FontWeight.SemiBold else FontWeight.Medium),
                    color = if (isSelected) palette.onSurface else palette.muted,
                )
            }
        }
    }
}

enum class ButtonKind { Primary, Prayed, Quiet }

@Composable
internal fun PillButton(
    kind: ButtonKind,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.(contentColor: Color) -> Unit,
) {
    val palette = LocalAppPalette.current
    val (background, contentColor) = when (kind) {
        ButtonKind.Primary -> palette.accent to palette.onAccent
        ButtonKind.Prayed -> palette.prayed to palette.onPrayed
        ButtonKind.Quiet -> palette.raised to palette.onSurface
    }
    Row(
        modifier = modifier
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .pressable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        content(contentColor)
    }
}

@Composable
internal fun ButtonLabel(text: String, color: Color) {
    Text(text, style = LocalAppType.current.body(12.sp, FontWeight.SemiBold), color = color)
}
