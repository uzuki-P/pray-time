@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package dev.praytime

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.awt.SwingDialog
import androidx.compose.ui.window.WindowDecoration
import androidx.compose.ui.window.rememberDialogState
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.nucleusframework.composenativetray.tray.api.Tray
import dev.praytime.domain.builtInRegions
import dev.praytime.domain.medanJohor
import dev.praytime.platform.AppState
import dev.praytime.platform.Autostart
import dev.praytime.platform.LinuxSniTray
import dev.praytime.resources.Res
import dev.praytime.resources.pray_time_app_dark
import dev.praytime.resources.pray_time_app_light
import dev.praytime.resources.pray_time_tray_dark
import dev.praytime.resources.pray_time_tray_light
import dev.praytime.ui.CompactView
import dev.praytime.ui.ScheduleViewModel
import dev.praytime.ui.cardSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import java.awt.KeyboardFocusManager
import java.awt.Toolkit
import java.beans.PropertyChangeListener
import kotlin.math.roundToInt

fun main() = application {
    var compactVisible by remember { mutableStateOf(false) }
    var compactSuppressUntil by remember { mutableStateOf(0L) }
    var compactHadFocus by remember { mutableStateOf(false) }
    var menuVisible by remember { mutableStateOf(false) }
    var menuPosition by remember { mutableStateOf(0 to 0) }
    var menuSuppressUntil by remember { mutableStateOf(0L) }
    var menuHadFocus by remember { mutableStateOf(false) }

    val darkTheme = isSystemInDarkTheme()
    val appIcon = if (darkTheme) {
        painterResource(Res.drawable.pray_time_app_dark)
    } else {
        painterResource(Res.drawable.pray_time_app_light)
    }
    val trayIcon = if (darkTheme) {
        painterResource(Res.drawable.pray_time_tray_dark)
    } else {
        painterResource(Res.drawable.pray_time_tray_light)
    }
    val viewModel = remember {
        ScheduleViewModel(region = AppState.loadRegion(builtInRegions + AppState.recentRegions()) ?: medanJohor).apply {
            // Restore synchronously, before any collector can observe and re-save the empty initial set.
            restoreCompleted(AppState.loadCompleted(dayTimes.value.date))
        }
    }
    val quit = { exitApplication() }

    LaunchedEffect(viewModel) {
        viewModel.run()
    }
    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) { Autostart.refresh() }
    }
    LaunchedEffect(viewModel) {
        launch {
            viewModel.completed.collect { AppState.saveCompleted(viewModel.dayTimes.value.date, it) }
        }
        launch {
            viewModel.dayTimes.collect { AppState.saveRegion(it.region) }
        }
    }

    val toggleCompact = {
        menuVisible = false
        compactSuppressUntil = System.currentTimeMillis() + 400
        compactHadFocus = false
        compactVisible = !compactVisible
    }
    val showMenu = { x: Int, y: Int ->
        if (menuVisible) {
            menuVisible = false
        } else {
            menuSuppressUntil = System.currentTimeMillis() + 400
            menuHadFocus = false
            menuPosition = clampToScreen(x, y)
            menuVisible = true
        }
    }
    val isLinux = System.getProperty("os.name").lowercase().contains("linux")

    if (isLinux) {
        DisposableEffect(Unit) {
            fun onEdt(block: () -> Unit) {
                java.awt.EventQueue.invokeLater(block)
            }
            val tray = LinuxSniTray(
                onPrimaryClick = { onEdt { toggleCompact() } },
                onContextMenu = { x, y -> onEdt { showMenu(x, y) } },
                onQuit = { onEdt { quit() } },
            )
            tray.start()
            onDispose { tray.stop() }
        }
    } else {
        Tray(
            icon = trayIcon,
            tooltip = "Pray Time",
            primaryAction = { toggleCompact() },
        ) {
            Item("Show / Hide popup") { toggleCompact() }
            Divider()
            Item("Quit") { quit() }
        }
    }

    Window(
        onCloseRequest = { compactVisible = false },
        visible = compactVisible,
        title = "Pray Time",
        icon = appIcon,
        undecorated = true,
        transparent = true,
        resizable = false,
        focusable = true,
        alwaysOnTop = true,
        state = rememberWindowState(
            size = DpSize(296.dp, 372.dp),
            position = WindowPosition(Alignment.TopEnd),
        ),
    ) {
        TransparentWindowBackground(window)
        DisposableEffect(window) {
            val focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager()
            val listener = PropertyChangeListener { event ->
                if (event.propertyName != "activeWindow") return@PropertyChangeListener
                if (event.newValue == window) {
                    compactHadFocus = true
                } else if (compactHadFocus && System.currentTimeMillis() >= compactSuppressUntil) {
                    compactHadFocus = false
                    compactVisible = false
                }
            }
            focusManager.addPropertyChangeListener(listener)
            onDispose { focusManager.removePropertyChangeListener(listener) }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        window.setLocation(
                            window.x + dragAmount.x.roundToInt(),
                            window.y + dragAmount.y.roundToInt(),
                        )
                    }
                },
        ) {
            CompactView(viewModel)
        }
    }

    SwingDialog(
        onCloseRequest = { menuVisible = false },
        state = rememberDialogState(size = DpSize(184.dp, 112.dp)),
        visible = menuVisible,
        title = "Pray Time Menu",
        icon = null,
        decoration = WindowDecoration.Undecorated(0.dp),
        transparent = true,
        resizable = false,
        enabled = true,
        focusable = true,
        alwaysOnTop = true,
        onPreviewKeyEvent = { false },
        onKeyEvent = { false },
        modalityType = java.awt.Dialog.ModalityType.MODELESS,
        init = { dialog ->
            // Utility windows are skipped by the taskbar, pager and alt-tab.
            runCatching { dialog.type = java.awt.Window.Type.UTILITY }
        },
    ) {
        TransparentWindowBackground(window)
        LaunchedEffect(menuPosition) {
            window.setLocation(menuPosition.first, menuPosition.second)
        }
        DisposableEffect(window) {
            val focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager()
            val listener = PropertyChangeListener { event ->
                if (event.propertyName != "activeWindow") return@PropertyChangeListener
                if (event.newValue == window) {
                    menuHadFocus = true
                } else if (menuHadFocus && System.currentTimeMillis() >= menuSuppressUntil) {
                    menuHadFocus = false
                    menuVisible = false
                }
            }
            focusManager.addPropertyChangeListener(listener)
            onDispose { focusManager.removePropertyChangeListener(listener) }
        }
        Box(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardSurface)
                    .padding(vertical = 4.dp),
            ) {
                MenuRow("Show / Hide popup") {
                    menuVisible = false
                    toggleCompact()
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                MenuRow("Quit") {
                    menuVisible = false
                    quit()
                }
            }
        }
    }
}

private fun clampToScreen(x: Int, y: Int): Pair<Int, Int> {
    val screen = Toolkit.getDefaultToolkit().screenSize
    val clampedX = x.coerceIn(0, (screen.width - 200).coerceAtLeast(0))
    val clampedY = y.coerceIn(0, (screen.height - 150).coerceAtLeast(0))
    return clampedX to clampedY
}

@Composable
private fun TransparentWindowBackground(window: java.awt.Window) {
    DisposableEffect(window) {
        val old = window.background
        window.background = java.awt.Color(0, 0, 0, 0)
        onDispose { window.background = old }
    }
}

@Composable
private fun MenuRow(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(
            label,
            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
            color = Color(0xFFE8E8FF),
        )
    }
}
