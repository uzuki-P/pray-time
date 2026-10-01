package dev.praytime.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import dev.praytime.domain.medanJohor
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test

/** Renders each view offscreen to build/ui-previews for visual review. */
class ViewPreviewRender {
    private val outDir = File("build/ui-previews").apply { mkdirs() }
    private val zone = ZoneId.of(medanJohor.timezoneId)

    private fun render(name: String, width: Int, height: Int, palette: AppPalette, content: @Composable () -> Unit) {
        ImageComposeScene(width * 2, height * 2, density = Density(2f)) {
            AppTheme(palette) {
                Box(Modifier.fillMaxSize()) { content() }
            }
        }.use { scene ->
            scene.render(0)
            val image = scene.render(1_000_000_000L)
            val data = image.encodeToData(EncodedImageFormat.PNG) ?: return
            File(outDir, "$name.png").writeBytes(data.bytes)
        }
    }

    /** Clicks at `tapDp` and saves a frame every `stepMillis`, for checking animations. */
    private fun renderFilmstrip(
        name: String,
        width: Int,
        height: Int,
        tapDp: Offset,
        frames: Int,
        stepMillis: Long,
        content: @Composable () -> Unit,
    ) {
        ImageComposeScene(width * 2, height * 2, density = Density(2f)) {
            AppTheme(DarkPalette) {
                Box(Modifier.fillMaxSize()) { content() }
            }
        }.use { scene ->
            var time = 0L
            scene.render(time)
            val tap = tapDp * 2f
            scene.sendPointerEvent(PointerEventType.Move, tap)
            scene.sendPointerEvent(PointerEventType.Press, tap)
            scene.sendPointerEvent(PointerEventType.Release, tap)
            repeat(frames) { index ->
                time += stepMillis * 1_000_000L
                val image = scene.render(time)
                val data = image.encodeToData(EncodedImageFormat.PNG) ?: return
                File(outDir, "$name-%02d.png".format(index)).writeBytes(data.bytes)
            }
        }
    }

    private fun viewModel(at: String) =
        ScheduleViewModel(Clock.fixed(Instant.parse(at), zone)).apply {
            val day = dayTimes.value
            restoreCompleted(day.times.filter { it.isPrayer && it.instant < now() }.take(2).map { it.instant }.toSet())
        }

    @Test
    fun renderPreviews() {
        // 2026-09-17 16:10 WIB: afternoon, Asr due, Fajr and Dhuhr completed.
        val afternoon = "2026-09-17T09:10:00Z"
        // 2026-09-17 21:30 WIB: night after Isha.
        val night = "2026-09-17T14:30:00Z"
        for ((label, palette) in listOf("dark" to DarkPalette, "light" to LightPalette)) {
            render("compact-afternoon-$label", 296, 400, palette) { CompactView(viewModel(afternoon)) }
            render("compact-night-$label", 296, 400, palette) { CompactView(viewModel(night)) }
            val vm = viewModel(afternoon)
            render("notification-$label", 328, 148 + NotificationDroopRoom.value.toInt(), palette) {
                PrayerNotification(
                    prayer = vm.dayTimes.value.times.first { it.name == "Asr" },
                    now = vm.now(),
                    snoozeMinutes = 10,
                    onPrayed = {},
                    onDismiss = {},
                )
            }
            if (label == "dark") {
                val asr = vm.dayTimes.value.times.first { it.name == "Asr" }
                for ((kind, tapDp) in listOf("prayed" to Offset(70f, 95f), "snooze" to Offset(220f, 95f))) {
                    renderFilmstrip("film-$kind", 328, 148 + NotificationDroopRoom.value.toInt(), tapDp, 24, 140) {
                        PrayerNotification(prayer = asr, now = vm.now(), snoozeMinutes = 10, onPrayed = {}, onDismiss = {})
                    }
                }
            }
            for (tab in SettingsTab.entries) {
                render("settings-${tab.name.lowercase()}-$label", 368, 576, palette) {
                    SettingsView(viewModel = vm, themeMode = ThemeMode.SYSTEM, onThemeModeChange = {}, onClose = {}, initialTab = tab)
                }
            }
        }
    }
}
