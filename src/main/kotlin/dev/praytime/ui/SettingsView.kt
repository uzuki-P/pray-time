package dev.praytime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.withContext

private fun WindowAnchor.label(): String = when (this) {
    WindowAnchor.TOP_LEFT -> "Top left"
    WindowAnchor.TOP_RIGHT -> "Top right"
    WindowAnchor.MIDDLE_LEFT -> "Middle left"
    WindowAnchor.MIDDLE_RIGHT -> "Middle right"
    WindowAnchor.BOTTOM_LEFT -> "Bottom left"
    WindowAnchor.BOTTOM_RIGHT -> "Bottom right"
}

@Composable
fun SettingsView(viewModel: ScheduleViewModel, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val dayTimes by viewModel.dayTimes.collectAsState()
    val shape = RoundedCornerShape(20.dp)

    var query by remember { mutableStateOf("") }
    var catalogResults by remember { mutableStateOf<List<Region>?>(null) }
    var recents by remember { mutableStateOf(AppState.recentRegions()) }
    var autostartEnabled by remember { mutableStateOf(Autostart.isEnabled()) }
    var reminderEnabled by remember { mutableStateOf(AppState.loadReminderEnabled()) }
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
                .background(cardSurface)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "SETTINGS",
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp),
                    color = Color.White.copy(alpha = 0.7f),
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "✕",
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Black),
                        color = Color.White.copy(alpha = 0.75f),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            SectionLabel("LOCATION")
            SearchField(query = query, onQueryChange = { query = it })
            Spacer(Modifier.height(6.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (q.isEmpty()) {
                    if (recents.isNotEmpty()) {
                        SectionLabel("RECENT")
                        RegionRows(recents, selected) { select(it) }
                    }
                    SectionLabel("ALL LOCATIONS")
                    RegionRows(builtInRegions, selected) { select(it) }
                } else if (merged != null && merged.isEmpty()) {
                    Text(
                        "No location matches \"$q\"",
                        style = TextStyle(fontSize = 11.sp),
                        color = Color.White.copy(alpha = 0.35f),
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
            Spacer(Modifier.height(8.dp))
            SectionLabel("REMINDER")
            ToggleRow("Remind every 10 min", reminderEnabled) {
                AppState.saveReminderEnabled(!reminderEnabled)
                reminderEnabled = AppState.loadReminderEnabled()
            }
            PickerLabel("Notification position")
            AnchorGrid(reminderAnchor) { anchor ->
                reminderAnchor = anchor
                AppState.saveReminderAnchor(anchor)
            }
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
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
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
            color = Color.White.copy(alpha = 0.75f),
            modifier = Modifier.weight(1f),
        )
        CheckCircle(done = checked, size = 16.dp, iconSize = 10.sp, onClick = onToggle)
    }
}

@Composable
private fun PickerLabel(text: String) {
    Text(
        text,
        style = TextStyle(fontSize = 10.sp),
        color = Color.White.copy(alpha = 0.45f),
        modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 3.dp),
    )
}

@Composable
private fun AnchorGrid(selected: WindowAnchor, onSelect: (WindowAnchor) -> Unit) {
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
                            .background(if (isSelected) Color(0xFF9D8CFF) else Color.White.copy(alpha = 0.08f))
                            .clickable { onSelect(anchor) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            anchor.label(),
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            ),
                            color = if (isSelected) Color(0xFF15152B) else Color.White.copy(alpha = 0.75f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(
        label,
        style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp),
        color = Color.White.copy(alpha = 0.4f),
        modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp),
    )
}

@Composable
private fun RegionRows(regions: List<Region>, selected: Region, onSelect: (Region) -> Unit) {
    regions.forEach { region ->
        val isSelected = region == selected
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clickable { onSelect(region) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (isSelected) 0.7f else 0.2f)),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                region.displayName,
                style = TextStyle(fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                color = Color.White.copy(alpha = if (isSelected) 1f else 0.75f),
                modifier = Modifier.weight(1f),
            )
            CheckCircle(done = isSelected, size = 16.dp, iconSize = 10.sp, onClick = { onSelect(region) })
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White),
        cursorBrush = SolidColor(Color(0xFF9D8CFF)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        decorationBox = { inner ->
            Box {
                if (query.isEmpty()) {
                    Text(
                        "Search location…",
                        style = TextStyle(fontSize = 12.sp),
                        color = Color.White.copy(alpha = 0.35f),
                    )
                }
                inner()
            }
        },
    )
}
