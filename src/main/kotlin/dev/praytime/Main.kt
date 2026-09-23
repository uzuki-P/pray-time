@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package dev.praytime

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
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
import dev.praytime.platform.LinuxWindowMover
import dev.praytime.resources.Res
import dev.praytime.resources.pray_time_app_dark
import dev.praytime.resources.pray_time_app_light
import dev.praytime.resources.pray_time_tray_dark
import dev.praytime.resources.pray_time_tray_light
import dev.praytime.calculation.PrayerTime
import dev.praytime.platform.WindowAnchor
import dev.praytime.platform.anchorPosition
import dev.praytime.ui.CompactView
import dev.praytime.ui.DarkPalette
import dev.praytime.ui.LightPalette
import dev.praytime.ui.LocalAppPalette
import dev.praytime.ui.PrayerNotification
import dev.praytime.ui.ReminderController
import dev.praytime.ui.ScheduleViewModel
import dev.praytime.ui.SettingsView
import dev.praytime.ui.ThemeMode
import dev.praytime.ui.latestDuePrayer
import dev.praytime.ui.rememberSystemThemeIsDark
import dev.praytime.ui.themeIsDark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import java.awt.KeyboardFocusManager
import java.awt.Point
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
    var settingsVisible by remember { mutableStateOf(false) }
    var settingsSuppressUntil by remember { mutableStateOf(0L) }
    var settingsHadFocus by remember { mutableStateOf(false) }

    val darkTheme by rememberSystemThemeIsDark()
    var themeMode by remember { mutableStateOf(AppState.loadThemeMode()) }
    val setThemeMode = { mode: ThemeMode ->
        themeMode = mode
        AppState.saveThemeMode(mode)
    }
    val palette = if (themeIsDark(themeMode, darkTheme)) DarkPalette else LightPalette
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

    val reminderController = remember { ReminderController() }
    var notifyVisible by remember { mutableStateOf(false) }
    var notifyPrayer by remember { mutableStateOf<PrayerTime?>(null) }
    var testReminderVisible by remember { mutableStateOf(false) }
    var testReminderPrayer by remember { mutableStateOf<PrayerTime?>(null) }
    val dismissNotification = {
        reminderController.dismiss(AppState.loadReminderEnabled(), AppState.loadReminderMinutes())
        notifyVisible = false
    }
    val showTestReminder = {
        val day = viewModel.dayTimes.value
        val now = viewModel.now()
        testReminderPrayer = latestDuePrayer(day, emptySet(), now) ?: day.next(now) ?: day.times.last()
        testReminderVisible = true
    }

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
        launch {
            // countdown ticks every second, driving both due detection and snooze expiry.
            combine(viewModel.dayTimes, viewModel.completed, viewModel.countdown) { day, completed, _ ->
                latestDuePrayer(day, completed, viewModel.now())
            }.collect { due ->
                reminderController.onTick(due, AppState.loadReminderEnabled())
                notifyPrayer = reminderController.current
                notifyVisible = reminderController.visible && !compactVisible && !menuVisible
            }
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
    val showSettingsWindow = {
        settingsSuppressUntil = System.currentTimeMillis() + 400
        settingsHadFocus = false
        // Settings taking focus must not read as the popup losing it, else
        // closing settings later would close the popup too.
        compactHadFocus = false
        settingsVisible = true
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
                onSettings = { onEdt { showSettingsWindow() } },
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
            Item("Settings") { showSettingsWindow() }
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
        val density = LocalDensity.current
        LaunchedEffect(compactVisible) {
            if (!compactVisible) return@LaunchedEffect
            val bounds = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                .defaultScreenDevice.defaultConfiguration.bounds
            val margin = with(density) { 12.dp.roundToPx() }
            val width = with(density) { 296.dp.roundToPx() }
            val height = with(density) { 372.dp.roundToPx() }
            val position = anchorPosition(AppState.loadMainAnchor(), bounds, width, height, margin)
            window.setLocation(position.x, position.y)
        }
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
        var dragBase by remember { mutableStateOf<Point?>(null) }
        var dragDelta by remember { mutableStateOf(Offset.Zero) }
        var wmMoving by remember { mutableStateOf(false) }
        val headerDrag = Modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = {
                    // setLocation dragging jitters under XWayland; the WM's own
                    // move tracks the pointer per-frame, so prefer it there.
                    wmMoving = isLinux && LinuxWindowMover.requestInteractiveMove(window)
                    if (!wmMoving) {
                        dragBase = Point(window.x, window.y)
                        dragDelta = Offset.Zero
                    }
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    if (wmMoving) return@detectDragGestures
                    val base = dragBase ?: return@detectDragGestures
                    dragDelta += dragAmount
                    val scale = density.density
                    window.setLocation(
                        base.x + (dragDelta.x * scale).roundToInt(),
                        base.y + (dragDelta.y * scale).roundToInt(),
                    )
                },
                onDragEnd = { dragBase = null; wmMoving = false },
                onDragCancel = { dragBase = null; wmMoving = false },
            )
        }
        CompositionLocalProvider(LocalAppPalette provides palette) {
            Box(modifier = Modifier.fillMaxSize()) {
                CompactView(
                    viewModel = viewModel,
                    headerDrag = headerDrag,
                    onOpenSettings = { showSettingsWindow() },
                )
            }
        }
    }

    val showNotification = notifyVisible || testReminderVisible
    SwingDialog(
        onCloseRequest = { if (testReminderVisible) testReminderVisible = false else dismissNotification() },
        state = rememberDialogState(size = DpSize(296.dp, 148.dp)),
        visible = showNotification,
        title = "Pray Time",
        icon = null,
        decoration = WindowDecoration.Undecorated(0.dp),
        transparent = true,
        resizable = false,
        enabled = true,
        focusable = false,
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
        val density = LocalDensity.current
        LaunchedEffect(showNotification) {
            if (!showNotification) return@LaunchedEffect
            val bounds = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                .defaultScreenDevice.defaultConfiguration.bounds
            val margin = with(density) { 12.dp.roundToPx() }
            val width = with(density) { 296.dp.roundToPx() }
            val height = with(density) { 148.dp.roundToPx() }
            val position = anchorPosition(AppState.loadReminderAnchor(), bounds, width, height, margin)
            // AWT centers the dialog when it becomes visible, so keep overriding
            // for a few frames until it settles at the chosen anchor.
            repeat(5) {
                window.setLocation(position.x, position.y)
                withFrameNanos { }
            }
        }
        CompositionLocalProvider(LocalAppPalette provides palette) {
            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                val prayer = if (testReminderVisible) testReminderPrayer else notifyPrayer
                if (showNotification) {
                    prayer?.let { current ->
                        // Fresh animation state on every show: SwingDialog keeps the
                        // hidden dialog's composition (and its remember state) alive,
                        // so stale flags would leave the buttons permanently busy.
                        key(showNotification, current.instant) {
                            if (testReminderVisible) {
                                PrayerNotification(
                                    prayer = current,
                                    onPrayed = { testReminderVisible = false },
                                    onDismiss = { testReminderVisible = false },
                                )
                            } else {
                                PrayerNotification(
                                    prayer = current,
                                    onPrayed = { viewModel.markPrayed(current, true) },
                                    onDismiss = { dismissNotification() },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    SwingDialog(
        onCloseRequest = { menuVisible = false },
        state = rememberDialogState(size = DpSize(184.dp, 136.dp)),
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
        CompositionLocalProvider(LocalAppPalette provides palette) {
            Box(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.surface)
                        .padding(vertical = 4.dp),
                ) {
                    MenuRow("Show / Hide popup") {
                        menuVisible = false
                        toggleCompact()
                    }
                    HorizontalDivider(color = palette.divider)
                    MenuRow("Settings") {
                        menuVisible = false
                        showSettingsWindow()
                    }
                    HorizontalDivider(color = palette.divider)
                    MenuRow("Quit") {
                        menuVisible = false
                        quit()
                    }
                }
            }
        }
    }

    SwingDialog(
        onCloseRequest = { settingsVisible = false },
        state = rememberDialogState(size = DpSize(296.dp, 680.dp)),
        visible = settingsVisible,
        title = "Pray Time Settings",
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
        val density = LocalDensity.current
        LaunchedEffect(Unit) {
            val bounds = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                .defaultScreenDevice.defaultConfiguration.bounds
            val width = with(density) { 296.dp.roundToPx() }
            val height = with(density) { 680.dp.roundToPx() }
            // AWT centers the dialog when it becomes visible, so keep overriding
            // for a few frames until it settles centered on the screen.
            repeat(5) {
                window.setLocation(bounds.x + (bounds.width - width) / 2, bounds.y + (bounds.height - height) / 2)
                withFrameNanos { }
            }
        }
        DisposableEffect(window) {
            val focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager()
            val listener = PropertyChangeListener { event ->
                if (event.propertyName != "activeWindow") return@PropertyChangeListener
                if (event.newValue == window) {
                    settingsHadFocus = true
                } else if (settingsHadFocus && System.currentTimeMillis() >= settingsSuppressUntil) {
                    settingsHadFocus = false
                    settingsVisible = false
                }
            }
            focusManager.addPropertyChangeListener(listener)
            onDispose { focusManager.removePropertyChangeListener(listener) }
        }
        CompositionLocalProvider(LocalAppPalette provides palette) {
            Box(modifier = Modifier.fillMaxSize()) {
                SettingsView(
                    viewModel = viewModel,
                    themeMode = themeMode,
                    onThemeModeChange = setThemeMode,
                    onClose = { settingsVisible = false },
                    onTestReminder = showTestReminder,
                )
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
    val palette = LocalAppPalette.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(
            label,
            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
            color = palette.onSurface,
        )
    }
}
