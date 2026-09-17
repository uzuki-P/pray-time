package dev.praytime

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberNotification
import androidx.compose.ui.window.rememberTrayState
import dev.praytime.calculation.PrayerTime
import dev.praytime.ui.ScheduleViewModel
import java.io.InputStream
import java.time.Instant

fun main() = application {
    var showWindow by remember { mutableStateOf(true) }
    val darkTheme = isSystemInDarkTheme()
    val appIcon = remember(darkTheme) {
        loadIcon(if (darkTheme) "pray-time-app-dark.png" else "pray-time-app-light.png")
    }
    val trayIcon = remember(darkTheme) {
        loadIcon(if (darkTheme) "pray-time-tray-dark.png" else "pray-time-tray-light.png")
    }
    val trayState = rememberTrayState()
    val testNotification = rememberNotification(
        title = "Prayer reminder",
        message = "The next prayer is coming up.",
    )
    val quit = { exitApplication() }

    Tray(
        state = trayState,
        icon = trayIcon,
        tooltip = "Pray Time",
        onAction = { showWindow = true },
        menu = {
            Item("Open schedule", onClick = { showWindow = true })
            Item("Test notification", onClick = { trayState.sendNotification(testNotification) })
            Item("Quit", onClick = quit)
        },
    )

    if (showWindow) {
        Window(
            onCloseRequest = { showWindow = false },
            title = "Pray Time",
            icon = appIcon,
        ) {
            PrayTimeApp()
        }
    }
}

@Composable
private fun PrayTimeApp() {
    val darkTheme = isSystemInDarkTheme()
    val viewModel = remember { ScheduleViewModel() }
    val dayTimes by viewModel.dayTimes.collectAsState()
    val countdown by viewModel.countdown.collectAsState()
    val completed by viewModel.completed.collectAsState()

    LaunchedEffect(viewModel) { viewModel.run() }

    val next = countdown.nextPrayer
    val canMarkNext = next != null &&
        next.isPrayer &&
        next.instant <= viewModel.now() &&
        next.instant !in completed

    MaterialTheme(
        colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(),
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Pray Time", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            dayTimes.region.displayName + " · " + dayTimes.date,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedButton(onClick = { /* TODO: open region settings */ }) {
                        Text("Change region")
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Next prayer",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        if (next != null) {
                            Text(next.name, style = MaterialTheme.typography.headlineMedium)
                            Text(
                                next.displayTime + " · in " + countdown.displayRemaining,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        } else {
                            Text("—", style = MaterialTheme.typography.headlineMedium)
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { next?.let { viewModel.markPrayed(it, true) } },
                            enabled = canMarkNext,
                        ) {
                            Text("Mark as prayed")
                        }
                    }
                }

                Text("Today’s prayers", style = MaterialTheme.typography.titleLarge)

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(dayTimes.times, key = { it.name }) { prayer ->
                        PrayerRow(
                            prayer = prayer,
                            now = viewModel.now(),
                            completed = prayer.instant in completed,
                            enabled = prayer.isPrayer && prayer.instant <= viewModel.now(),
                            onPrayedChange = { viewModel.markPrayed(prayer, it) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerRow(
    prayer: PrayerTime,
    now: Instant,
    completed: Boolean,
    enabled: Boolean,
    onPrayedChange: (Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(prayer.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    when {
                        !prayer.isPrayer -> "Sunrise"
                        completed -> "Completed"
                        prayer.instant <= now -> "Due"
                        else -> "Upcoming"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(prayer.displayTime, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            Checkbox(
                checked = completed,
                onCheckedChange = onPrayedChange,
                enabled = enabled,
            )
        }
    }
}

private fun loadIcon(fileName: String): Painter {
    val stream: InputStream = requireNotNull(
        Main::class.java.getResourceAsStream("/icons/$fileName"),
    ) { "Missing icon resource: $fileName" }
    return BitmapPainter(stream.use(::loadImageBitmap))
}

private object Main
