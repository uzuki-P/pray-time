package dev.praytime.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

enum class ThemeMode { LIGHT, DARK, SYSTEM }

data class AppPalette(
    val surface: Color,
    val onSurface: Color,
    val accent: Color,
    val onAccent: Color,
) {
    val divider: Color = onSurface.copy(alpha = 0.08f)
}

internal val DarkPalette = AppPalette(
    surface = Color(0xFF15152B),
    onSurface = Color.White,
    accent = Color(0xFF9D8CFF),
    onAccent = Color(0xFF15152B),
)

internal val LightPalette = AppPalette(
    surface = Color(0xFFF4F3FB),
    onSurface = Color(0xFF1D1C36),
    accent = Color(0xFF6C5CE7),
    onAccent = Color.White,
)

val LocalAppPalette = staticCompositionLocalOf<AppPalette> { DarkPalette }

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
