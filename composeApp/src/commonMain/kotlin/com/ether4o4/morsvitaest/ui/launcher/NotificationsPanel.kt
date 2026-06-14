package com.ether4o4.morsvitaest.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ether4o4.morsvitaest.data.AppSettings
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
 * The Widgets popup, opened from the desktop clock — a glass window of live
 * widgets (clock, weather, calendar, sticky note). No quick toggles.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun NotificationsPanel(
    onClose: () -> Unit,
    onOpenAssistant: () -> Unit,
) {
    val settings = koinInject<AppSettings>()
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
    val ampm = if (hour < 12) "AM" else "PM"
    val time = "$h12:${local.minute.toString().padStart(2, '0')}"
    val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    )
    val weekdays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val monthName = months[local.month.ordinal]
    val weekdayName = weekdays[local.date.dayOfWeek.ordinal]

    LauncherAppShell(title = "Widgets", onClose = onClose) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(Color(0xE61C2334), Color(0xE6121723))),
                )
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Clock widget
            Text(time, color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Bold)
            Text(
                "$weekdayName, $monthName ${local.day}",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 16.sp,
            )
            Spacer(Modifier.height(18.dp))

            WeatherWidget()
            Spacer(Modifier.height(14.dp))
            SystemWidget()
            Spacer(Modifier.height(14.dp))
            CalendarWidget(year = local.year, monthIndex0 = local.month.ordinal, today = local.day, monthName = monthName)
            Spacer(Modifier.height(14.dp))
            NoteWidget(settings)

            // The MVE assistant perches in the corner of the panel.
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun WidgetCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) { content() }
}

@Composable
private fun WeatherWidget() {
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
    WidgetCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 40.sp)
            Spacer(Modifier.width(16.dp))
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(temp ?: "—", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text(desc, color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp)
                }
                if (place.isNotBlank()) {
                    Text(place, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun CalendarWidget(year: Int, monthIndex0: Int, today: Int, monthName: String) {
    WidgetCard {
        Column {
            Text("$monthName $year", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                    Text(
                        it,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            val first = LocalDate(year, monthIndex0 + 1, 1)
            // Sunday-first leading blanks: DayOfWeek.ordinal Mon=0..Sun=6 -> Sun=0.
            val lead = (first.dayOfWeek.ordinal + 1) % 7
            val daysInMonth = first.daysUntil(first.plus(DatePeriod(months = 1)))
            val cells = lead + daysInMonth
            val rows = (cells + 6) / 7
            var day = 1
            for (r in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (c in 0 until 7) {
                        val index = r * 7 + c
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
                                        .background(if (isToday) Color(0xFF3B82F6) else Color.Transparent),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "$d",
                                        color = Color.White.copy(alpha = if (isToday) 1f else 0.85f),
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
private fun SystemWidget() {
    var stats by remember { mutableStateOf<com.ether4o4.morsvitaest.SystemStats?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            stats = com.ether4o4.morsvitaest.getSystemStats()
            delay(3_000)
        }
    }
    WidgetCard {
        Column {
            Text("System", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            val s = stats
            if (s == null) {
                Text("Reading…", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
            } else {
                StatRow("CPU", "${s.cpu} · ${s.cores} cores")
                val ramPct = if (s.ramTotalMb > 0) (s.ramUsedMb * 100 / s.ramTotalMb) else 0
                StatRow("RAM", "${s.ramUsedMb} / ${s.ramTotalMb} MB ($ramPct%)")
                StatRow(
                    "Storage",
                    "${(s.storageTotalGb - s.storageFreeGb).toInt()} / ${s.storageTotalGb.toInt()} GB used",
                )
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, modifier = Modifier.width(76.dp))
        Text(value, color = Color.White, fontSize = 13.sp)
    }
}

@Composable
private fun NoteWidget(settings: AppSettings) {
    var note by remember { mutableStateOf(settings.getLauncherNote()) }
    WidgetCard {
        Column {
            Text("Notes", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            TextField(
                value = note,
                onValueChange = {
                    note = it
                    settings.setLauncherNote(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Jot something down…", color = Color.White.copy(alpha = 0.4f)) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.06f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFF6AA9FF),
                ),
            )
        }
    }
}
