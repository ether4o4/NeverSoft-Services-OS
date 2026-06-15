package com.ether4o4.morsvitaest.ui.launcher

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ether4o4.morsvitaest.SystemStats
import com.ether4o4.morsvitaest.data.AppSettings
import com.ether4o4.morsvitaest.getSystemStats
import com.ether4o4.morsvitaest.weatherNow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.delay
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

/**
 * The live widgets surface (clock, weather, system, calendar, sticky note)
 * without any full-screen overlay chrome, so it can be hosted inside a NeverSoft
 * OS window. The host supplies the window frame, scroll, and close button.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun WidgetsContent() {
    val settings = koinInject<AppSettings>()
    val theme = resolveLauncherTheme(settings.getLauncherTheme())
    val c = theme.content
    var now by remember { mutableStateOf(Clock.System.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Clock.System.now()
            delay(1_000)
        }
    }
    val local = now.toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = local.hour
    val h12 = if (hour % 12 == 0) 12 else hour % 12
    val time = "$h12:${local.minute.toString().padStart(2, '0')}"
    val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    )
    val weekdays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val monthName = months[local.month.ordinal]
    val weekdayName = weekdays[local.date.dayOfWeek.ordinal]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (theme.glass) {
                    Modifier.neverSoftGlassClear()
                } else {
                    Modifier.background(theme.panel)
                },
            )
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The NS guy hangs out at the top of the widgets box, swinging and
        // switching poses.
        HangingMascot(sizeDp = 132)
        // Clock widget
        Text(time, color = c, fontSize = 64.sp, fontWeight = FontWeight.Bold)
        Text("$weekdayName, $monthName ${local.day}", color = c.copy(alpha = 0.75f), fontSize = 16.sp)
        Spacer(Modifier.height(18.dp))

        WeatherWidget(c)
        Spacer(Modifier.height(14.dp))
        SystemWidget(c)
        Spacer(Modifier.height(14.dp))
        CalendarWidget(local.year, local.month.ordinal, local.day, monthName, c)
        Spacer(Modifier.height(14.dp))
        NoteWidget(settings, c)
        Spacer(Modifier.height(8.dp))
    }
}

/**
 * The Widgets window, opened from the taskbar clock — a themed, resizable glass
 * panel of live widgets (clock, weather, system, calendar, sticky note). No
 * quick toggles. Drag the top-right grip to resize, just like the Start menu.
 *
 * Kept as a full-screen overlay entry; the window-hostable surface lives in
 * [WidgetsContent].
 */
@OptIn(ExperimentalTime::class)
@Composable
fun NotificationsPanel(
    onClose: () -> Unit,
    onOpenAssistant: () -> Unit,
) {
    val settings = koinInject<AppSettings>()
    val theme = resolveLauncherTheme(settings.getLauncherTheme())
    val c = theme.content
    var now by remember { mutableStateOf(Clock.System.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Clock.System.now()
            delay(1_000)
        }
    }
    val local = now.toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = local.hour
    val h12 = if (hour % 12 == 0) 12 else hour % 12
    val time = "$h12:${local.minute.toString().padStart(2, '0')}"
    val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    )
    val weekdays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val monthName = months[local.month.ordinal]
    val weekdayName = weekdays[local.date.dayOfWeek.ordinal]

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable { onClose() },
    ) {
        val maxWpx = constraints.maxWidth.toFloat()
        val maxHpx = constraints.maxHeight.toFloat()
        val savedSize = remember { settings.getWidgetPanelSize(0.66f, 0.74f) }
        var wFrac by remember { mutableFloatStateOf(savedSize.first) }
        var hFrac by remember { mutableFloatStateOf(savedSize.second) }

        // Spring open: slide in + fade + scale from the bottom-right corner it hugs.
        val reveal = remember { Animatable(0f) }
        LaunchedEffect(Unit) { reveal.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 380f)) }

        // Hugs the bottom-right; the bottom-right corner is fixed and the
        // top-left grip grows it out to the left / upward.
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 62.dp)
                .fillMaxWidth(wFrac)
                .fillMaxHeight(hFrac)
                .graphicsLayer {
                    val p = reveal.value
                    alpha = p
                    scaleX = 0.95f + 0.05f * p
                    scaleY = 0.95f + 0.05f * p
                    translationY = (1f - p) * 60f
                    transformOrigin = TransformOrigin(1f, 1f)
                },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .then(
                        if (theme.glass) {
                            Modifier.neverSoftGlassClear()
                        } else {
                            Modifier.background(theme.panel)
                        },
                    )
                    .border(1.dp, c.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .clickable(enabled = false) {}
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Header — resize grip is top-left, so close sits top-right.
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(50))
                            .background(c.copy(alpha = 0.12f))
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("✕", color = c, fontSize = 14.sp)
                    }
                }

                // Clock widget
                Text(time, color = c, fontSize = 64.sp, fontWeight = FontWeight.Bold)
                Text("$weekdayName, $monthName ${local.day}", color = c.copy(alpha = 0.75f), fontSize = 16.sp)
                Spacer(Modifier.height(18.dp))

                WeatherWidget(c)
                Spacer(Modifier.height(14.dp))
                SystemWidget(c)
                Spacer(Modifier.height(14.dp))
                CalendarWidget(local.year, local.month.ordinal, local.day, monthName, c)
                Spacer(Modifier.height(14.dp))
                NoteWidget(settings, c)
                Spacer(Modifier.height(8.dp))
            }

            // Drag the top-left grip to resize; bottom-right corner stays put.
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .size(30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(c.copy(alpha = 0.18f))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = { settings.setWidgetPanelSize(wFrac, hFrac) },
                        ) { change, drag ->
                            change.consume()
                            // Bottom-right is fixed: drag left widens, drag up grows taller.
                            wFrac = (wFrac - drag.x / maxWpx).coerceIn(0.45f, 1f)
                            hFrac = (hFrac - drag.y / maxHpx).coerceIn(0.4f, 0.92f)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("⤢", color = c, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun WidgetCard(content: Color, body: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(content.copy(alpha = 0.08f))
            .border(1.dp, content.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
            .padding(16.dp),
    ) { body() }
}

@Composable
private fun WeatherWidget(c: Color) {
    var temp by remember { mutableStateOf<String?>(null) }
    var desc by remember { mutableStateOf("Loading weather…") }
    var emoji by remember { mutableStateOf("⛅") }
    var place by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val w = weatherNow()
        if (w != null) {
            temp = "${w.temperatureF}°F"
            desc = w.description
            emoji = w.emoji
            place = w.place
        } else {
            desc = "Weather unavailable"
        }
    }
    WidgetCard(c) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 40.sp)
            Spacer(Modifier.width(16.dp))
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(temp ?: "—", color = c, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text(desc, color = c.copy(alpha = 0.8f), fontSize = 15.sp)
                }
                if (place.isNotBlank()) {
                    Text(place, color = c.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SystemWidget(c: Color) {
    var stats by remember { mutableStateOf<SystemStats?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            stats = getSystemStats()
            delay(3_000)
        }
    }
    WidgetCard(c) {
        Column {
            Text("System", color = c, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            val s = stats
            if (s == null) {
                Text("Reading…", color = c.copy(alpha = 0.5f), fontSize = 13.sp)
            } else {
                StatRow("CPU", "${s.cpu} · ${s.cores} cores", c)
                val ramPct = if (s.ramTotalMb > 0) (s.ramUsedMb * 100 / s.ramTotalMb) else 0
                StatRow("RAM", "${s.ramUsedMb} / ${s.ramTotalMb} MB ($ramPct%)", c)
                StatRow(
                    "Storage",
                    "${(s.storageTotalGb - s.storageFreeGb).toInt()} / ${s.storageTotalGb.toInt()} GB used",
                    c,
                )
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, c: Color) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = c.copy(alpha = 0.6f), fontSize = 13.sp, modifier = Modifier.width(76.dp))
        Text(value, color = c, fontSize = 13.sp)
    }
}

@Composable
private fun CalendarWidget(year: Int, monthIndex0: Int, today: Int, monthName: String, c: Color) {
    WidgetCard(c) {
        Column {
            Text("$monthName $year", color = c, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                    Text(
                        it,
                        color = c.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            val first = LocalDate(year, monthIndex0 + 1, 1)
            val lead = (first.dayOfWeek.ordinal + 1) % 7
            val daysInMonth = first.daysUntil(first.plus(DatePeriod(months = 1)))
            val rows = (lead + daysInMonth + 6) / 7
            var day = 1
            for (r in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val index = r * 7 + col
                        if (index < lead || day > daysInMonth) {
                            Spacer(Modifier.weight(1f).height(30.dp))
                        } else {
                            val d = day
                            val isToday = d == today
                            Box(
                                modifier = Modifier.weight(1f).height(30.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(if (isToday) NeverSoftAccent else Color.Transparent),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "$d",
                                        color = if (isToday) Color.White else c.copy(alpha = 0.85f),
                                        fontSize = 13.sp,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    )
                                }
                            }
                            day++
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteWidget(settings: AppSettings, c: Color) {
    var note by remember { mutableStateOf(settings.getLauncherNote()) }
    WidgetCard(c) {
        Column {
            Text("Notes", color = c, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            TextField(
                value = note,
                onValueChange = {
                    note = it
                    settings.setLauncherNote(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Jot something down…", color = c.copy(alpha = 0.4f)) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = c.copy(alpha = 0.06f),
                    unfocusedContainerColor = c.copy(alpha = 0.06f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = c,
                    unfocusedTextColor = c,
                    cursorColor = NeverSoftAccent,
                ),
            )
        }
    }
}
