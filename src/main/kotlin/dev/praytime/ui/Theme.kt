package dev.praytime.ui

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import dev.praytime.resources.Res
import dev.praytime.resources.ibm_plex_sans_bold
import dev.praytime.resources.ibm_plex_sans_medium
import dev.praytime.resources.ibm_plex_sans_regular
import dev.praytime.resources.ibm_plex_sans_semibold
import dev.praytime.resources.reem_kufi_bold
import dev.praytime.resources.reem_kufi_medium
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.jetbrains.compose.resources.Font

enum class ThemeMode { LIGHT, DARK, SYSTEM }

data class AppPalette(
    val surface: Color,
    val raised: Color,
    val onSurface: Color,
    val accent: Color,
    val onAccent: Color,
    val due: Color,
    val prayed: Color,
    val onPrayed: Color,
) {
    val muted: Color = onSurface.copy(alpha = 0.62f)
    val faint: Color = onSurface.copy(alpha = 0.38f)
    val divider: Color = onSurface.copy(alpha = 0.09f)
}

// Night-sky ink with a brass lamp accent; coral marks a due prayer, teal a completed one.
internal val DarkPalette = AppPalette(
    surface = Color(0xFF111B2B),
    raised = Color(0xFF1B2739),
    onSurface = Color(0xFFE8EDF4),
    accent = Color(0xFFE6B35A),
    onAccent = Color(0xFF1F1606),
    due = Color(0xFFF08A6C),
    prayed = Color(0xFF5CC4A6),
    onPrayed = Color(0xFF07261D),
)

internal val LightPalette = AppPalette(
    surface = Color(0xFFF6F8FB),
    raised = Color(0xFFE6EBF2),
    onSurface = Color(0xFF142033),
    accent = Color(0xFF8C5A06),
    onAccent = Color.White,
    due = Color(0xFFC2482B),
    prayed = Color(0xFF1E8A6E),
    onPrayed = Color.White,
)

/** The sky's colour at each time in the day schedule; the schedule rail blends between them. */
internal val skyColors = mapOf(
    "Fajr" to Color(0xFF7B6FD0),
    "Sunrise" to Color(0xFFF2A766),
    "Dhuhr" to Color(0xFF4EA3E6),
    "Asr" to Color(0xFFE7B04F),
    "Maghrib" to Color(0xFFE0607A),
    "Isha" to Color(0xFF4656A8),
)

internal val arabicNames = mapOf(
    "Fajr" to "الفجر",
    "Sunrise" to "الشروق",
    "Dhuhr" to "الظهر",
    "Asr" to "العصر",
    "Maghrib" to "المغرب",
    "Isha" to "العشاء",
)

/** Reem Kufi for prayer names (Latin and Arabic), IBM Plex Sans for everything else. */
data class AppType(val display: FontFamily, val body: FontFamily) {
    fun display(size: TextUnit, weight: FontWeight = FontWeight.Bold) =
        TextStyle(fontFamily = display, fontSize = size, fontWeight = weight)

    fun body(size: TextUnit, weight: FontWeight = FontWeight.Normal) =
        TextStyle(fontFamily = body, fontSize = size, fontWeight = weight)

    /** Tabular figures, so times and countdowns do not jitter as digits change. */
    fun figures(size: TextUnit, weight: FontWeight = FontWeight.Medium) =
        body(size, weight).copy(fontFeatureSettings = "tnum")
}

val LocalAppPalette = staticCompositionLocalOf { DarkPalette }
val LocalAppType = staticCompositionLocalOf { AppType(FontFamily.Default, FontFamily.Default) }

@Composable
fun AppTheme(palette: AppPalette, content: @Composable () -> Unit) {
    val type = AppType(
        display = FontFamily(
            Font(Res.font.reem_kufi_medium, FontWeight.Medium),
            Font(Res.font.reem_kufi_bold, FontWeight.Bold),
        ),
        body = FontFamily(
            Font(Res.font.ibm_plex_sans_regular, FontWeight.Normal),
            Font(Res.font.ibm_plex_sans_medium, FontWeight.Medium),
            Font(Res.font.ibm_plex_sans_semibold, FontWeight.SemiBold),
            Font(Res.font.ibm_plex_sans_bold, FontWeight.Bold),
        ),
    )
    CompositionLocalProvider(
        LocalAppPalette provides palette,
        LocalAppType provides type,
        LocalTextStyle provides type.body(13.sp).copy(color = palette.onSurface),
        content = content,
    )
}

fun themeIsDark(mode: ThemeMode, systemDark: Boolean): Boolean = when (mode) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> systemDark
}

// Compose's isSystemInDarkTheme() reads a composition-local default that is
// resolved once per process on desktop, so it never tracks theme changes.
// skiko's query is a cheap XDG-portal DBus read, so polling is enough.
fun systemThemeIsDark(): Boolean =
    org.jetbrains.skiko.currentSystemTheme == org.jetbrains.skiko.SystemTheme.DARK

@Composable
fun rememberSystemThemeIsDark(pollMillis: Long = 1_000L): State<Boolean> =
    produceState(systemThemeIsDark()) {
        while (isActive) {
            value = systemThemeIsDark()
            delay(pollMillis)
        }
    }
