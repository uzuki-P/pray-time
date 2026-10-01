package dev.praytime.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.praytime.domain.Region
import dev.praytime.domain.builtInRegions
import dev.praytime.platform.AppState
import dev.praytime.platform.Autostart
import dev.praytime.platform.CityCatalog
import dev.praytime.platform.WindowAnchor
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SettingsTab(val label: String) { REGION("Region"), REMINDERS("Reminders"), GENERAL("General") }

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.DARK -> "Dark"
    ThemeMode.LIGHT -> "Light"
}

private fun WindowAnchor.label(): String = when (this) {
    WindowAnchor.TOP_LEFT -> "Top left"
    WindowAnchor.TOP_RIGHT -> "Top right"
    WindowAnchor.MIDDLE_LEFT -> "Middle left"
    WindowAnchor.MIDDLE_RIGHT -> "Middle right"
    WindowAnchor.BOTTOM_LEFT -> "Bottom left"
    WindowAnchor.BOTTOM_RIGHT -> "Bottom right"
}

private fun Region.subtitle(): String = listOfNotNull(adminName.ifBlank { null }, country).joinToString(", ")

private fun Region.coordinates(): String {
    fun format(value: Double, positive: Char, negative: Char) =
        String.format(Locale.ROOT, "%.2f°%c", abs(value), if (value >= 0) positive else negative)
    return "${format(latitude, 'N', 'S')} ${format(longitude, 'E', 'W')} · $timezoneId"
}

@Composable
fun SettingsView(
    viewModel: ScheduleViewModel,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onClose: () -> Unit,
    onTestReminder: () -> Unit = {},
    initialTab: SettingsTab = SettingsTab.REGION,
    headerDrag: Modifier = Modifier,
    modifier: Modifier = Modifier,
) {
    val palette = LocalAppPalette.current
    val type = LocalAppType.current
    val shape = RoundedCornerShape(18.dp)
    var tab by remember { mutableStateOf(initialTab) }

    Box(modifier = modifier.fillMaxSize().padding(12.dp)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .shadow(8.dp, shape)
                .clip(shape)
                .background(palette.surface)
                .padding(start = 18.dp, end = 12.dp, top = 12.dp, bottom = 16.dp),
        ) {
            Row(modifier = Modifier.then(headerDrag), verticalAlignment = Alignment.CenterVertically) {
                Text("Settings", style = type.display(22.sp), color = palette.onSurface, modifier = Modifier.weight(1f))
                CloseButton(onClick = onClose)
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.padding(end = 6.dp)) {
                Segmented(SettingsTab.entries, tab, { it.label }, { tab = it })
            }
            Spacer(Modifier.height(16.dp))
            Column(modifier = Modifier.fillMaxWidth().weight(1f).padding(end = 6.dp)) {
                when (tab) {
                    SettingsTab.REGION -> RegionTab(viewModel)
                    SettingsTab.REMINDERS -> RemindersTab(onTestReminder)
                    SettingsTab.GENERAL -> GeneralTab(themeMode, onThemeModeChange)
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.RegionTab(viewModel: ScheduleViewModel) {
    val dayTimes by viewModel.dayTimes.collectAsState()
    val palette = LocalAppPalette.current
    val type = LocalAppType.current
    val listScrollState = rememberScrollState()
    var query by remember { mutableStateOf("") }
    var catalogResults by remember { mutableStateOf<List<Region>?>(null) }
    var recents by remember { mutableStateOf(AppState.recentRegions()) }

    LaunchedEffect(query) {
        val q = query.trim()
        if (q.isEmpty()) {
            catalogResults = null
            return@LaunchedEffect
        }
        delay(250)
        catalogResults = withContext(Dispatchers.IO) { CityCatalog.search(q) }
    }

    val q = query.trim()
    val selected = dayTimes.region
    val localMatches = builtInRegions.filter { it.displayName.contains(q, ignoreCase = true) } +
        recents.filter {
            it.displayName.contains(q, ignoreCase = true) &&
                builtInRegions.none { builtin -> builtin.name == it.name && builtin.country == it.country }
        }
    val merged = catalogResults
        ?.let { catalog -> localMatches + catalog.filter { r -> localMatches.none { it.name == r.name && it.country == r.country } } }

    fun select(region: Region) {
        AppState.saveRecentRegion(region)
        viewModel.setRegion(region)
        recents = AppState.recentRegions()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.raised)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text("Prayer times are calculated for", style = type.body(11.sp), color = palette.muted)
        Text(selected.name, style = type.body(16.sp, FontWeight.SemiBold), color = palette.onSurface)
        Text(selected.subtitle(), style = type.body(12.sp), color = palette.muted)
        Text(selected.coordinates(), style = type.figures(11.sp, FontWeight.Normal), color = palette.faint)
    }
    Spacer(Modifier.height(12.dp))
    SearchField(query = query, onQueryChange = { query = it })
    Spacer(Modifier.height(6.dp))
    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 10.dp)
                .verticalScroll(listScrollState),
        ) {
            if (q.isEmpty()) {
                if (recents.isNotEmpty()) {
                    GroupLabel("Recent")
                    RegionRows(recents, selected) { select(it) }
                }
                GroupLabel("Suggested")
                RegionRows(builtInRegions, selected) { select(it) }
            } else if (merged != null && merged.isEmpty()) {
                Text(
                    "Nothing matches \"$q\". Try a city, district, or kelurahan name.",
                    style = type.body(12.sp),
                    color = palette.muted,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                )
            } else {
                RegionRows(merged ?: localMatches, selected) { select(it) }
                if (catalogResults == null) {
                    GroupLabel("Searching the world list…")
                }
            }
        }
        Scrollbar(listScrollState, modifier = Modifier.align(Alignment.CenterEnd))
    }
}

@Composable
private fun RemindersTab(onTestReminder: () -> Unit) {
    var reminderEnabled by remember { mutableStateOf(AppState.loadReminderEnabled()) }
    var reminderMinutes by remember { mutableStateOf(AppState.loadReminderMinutes()) }
    var reminderAnchor by remember { mutableStateOf(AppState.loadReminderAnchor()) }

    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SettingRow(
            title = "Snooze due reminders",
            description = if (reminderEnabled) {
                "Snoozing hides the reminder, and it returns while the prayer is still not marked as prayed."
            } else {
                "Dismissing hides the reminder until the next prayer is due."
            },
        ) {
            Switch(reminderEnabled, onToggle = {
                AppState.saveReminderEnabled(!reminderEnabled)
                reminderEnabled = AppState.loadReminderEnabled()
            })
        }
        SettingRow(title = "Snooze for", enabled = reminderEnabled) {
            MinutesStepper(reminderMinutes, enabled = reminderEnabled) { minutes ->
                AppState.saveReminderMinutes(minutes)
                reminderMinutes = AppState.loadReminderMinutes()
            }
        }
        PositionSetting("Reminder position", reminderAnchor) { anchor ->
            reminderAnchor = anchor
            AppState.saveReminderAnchor(anchor)
        }
        PillButton(ButtonKind.Quiet, onClick = onTestReminder) { color ->
            ButtonLabel("Show a test reminder", color)
        }
    }
}

@Composable
private fun GeneralTab(themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit) {
    var autostartEnabled by remember { mutableStateOf(Autostart.isEnabled()) }
    var mainAnchor by remember { mutableStateOf(AppState.loadMainAnchor()) }

    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SettingRow(title = "Theme") {
            Segmented(listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK), themeMode, { it.label() }, onThemeModeChange)
        }
        SettingRow(title = "Start on login", description = "Opens Pray Time in the tray when you sign in.") {
            Switch(autostartEnabled, onToggle = {
                Autostart.setEnabled(!autostartEnabled)
                autostartEnabled = Autostart.isEnabled()
            })
        }
        PositionSetting("Popup position", mainAnchor) { anchor ->
            mainAnchor = anchor
            AppState.saveMainAnchor(anchor)
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    description: String? = null,
    enabled: Boolean = true,
    control: @Composable () -> Unit,
) {
    val palette = LocalAppPalette.current
    val type = LocalAppType.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = type.body(13.sp, FontWeight.SemiBold), color = if (enabled) palette.onSurface else palette.faint)
            if (description != null) {
                Text(description, style = type.body(11.sp), color = palette.muted)
            }
        }
        control()
    }
}

@Composable
private fun GroupLabel(label: String) {
    val palette = LocalAppPalette.current
    Text(
        label,
        style = LocalAppType.current.body(11.sp, FontWeight.SemiBold),
        color = palette.faint,
        modifier = Modifier.padding(start = 10.dp, top = 10.dp, bottom = 4.dp),
    )
}

@Composable
private fun RegionRows(regions: List<Region>, selected: Region, onSelect: (Region) -> Unit) {
    val palette = LocalAppPalette.current
    val type = LocalAppType.current
    regions.forEach { region ->
        val isSelected = region == selected
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) palette.accent.copy(alpha = 0.12f) else Color.Transparent)
                .pressable(role = Role.RadioButton) { onSelect(region) }
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    region.name,
                    style = type.body(13.sp, if (isSelected) FontWeight.SemiBold else FontWeight.Medium),
                    color = palette.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(region.subtitle(), style = type.body(11.sp), color = palette.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (isSelected) {
                CheckIcon(palette.accent, Modifier.size(16.dp), strokeWidth = 2.dp)
            }
        }
    }
}

/** A miniature screen with the six anchor spots; the chosen spot is drawn as a small window. */
@Composable
private fun PositionSetting(title: String, selected: WindowAnchor, onSelect: (WindowAnchor) -> Unit) {
    val palette = LocalAppPalette.current
    val type = LocalAppType.current
    val rows = listOf(
        WindowAnchor.TOP_LEFT to WindowAnchor.TOP_RIGHT,
        WindowAnchor.MIDDLE_LEFT to WindowAnchor.MIDDLE_RIGHT,
        WindowAnchor.BOTTOM_LEFT to WindowAnchor.BOTTOM_RIGHT,
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = type.body(13.sp, FontWeight.SemiBold), color = palette.onSurface)
            Text(selected.label(), style = type.body(11.sp), color = palette.muted)
        }
        Column(
            modifier = Modifier
                .width(132.dp)
                .height(84.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(palette.raised)
                .border(1.5.dp, palette.divider, RoundedCornerShape(8.dp))
                .padding(5.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            rows.forEach { (left, right) ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf(left, right).forEach { anchor ->
                        val isSelected = anchor == selected
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) palette.accent else palette.onSurface.copy(alpha = 0.07f))
                                .pressable(role = Role.RadioButton) { onSelect(anchor) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MinutesStepper(minutes: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    val palette = LocalAppPalette.current
    val type = LocalAppType.current
    var editing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(TextFieldValue(minutes.toString())) }
    var fieldHadFocus by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    fun beginEdit() {
        val text = minutes.toString()
        draft = TextFieldValue(text, TextRange(0, text.length))
        fieldHadFocus = false
        editing = true
    }

    fun commit() {
        if (!editing) return
        editing = false
        val parsed = draft.text.toIntOrNull()
        if (parsed != null) {
            onChange(parsed.coerceIn(AppState.MIN_REMINDER_MINUTES, AppState.MAX_REMINDER_MINUTES))
        } else {
            draft = TextFieldValue(minutes.toString())
        }
    }

    LaunchedEffect(minutes) { if (!editing) draft = TextFieldValue(minutes.toString()) }
    LaunchedEffect(editing) { if (editing) focusRequester.requestFocus() }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(palette.raised)
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton("−", enabled = enabled && minutes > AppState.MIN_REMINDER_MINUTES) { onChange(minutes - 1) }
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(8.dp))
                .pressable(enabled = enabled && !editing) { beginEdit() },
            contentAlignment = Alignment.Center,
        ) {
            if (editing) {
                BasicTextField(
                    value = draft,
                    onValueChange = { value ->
                        // Reject (instead of rewrite) non-digit edits so the
                        // cursor position stays intact while typing.
                        if (value.text.all(Char::isDigit) && value.text.length <= 3) draft = value
                    },
                    singleLine = true,
                    textStyle = type.figures(13.sp, FontWeight.SemiBold).copy(color = palette.onSurface, textAlign = TextAlign.Center),
                    cursorBrush = SolidColor(palette.accent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged { state ->
                            when {
                                state.isFocused -> fieldHadFocus = true
                                // The attach-time event also reports unfocused;
                                // only commit after the field really had focus.
                                fieldHadFocus && editing -> commit()
                            }
                        }
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                                commit()
                                true
                            } else {
                                false
                            }
                        },
                )
            } else {
                Text(
                    "$minutes min",
                    style = type.figures(13.sp, FontWeight.SemiBold),
                    color = if (enabled) palette.onSurface else palette.faint,
                )
            }
        }
        StepperButton("+", enabled = enabled && minutes < AppState.MAX_REMINDER_MINUTES) { onChange(minutes + 1) }
    }
}

@Composable
private fun StepperButton(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) palette.surface else Color.Transparent)
            .pressable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, style = LocalAppType.current.body(15.sp, FontWeight.SemiBold), color = if (enabled) palette.onSurface else palette.faint)
    }
}

@Composable
private fun Scrollbar(scrollState: ScrollState, modifier: Modifier = Modifier) {
    val palette = LocalAppPalette.current
    val scope = rememberCoroutineScope()
    val minThumbPx = with(LocalDensity.current) { 24.dp.toPx() }
    var trackHeight by remember { mutableFloatStateOf(0f) }
    Canvas(
        modifier = modifier
            .width(8.dp)
            .fillMaxHeight()
            .onSizeChanged { trackHeight = it.height.toFloat() }
            .pointerInput(scrollState.maxValue) {
                if (scrollState.maxValue <= 0) return@pointerInput
                detectVerticalDragGestures { change, dragAmount ->
                    change.consume()
                    val viewport = scrollState.viewportSize.coerceAtLeast(1).toFloat()
                    val contentHeight = viewport + scrollState.maxValue
                    val thumb = (trackHeight * viewport / contentHeight).coerceAtLeast(minThumbPx)
                    val travel = (trackHeight - thumb).coerceAtLeast(1f)
                    val target = scrollState.value + (dragAmount / travel * scrollState.maxValue).roundToInt()
                    scope.launch { scrollState.scrollTo(target.coerceIn(0, scrollState.maxValue)) }
                }
            },
    ) {
        val viewport = scrollState.viewportSize.toFloat()
        val maxValue = scrollState.maxValue.toFloat()
        if (viewport <= 0f || maxValue <= 0f) return@Canvas
        val pad = 2.dp.toPx()
        val track = size.height - pad * 2
        val thumb = (track * viewport / (viewport + maxValue)).coerceAtLeast(24.dp.toPx())
        val travel = (track - thumb).coerceAtLeast(1f)
        val fraction = scrollState.value / maxValue
        drawRoundRect(
            color = palette.onSurface.copy(alpha = 0.22f),
            topLeft = Offset(size.width - 4.dp.toPx(), pad + travel * fraction),
            size = Size(3.dp.toPx(), thumb),
            cornerRadius = CornerRadius(1.5.dp.toPx()),
        )
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val palette = LocalAppPalette.current
    val type = LocalAppType.current
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = type.body(13.sp, FontWeight.Medium).copy(color = palette.onSurface),
        cursorBrush = SolidColor(palette.accent),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, palette.divider, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        decorationBox = { inner ->
            Box {
                if (query.isEmpty()) {
                    Text("Search for a city or district", style = type.body(13.sp), color = palette.faint)
                }
                inner()
            }
        },
    )
}
