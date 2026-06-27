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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ether4o4.morsvitaest.Platform
import com.ether4o4.morsvitaest.SystemStats
import com.ether4o4.morsvitaest.currentPlatform
import com.ether4o4.morsvitaest.data.AppSettings
import com.ether4o4.morsvitaest.getSystemStats
import com.ether4o4.morsvitaest.ui.chat.ChatScreenContent
import com.ether4o4.morsvitaest.ui.chat.ChatViewModel
import com.ether4o4.morsvitaest.ui.workspace.WorkspaceScreen
import com.ether4o4.morsvitaest.ui.workspace.WorkspaceTab
import com.ether4o4.morsvitaest.weatherNow
import kotlinx.coroutines.delay
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * The live widgets surface (clock, weather, system, calendar, sticky note)
 * without any full-screen overlay chrome, so it can be hosted inside a NeverSoft
 * OS window. The host supplies the window frame, scroll, and close button.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun WidgetsContent(onOpenAssistant: () -> Unit = {}) {
    val settings = koinInject<AppSettings>()
    val theme = resolveLauncherTheme(settings.getLauncherTheme())
    val c = theme.content
    var now by remember { mutableStateOf(Clock.System.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Clock.System.now()
            // Date-resolution display: tick on the minute boundary, not every second,
            // so the whole panel (and its embedded chat) doesn't recompose 60x/min.
            delay(60_000 - now.toEpochMilliseconds() % 60_000)
        }
    }
    val local = now.toLocalDateTime(TimeZone.currentSystemDefault())
    val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    )
    val monthName = months[local.month.ordinal]

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
            .padding(16.dp),
    ) {
        // The live agent chat takes the top of the widget surface.
        WidgetAgentChat(modifier = Modifier.fillMaxWidth().weight(0.55f))
        Spacer(Modifier.height(10.dp))
        // Widgets scroll below the chat.
        Column(
            modifier = Modifier.fillMaxWidth().weight(0.45f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
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
}

/**
 * The launcher's Widgets page: the live in-app widgets (weather, system, calendar,
 * note) plus the user's chosen home-screen app widgets from any installed app
 * (Android). Everything is tinted with the launcher theme and re-tints live.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun LauncherWidgetsBoard(modifier: Modifier = Modifier) {
    val settings = koinInject<AppSettings>()
    val appearance by settings.launcherAppearanceFlow.collectAsStateWithLifecycle()
    val theme = remember(appearance) { resolveLauncherTheme(settings.getLauncherTheme()) }
    val c = theme.content
    var now by remember { mutableStateOf(Clock.System.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Clock.System.now()
            delay(60_000 - now.toEpochMilliseconds() % 60_000)
        }
    }
    val local = now.toLocalDateTime(TimeZone.currentSystemDefault())
    val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    )
    val monthName = months[local.month.ordinal]

    Column(
        modifier = modifier
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
        WeatherWidget(c)
        Spacer(Modifier.height(14.dp))
        SystemWidget(c)
        Spacer(Modifier.height(14.dp))
        CalendarWidget(local.year, local.month.ordinal, local.day, monthName, c)
        Spacer(Modifier.height(14.dp))
        NoteWidget(settings, c)
        Spacer(Modifier.height(14.dp))
        // Any installed app's home-screen widgets (Android hosts these; no-op elsewhere).
        AppWidgetsSection(contentColor = c, modifier = Modifier.fillMaxWidth())
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
    onOpenSettings: () -> Unit = {},
) {
    val settings = koinInject<AppSettings>()
    val theme = resolveLauncherTheme(settings.getLauncherTheme())
    val c = theme.content

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            // Edge-to-edge: lift the whole pop-up above the keyboard so the chat's
            // input and messages stay visible while typing instead of hiding behind it.
            .imePadding()
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

        // MULTI LLM (two-pane compare) is cramped in the compact box, so that tab takes
        // the whole screen; CHAT / SHELL stay in the bottom-right floating box.
        var activeTab by remember { mutableStateOf(WorkspaceTab.Chat) }
        val expanded = activeTab == WorkspaceTab.MultiChat

        // Hugs the bottom-right (compact) or fills the screen (expanded).
        Box(
            modifier = Modifier
                .align(if (expanded) Alignment.Center else Alignment.BottomEnd)
                .then(
                    if (expanded) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier
                            .padding(bottom = 62.dp)
                            .fillMaxWidth(wFrac)
                            .fillMaxHeight(hFrac)
                    },
                )
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
                    // The pop-up is its own floating window: neutralize the chat's
                    // status/nav-bar insets so it doesn't pad against the screen edges here
                    // (the chat applies them itself for full-screen use).
                    .consumeWindowInsets(WindowInsets.systemBars)
                    // Same glossy glass as the home boxes — themed translucent surface +
                    // gloss sheen + bright hairline — so the pop-up matches the rest of the OS.
                    .glassPanel(theme.surfaceBrush(), RoundedCornerShape(8.dp))
                    .border(glassHairline, RoundedCornerShape(8.dp))
                    .clickable(enabled = false) {}
                    .padding(16.dp),
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
                Spacer(Modifier.height(8.dp))

                // CHAT / SHELL / MULTI CHAT tabs + a settings gear — the same unified
                // workspace as Page 2, hosted inside the clock pop-up.
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    WorkspaceScreen(
                        chatViewModel = koinViewModel<ChatViewModel>(),
                        textToSpeech = null,
                        onNavigateToSettings = onOpenSettings,
                        onOpenHelp = {},
                        isSandboxAvailable = currentPlatform is Platform.Mobile.Android,
                        embedded = true,
                        onTabSelected = { activeTab = it },
                    )
                }
            }

            // Drag the top-left grip to resize (compact mode only; MULTI LLM is full-screen).
            if (!expanded) {
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
}

/**
 * The live agent chat embedded at the top of the widget / notification pop-up —
 * the real chat surface bound to a [ChatViewModel]. Because that view-model is
 * backed by the singleton DataRepository, this stays in sync with the assistant
 * conversation everywhere else.
 */
@Composable
private fun WidgetAgentChat(modifier: Modifier = Modifier) {
    val viewModel: ChatViewModel = koinViewModel()
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    Box(modifier = modifier.clip(RoundedCornerShape(10.dp))) {
        ChatScreenContent(uiState = uiState, showSettingsButton = false)
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
            delay(10_000)
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
