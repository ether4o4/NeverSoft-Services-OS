package com.ether4o4.morsvitaest.ui.launcher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ether4o4.morsvitaest.InstalledApp
import com.ether4o4.morsvitaest.SystemApp
import com.ether4o4.morsvitaest.SystemSetting
import com.ether4o4.morsvitaest.data.AppSettings
import com.ether4o4.morsvitaest.data.DataRepository
import com.ether4o4.morsvitaest.getInstalledApps
import com.ether4o4.morsvitaest.launchApp
import com.ether4o4.morsvitaest.openSystemApp
import com.ether4o4.morsvitaest.openSystemSetting
import com.ether4o4.morsvitaest.openUrl
import com.ether4o4.morsvitaest.persistentTaskbarHasPermission
import com.ether4o4.morsvitaest.ui.onboarding.OnDeviceAiSetup
import com.ether4o4.morsvitaest.ui.onboarding.SettingsGuide
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import morsvitaest.composeapp.generated.resources.Res
import morsvitaest.composeapp.generated.resources.ic_desk_apps
import morsvitaest.composeapp.generated.resources.ic_desk_computer
import morsvitaest.composeapp.generated.resources.ic_desk_folder
import morsvitaest.composeapp.generated.resources.ic_desk_search
import morsvitaest.composeapp.generated.resources.ic_desk_security
import morsvitaest.composeapp.generated.resources.ic_desk_settings
import morsvitaest.composeapp.generated.resources.ic_glass_messages
import morsvitaest.composeapp.generated.resources.ic_glass_phone
import morsvitaest.composeapp.generated.resources.logo
import morsvitaest.composeapp.generated.resources.ns_mascot_face
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * NeverSoft launcher — a macOS-style shell (top menu bar + far-left desktop
 * icons + bottom Dock with a Start orb + two-panel app drawer) on a sleek dark
 * desktop. Each tile opens a real Mors Vita Est engine screen, so the shell
 * runs on the actual local-GGUF / sandbox / settings stack rather than a mock.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LauncherScreen(
    onOpenChat: () -> Unit,
    onOpenShell: () -> Unit,
    onOpenFiles: () -> Unit,
    onOpenFilesAt: (String) -> Unit,
    onOpenSandbox: () -> Unit,
    onOpenModels: () -> Unit,
    onOpenLauncherSettings: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSpotlight: () -> Unit,
    onOpenStub: (String, String) -> Unit,
    newsPage: @Composable () -> Unit,
    appContent: @Composable (DesktopApp, onRequestClose: () -> Unit) -> Unit,
) {
    val settings = koinInject<AppSettings>()
    val dataRepository = koinInject<DataRepository>()
    // Bumped when the Launcher Settings window closes so a freshly picked
    // wallpaper / Start-orb photo (and theme/label changes) apply live.
    var appearanceVersion by remember { mutableStateOf(0) }
    // When the persistent overlay taskbar is active it IS the taskbar, so the
    // launcher's own in-app taskbar is hidden to avoid a doubled-up bar.
    val taskbarOverlayActive = remember(appearanceVersion) {
        settings.isPersistentTaskbarEnabled() && persistentTaskbarHasPermission()
    }
    // Bumped when a desktop shortcut is created from the Start menu so the canvas reloads.
    var desktopVersion by remember { mutableStateOf(0) }
    val wallpaperColors = remember(appearanceVersion) {
        launcherWallpapers.firstOrNull { it.first == settings.getLauncherWallpaper() }
            ?.second ?: launcherWallpapers.first().second
    }
    val showLabels = remember(appearanceVersion) { settings.isLauncherLabelsShown() }
    val orbStyle = remember(appearanceVersion) { settings.getLauncherOrbStyle() }
    val wallpaperImage = remember(appearanceVersion) { settings.getLauncherWallpaperImage() }
    val orbImage = remember(appearanceVersion) { settings.getLauncherOrbImage() }
    val theme = remember(appearanceVersion) { resolveLauncherTheme(settings.getLauncherTheme()) }
    var showDrawer by remember { mutableStateOf(false) }
    var showWidgets by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }
    var showOnDeviceAi by remember { mutableStateOf(false) }

    // Shared Haze source so the floating windows really blur the wallpaper
    // behind them (glassmorphism).
    val hazeState = remember { HazeState() }

    // The persistent overlay taskbar's Start orb opens the Start menu through here.
    val openStartMenuRequested by dataRepository.openStartMenuRequested.collectAsStateWithLifecycle()
    LaunchedEffect(openStartMenuRequested) {
        if (openStartMenuRequested) {
            showDrawer = true
            dataRepository.consumeOpenStartMenuRequest()
        }
    }

    // Once setup is done, offer the guided tour a single time; it's always
    // re-launchable from the Start menu afterward.
    LaunchedEffect(Unit) {
        if (settings.hasSeenSetup() && !settings.hasSeenGuide()) {
            showGuide = true
        } else if (settings.hasSeenSetup() && settings.hasSeenGuide() && !settings.hasOfferedOnDeviceAi()) {
            // Guided tour already done; offer the one-tap on-device AI setup once.
            showOnDeviceAi = true
        }
    }

    // ---- Window manager: apps open as floating windows over the desktop. ----
    val windows = remember { mutableStateListOf<WinState>() }
    val cascadeStepPx = with(LocalDensity.current) { 28.dp.toPx() }.toInt()

    fun raise(win: WinState) {
        val top = (windows.maxOfOrNull { it.z } ?: 0) + 1
        win.z = top
    }

    fun openWindow(app: DesktopApp) {
        val existing = windows.firstOrNull { it.app == app }
        if (existing != null) {
            existing.minimized = false
            raise(existing)
        } else {
            val n = windows.size
            val newWin = WinState(
                app = app,
                offsetX = cascadeStepPx * (n % 8),
                offsetY = cascadeStepPx * (n % 8),
                z = (windows.maxOfOrNull { it.z } ?: 0) + 1,
            )
            windows.add(newWin)
        }
    }

    fun toggleTaskbar(win: WinState) {
        val isTop = win.z == (windows.maxOfOrNull { it.z } ?: 0)
        when {
            win.minimized -> {
                win.minimized = false
                raise(win)
            }

            isTop -> win.minimized = true

            else -> raise(win)
        }
    }

    // Installed device apps for the drawer (loaded off the main thread).
    var installedApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    LaunchedEffect(Unit) { installedApps = getInstalledApps() }

    // The launcher's app catalog: every app the drawer can offer. NeverSoft apps
    // open as windows; Internet launches externally.
    val catalog = remember {
        listOf(
            LauncherApp("assistant", "Assistant", null, Res.drawable.ns_mascot_face, Color(0xFF050507)) {
                openWindow(DesktopApp.Assistant)
            },
            LauncherApp("terminal", "Terminal", Icons.Filled.Terminal, null, Color(0xFF2B2D31)) {
                openWindow(DesktopApp.Terminal)
            },
            LauncherApp("files", "Files", Icons.Filled.FolderOpen, null, Color(0xFF1C7FE0)) {
                openWindow(DesktopApp.Files)
            },
            LauncherApp("sandbox", "Sandbox", Icons.Filled.Inventory2, null, Color(0xFFE2557A)) {
                openWindow(DesktopApp.Sandbox)
            },
            LauncherApp("models", "Models", Icons.Filled.SmartToy, null, Color(0xFF8A6CFF)) {
                openWindow(DesktopApp.Settings)
            },
            LauncherApp("settings", "Settings", Icons.Filled.Settings, null, Color(0xFF6B7077)) {
                openWindow(DesktopApp.LauncherSettings)
            },
            LauncherApp("internet", "Internet", Icons.Filled.Language, null, Color(0xFF1769AA)) {
                openUrl("https://www.google.com")
            },
        )
    }
    val byId = remember(catalog) { catalog.associateBy { it.id } }

    // Independent, user-curated pin lists (persisted).
    val dockPins = remember {
        settings.getLauncherDockPins(defaultDockPins).filter { byId.containsKey(it) }.toMutableStateList()
    }
    val startPins = remember {
        settings.getLauncherStartPins(defaultStartPins).filter { byId.containsKey(it) }.toMutableStateList()
    }

    fun toggleDockPin(id: String) {
        if (dockPins.contains(id)) dockPins.remove(id) else dockPins.add(id)
        settings.setLauncherDockPins(dockPins.toList())
    }

    fun toggleStartPin(id: String) {
        if (startPins.contains(id)) startPins.remove(id) else startPins.add(id)
        settings.setLauncherStartPins(startPins.toList())
    }

    // Launches a desktop item's target: a URL, a sandbox path, or an app package.
    fun openLink(target: String) {
        when {
            target.startsWith("http://") || target.startsWith("https://") -> openUrl(target)
            target.startsWith("/") -> onOpenFilesAt(target)
            else -> if (target.isNotBlank() && !launchApp(target)) openUrl(target)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Wallpaper: a chosen photo, else the preset diagonal (top-left →
        // bottom-right) gradient — the NeverSoft OS Aurora blue glass desktop.
        if (wallpaperImage.isNotBlank()) {
            AsyncImage(
                model = "file://$wallpaperImage",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().hazeSource(hazeState),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState)
                    .background(
                        Brush.linearGradient(
                            colors = wallpaperColors,
                            start = Offset.Zero,
                            end = Offset.Infinite,
                        ),
                    ),
            )
        }

        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            // The desktop area: an empty canvas of user-created icons/folders,
            // with floating app windows layered above. The news feed and widgets
            // pages flank it.
            val pagerState = rememberPagerState(initialPage = 1) { 3 }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                var areaSize by remember { mutableStateOf(IntSize.Zero) }
                // The largest desktop height we've seen. The Activity uses adjustResize,
                // so opening the soft keyboard shrinks this area live; sizing floating
                // windows off the live height makes them shrink/jump on every keystroke.
                // Sizing off the stable max keeps a window steady — the keyboard simply
                // overlaps its lower edge (its input stays at the top, still visible).
                var stableAreaHeight by remember { mutableStateOf(0) }
                if (areaSize.height > stableAreaHeight) stableAreaHeight = areaSize.height
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = windows.none { !it.minimized },
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    when (page) {
                        0 -> newsPage()

                        1 -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .onSizeChanged { areaSize = it },
                        ) {
                            // Empty desktop canvas: user-created icons + folders;
                            // long-press for New icon / New folder / Change wallpaper.
                            DesktopCanvas(
                                installedApps = installedApps,
                                showLabels = showLabels,
                                onLaunchTarget = { openLink(it) },
                                onChangeWallpaper = { openWindow(DesktopApp.LauncherSettings) },
                                modifier = Modifier.fillMaxSize(),
                                reloadKey = desktopVersion,
                            )

                            // Floating windows (sorted by z), above icons.
                            windows.sortedBy { it.z }.forEach { win ->
                                if (!win.minimized) {
                                    WindowFrame(
                                        win = win,
                                        areaWidthPx = areaSize.width,
                                        areaHeightPx = maxOf(areaSize.height, stableAreaHeight),
                                        onFocus = { raise(win) },
                                        onMinimize = { win.minimized = true },
                                        onClose = {
                                            windows.remove(win)
                                            if (win.app == DesktopApp.LauncherSettings) appearanceVersion++
                                        },
                                        haze = hazeState,
                                    ) {
                                        appContent(win.app) {
                                            windows.remove(win)
                                            if (win.app == DesktopApp.LauncherSettings) appearanceVersion++
                                        }
                                    }
                                }
                            }
                        }

                        else -> EmptyDesktopPage()
                    }
                }
                PageDots(
                    count = 3,
                    current = pagerState.currentPage,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                )
            }

            // Edge-to-edge glass taskbar (48dp): open-window buttons + pins on the
            // left, the Start orb + Phone + Messages centered, the clock on the right.
            // Hidden when the persistent overlay taskbar is active — it replaces this bar.
            if (!taskbarOverlayActive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        // Taskbar follows the launcher theme (color + glass gradient).
                        .background(theme.surfaceBrush())
                        .drawBehind {
                            // 1px white top-edge highlight line.
                            drawLine(
                                color = Color.White.copy(alpha = 0.5f),
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                strokeWidth = 1f,
                            )
                        }
                        .padding(horizontal = 8.dp),
                ) {
                    // Left: user-pinned apps + a button per open window.
                    Row(
                        modifier = Modifier.align(Alignment.CenterStart),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        dockPins.mapNotNull { byId[it] }.forEach {
                            DockIcon(it.icon, it.image, it.label, it.color, it.onOpen)
                        }
                        val topZ = windows.maxOfOrNull { it.z } ?: 0
                        windows.forEach { win ->
                            TaskbarWindowButton(
                                label = win.app.title,
                                active = !win.minimized && win.z == topZ,
                                tint = theme.content,
                                onClick = { toggleTaskbar(win) },
                            )
                        }
                    }

                    // Center: Start orb + the Phone & Messages hollow-glass icons.
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        StartOrb(style = orbStyle, imagePath = orbImage) { showDrawer = true }
                        GlassDockIcon(Res.drawable.ic_glass_phone, "Phone") { openSystemApp(SystemApp.Phone) }
                        GlassDockIcon(Res.drawable.ic_glass_messages, "Messages") { openSystemApp(SystemApp.Messages) }
                    }

                    // Right: the two-line clock opens the widgets panel.
                    DesktopClock(
                        onClick = { showWidgets = true },
                        content = theme.content,
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp),
                    )
                }
            }
        }

        if (showDrawer) {
            StartDrawer(
                apps = catalog,
                installedApps = installedApps,
                startPins = startPins,
                dockPins = dockPins,
                onToggleStartPin = ::toggleStartPin,
                onToggleDockPin = ::toggleDockPin,
                onLaunchPackage = { launchApp(it) },
                onClose = { showDrawer = false },
                onOpenGuide = {
                    showDrawer = false
                    showGuide = true
                },
                showGuideButton = true,
                onOpenLauncherCustomize = {
                    showDrawer = false
                    openWindow(DesktopApp.LauncherSettings)
                },
                onOpenAgentSettings = {
                    showDrawer = false
                    openWindow(DesktopApp.Settings)
                },
                onLaunchChat = {
                    showDrawer = false
                    openWindow(DesktopApp.Assistant)
                },
                onLaunchShell = {
                    showDrawer = false
                    openWindow(DesktopApp.Terminal)
                },
                allowDesktopShortcut = true,
                onDesktopChanged = { desktopVersion++ },
            )
        }

        if (showWidgets) {
            NotificationsPanel(
                onClose = { showWidgets = false },
                onOpenAssistant = { openWindow(DesktopApp.Assistant) },
            )
        }

        if (showGuide) {
            SettingsGuide(
                onOpenAppearance = { openWindow(DesktopApp.LauncherSettings) },
                onOpenSystemSettings = { openWindow(DesktopApp.Settings) },
                onOpenSystemSetting = { openSystemSetting(it) },
                onFinish = {
                    showGuide = false
                    settings.setGuideSeen()
                    if (!settings.hasOfferedOnDeviceAi()) showOnDeviceAi = true
                },
            )
        }

        if (showOnDeviceAi) {
            OnDeviceAiSetup(onClose = { showOnDeviceAi = false })
        }
    }
}

/** A taskbar button — glass tile with an icon; supports tap + long-press. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskbarButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(tint.copy(alpha = 0.14f))
            .border(1.dp, tint.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/**
 * A taskbar button for one open window — a glass pill with the app title and an
 * accent bar under it when this window is focused (active). Tapping toggles
 * minimize/restore via the host.
 */
@Composable
private fun TaskbarWindowButton(
    label: String,
    active: Boolean,
    tint: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(tint.copy(alpha = if (active) 0.22f else 0.10f))
            .border(1.dp, tint.copy(alpha = if (active) 0.45f else 0.22f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            color = tint.copy(alpha = if (active) 1f else 0.8f),
            fontSize = 12.sp,
            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
        )
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .height(2.dp)
                .width(if (active) 22.dp else 0.dp)
                .clip(RoundedCornerShape(50))
                .background(if (active) NeverSoftAccent else Color.Transparent),
        )
    }
}

/** Pick which installed app the taskbar's file button should launch. */
@Composable
private fun FileExplorerChooser(
    installedApps: List<InstalledApp>,
    onPick: (String) -> Unit,
    onClose: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Choose your file explorer") },
        text = {
            if (installedApps.isEmpty()) {
                Text("Loading installed apps…")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                ) {
                    items(installedApps, key = { it.packageName }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onPick(app.packageName) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val ic = app.icon
                            if (ic != null) {
                                Image(bitmap = ic, contentDescription = app.label, modifier = Modifier.size(30.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(app.label, fontSize = 15.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } },
    )
}

/** Right page — reserved for widgets. */
@Composable
private fun EmptyDesktopPage() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Widgets coming soon", color = Color.White.copy(alpha = 0.30f), fontSize = 14.sp)
    }
}

@Composable
private fun PageDots(count: Int, current: Int, modifier: Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { i ->
            Box(
                modifier = Modifier
                    .size(if (i == current) 8.dp else 6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = if (i == current) 0.9f else 0.4f)),
            )
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
internal fun DesktopClock(onClick: () -> Unit, content: Color, modifier: Modifier) {
    var now by remember { mutableStateOf(Clock.System.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Clock.System.now()
            delay(15_000)
        }
    }
    val local = now.toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = local.hour
    val h12 = if (hour % 12 == 0) 12 else hour % 12
    val ampm = if (hour < 12) "AM" else "PM"
    val time = "$h12:${local.minute.toString().padStart(2, '0')} $ampm"
    val date = "${local.month.ordinal + 1}/${local.day}/${local.year}"
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.End,
    ) {
        Text(time, color = content, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text(date, color = content.copy(alpha = 0.6f), fontSize = 11.sp)
    }
}

/**
 * Chrome wrapper for an opened app — a floating, glass-framed window (not full
 * screen, so the desktop shows behind the dim scrim) with minimize / maximize /
 * close buttons in a glass title bar, hosting any engine screen below it.
 */
@Composable
fun LauncherAppShell(
    title: String,
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    var maximized by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.32f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .let { if (maximized) it.fillMaxSize() else it.fillMaxWidth(0.94f).fillMaxHeight(0.9f) }
                .systemBarsPadding()
                .padding(if (maximized) 0.dp else 4.dp)
                .clip(RoundedCornerShape(if (maximized) 0.dp else 18.dp))
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.30f),
                    RoundedCornerShape(if (maximized) 0.dp else 18.dp),
                ),
        ) {
            WindowTitleBar(
                title = title,
                onMinimize = onClose,
                onMaximizeToggle = { maximized = !maximized },
                onClose = onClose,
            )
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}

/** Glass title bar with minimize / maximize / close controls. */
@Composable
internal fun WindowTitleBar(
    title: String,
    onMinimize: () -> Unit,
    onMaximizeToggle: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xF22A2E38), Color(0xF21A1D24)),
                ),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        WindowButton("—", Color(0xFFFEBC2E), onMinimize)
        Spacer(Modifier.width(8.dp))
        WindowButton("▢", Color(0xFF28C840), onMaximizeToggle)
        Spacer(Modifier.width(8.dp))
        WindowButton("✕", Color(0xFFFF5F57), onClose)
    }
}

@Composable
private fun WindowButton(glyph: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.9f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, color = Color(0xFF20140A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * The Start orb — a glossy blue→cyan radial-gradient circle ringed by a soft
 * cyan (#00D4FF) glow. A user photo or alternate style (mascot / grid / logo)
 * overrides the default glyph; the glowing orb is the default look.
 */
@Composable
internal fun StartOrb(style: String, imagePath: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .size(44.dp)
            .clip(RoundedCornerShape(50))
            // Soft cyan glow ring + glossy blue→cyan radial body.
            .border(2.dp, NeverSoftAccent.copy(alpha = 0.6f), RoundedCornerShape(50))
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF8FE6FF), Color(0xFF1E7FD0), Color(0xFF0B3C73)),
                ),
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (imagePath.isNotBlank()) {
            AsyncImage(
                model = "file://$imagePath",
                contentDescription = "Start",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            return@Box
        }
        when (style) {
            "grid" -> Icon(
                imageVector = Icons.Filled.Apps,
                contentDescription = "Start",
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )

            "logo" -> Image(
                painter = painterResource(Res.drawable.logo),
                contentDescription = "Start",
                modifier = Modifier.size(26.dp),
            )

            "mascot" -> Image(
                painter = painterResource(Res.drawable.ns_mascot_face),
                contentDescription = "Start",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            // Default "orb" — a glossy white highlight over the blue body.
            else -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.0f)),
                        ),
                    ),
            )
        }
    }
}

/**
 * A taskbar tile for a hollow-glass icon — the icon image (rendered on pure
 * black, as the pack requires) sits in a small black tile so it floats the way
 * the reference renders do.
 */
@Composable
internal fun GlassDockIcon(image: DrawableResource, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(Color.Black)
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(9.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(image),
            contentDescription = label,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(9.dp)),
        )
    }
}

@Composable
internal fun DockIcon(
    icon: ImageVector?,
    image: DrawableResource?,
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.14f)),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.40f), RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            Image(
                painter = painterResource(image),
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
