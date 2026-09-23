package dev.praytime.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

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

@Composable
fun SettingsView(
    viewModel: ScheduleViewModel,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onClose: () -> Unit,
    onTestReminder: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val dayTimes by viewModel.dayTimes.collectAsState()
    val palette = LocalAppPalette.current
    val shape = RoundedCornerShape(20.dp)
    val listScrollState = rememberScrollState()

    var query by remember { mutableStateOf("") }
    var catalogResults by remember { mutableStateOf<List<Region>?>(null) }
    var recents by remember { mutableStateOf(AppState.recentRegions()) }
    var autostartEnabled by remember { mutableStateOf(Autostart.isEnabled()) }
    var reminderEnabled by remember { mutableStateOf(AppState.loadReminderEnabled()) }
    var reminderMinutes by remember { mutableStateOf(AppState.loadReminderMinutes()) }
    var reminderAnchor by remember { mutableStateOf(AppState.loadReminderAnchor()) }
    var mainAnchor by remember { mutableStateOf(AppState.loadMainAnchor()) }

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

    Box(modifier = modifier.fillMaxSize().padding(12.dp)) {
        Column(
            modifier = Modifier
                .width(272.dp)
                .shadow(6.dp, shape)
                .clip(shape)
                .background(palette.surface)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "SETTINGS",
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp),
                    color = palette.onSurface.copy(alpha = 0.7f),
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(palette.onSurface.copy(alpha = 0.1f))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "✕",
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Black),
                        color = palette.onSurface.copy(alpha = 0.75f),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            SectionLabel("REGION")
            SearchField(query = query, onQueryChange = { query = it })
            Spacer(Modifier.height(6.dp))
            Box(modifier = Modifier.fillMaxWidth().weight(1f, fill = true)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 8.dp)
                        .verticalScroll(listScrollState),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (q.isEmpty()) {
                        if (recents.isNotEmpty()) {
                            SectionLabel("RECENT")
                            RegionRows(recents, selected) { select(it) }
                        }
                        SectionLabel("ALL REGIONS")
                        RegionRows(builtInRegions, selected) { select(it) }
                    } else if (merged != null && merged.isEmpty()) {
                        Text(
                            "No region matches \"$q\"",
                            style = TextStyle(fontSize = 11.sp),
                            color = palette.onSurface.copy(alpha = 0.35f),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                        )
                    } else {
                        if (merged != null) {
                            RegionRows(merged, selected) { select(it) }
                        } else if (localMatches.isNotEmpty()) {
                            RegionRows(localMatches, selected) { select(it) }
                        }
                        if (catalogResults == null) {
                            SectionLabel("SEARCHING…")
                        }
                    }
                }
                Scrollbar(listScrollState, modifier = Modifier.align(Alignment.CenterEnd))
            }
            Spacer(Modifier.height(8.dp))
            SectionLabel("REMINDER")
            ToggleRow("Remind every $reminderMinutes min", reminderEnabled) {
                AppState.saveReminderEnabled(!reminderEnabled)
                reminderEnabled = AppState.loadReminderEnabled()
            }
            IntervalStepperRow(reminderMinutes) { minutes ->
                AppState.saveReminderMinutes(minutes)
                reminderMinutes = AppState.loadReminderMinutes()
            }
            PickerLabel("Notification position")
            AnchorGrid(reminderAnchor) { anchor ->
                reminderAnchor = anchor
                AppState.saveReminderAnchor(anchor)
            }
            TestReminderRow(onClick = onTestReminder)
            Spacer(Modifier.height(8.dp))
            SectionLabel("MAIN WINDOW")
            PickerLabel("Default position")
            AnchorGrid(mainAnchor) { anchor ->
                mainAnchor = anchor
                AppState.saveMainAnchor(anchor)
            }
            Spacer(Modifier.height(6.dp))
            ToggleRow("Start on login", autostartEnabled) {
                Autostart.setEnabled(!autostartEnabled)
                autostartEnabled = Autostart.isEnabled()
            }
            Spacer(Modifier.height(8.dp))
            SectionLabel("APPEARANCE")
            PickerLabel("Theme")
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(ThemeMode.SYSTEM, ThemeMode.DARK, ThemeMode.LIGHT).forEach { mode ->
                    val isSelected = mode == themeMode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(24.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) palette.accent else palette.onSurface.copy(alpha = 0.08f))
                            .clickable { onThemeModeChange(mode) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            mode.label(),
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            ),
                            color = if (isSelected) palette.onAccent else palette.onSurface.copy(alpha = 0.75f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    val palette = LocalAppPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
            color = palette.onSurface.copy(alpha = 0.75f),
            modifier = Modifier.weight(1f),
        )
        CheckCircle(done = checked, size = 16.dp, iconSize = 10.sp, onClick = onToggle)
    }
}

@Composable
private fun PickerLabel(text: String) {
    val palette = LocalAppPalette.current
    Text(
        text,
        style = TextStyle(fontSize = 10.sp),
        color = palette.onSurface.copy(alpha = 0.45f),
        modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 3.dp),
    )
}

@Composable
private fun AnchorGrid(selected: WindowAnchor, onSelect: (WindowAnchor) -> Unit) {
    val palette = LocalAppPalette.current
    val rows = listOf(
        listOf(WindowAnchor.TOP_LEFT, WindowAnchor.TOP_RIGHT),
        listOf(WindowAnchor.MIDDLE_LEFT, WindowAnchor.MIDDLE_RIGHT),
        listOf(WindowAnchor.BOTTOM_LEFT, WindowAnchor.BOTTOM_RIGHT),
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { anchor ->
                    val isSelected = anchor == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(24.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) palette.accent else palette.onSurface.copy(alpha = 0.08f))
                            .clickable { onSelect(anchor) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            anchor.label(),
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            ),
                            color = if (isSelected) palette.onAccent else palette.onSurface.copy(alpha = 0.75f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(label: String) {
    val palette = LocalAppPalette.current
    Text(
        label,
        style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp),
        color = palette.onSurface.copy(alpha = 0.4f),
        modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp),
    )
}

@Composable
private fun RegionRows(regions: List<Region>, selected: Region, onSelect: (Region) -> Unit) {
    val palette = LocalAppPalette.current
    regions.forEach { region ->
        val isSelected = region == selected
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 26.dp)
                .clickable { onSelect(region) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(palette.onSurface.copy(alpha = if (isSelected) 0.7f else 0.2f)),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                region.displayName,
                style = TextStyle(fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                color = palette.onSurface.copy(alpha = if (isSelected) 1f else 0.75f),
                modifier = Modifier.weight(1f).padding(vertical = 3.dp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(8.dp))
            CheckCircle(done = isSelected, size = 16.dp, iconSize = 10.sp, onClick = { onSelect(region) })
        }
    }
}

@Composable
private fun IntervalStepperRow(minutes: Int, onChange: (Int) -> Unit) {
    val palette = LocalAppPalette.current
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
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperTile("-", enabled = minutes > AppState.MIN_REMINDER_MINUTES) { onChange(minutes - 1) }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(palette.accent)
                .clickable(enabled = !editing) { beginEdit() },
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
                    textStyle = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.onAccent,
                        textAlign = TextAlign.Center,
                    ),
                    cursorBrush = SolidColor(palette.onAccent),
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
                    style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = palette.onAccent,
                )
            }
        }
        StepperTile("+", enabled = minutes < AppState.MAX_REMINDER_MINUTES) { onChange(minutes + 1) }
    }
}

@Composable
private fun StepperTile(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(palette.onSurface.copy(alpha = if (enabled) 0.08f else 0.04f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            symbol,
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Black),
            color = palette.onSurface.copy(alpha = if (enabled) 0.75f else 0.25f),
        )
    }
}

@Composable
private fun TestReminderRow(onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(palette.onSurface.copy(alpha = 0.08f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Test due reminder notification",
            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium),
            color = palette.onSurface.copy(alpha = 0.75f),
        )
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
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = palette.onSurface),
        cursorBrush = SolidColor(palette.accent),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(palette.onSurface.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        decorationBox = { inner ->
            Box {
                if (query.isEmpty()) {
                    Text(
                        "Search region…",
                        style = TextStyle(fontSize = 12.sp),
                        color = palette.onSurface.copy(alpha = 0.35f),
                    )
                }
                inner()
            }
        },
    )
}
