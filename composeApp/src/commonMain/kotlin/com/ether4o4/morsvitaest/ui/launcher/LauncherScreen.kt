package com.ether4o4.morsvitaest.ui.launcher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Language
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ether4o4.morsvitaest.data.AppSettings
import com.ether4o4.morsvitaest.launchApp
import com.ether4o4.morsvitaest.openUrl
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import morsvitaest.composeapp.generated.resources.Res
import morsvitaest.composeapp.generated.resources.ic_desk_apps
import morsvitaest.composeapp.generated.resources.ic_desk_computer
import morsvitaest.composeapp.generated.resources.ic_desk_documents
import morsvitaest.composeapp.generated.resources.ic_desk_folder
import morsvitaest.composeapp.generated.resources.ic_desk_internet
import morsvitaest.composeapp.generated.resources.ic_desk_search
import morsvitaest.composeapp.generated.resources.ic_desk_security
import morsvitaest.composeapp.generated.resources.ic_desk_settings
import morsvitaest.composeapp.generated.resources.ic_desk_trash
import morsvitaest.composeapp.generated.resources.logo
import morsvitaest.composeapp.generated.resources.ns_mascot_face
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

/**
 * NeverSoft launcher — a macOS-style shell (top menu bar + far-left desktop
 * icons + bottom Dock with a Start orb + two-panel app drawer) on a sleek dark
 * desktop. Each tile opens a real Mors Vita Est engine screen, so the shell
 * runs on the actual local-GGUF / sandbox / settings stack rather than a mock.
 */
private data class LauncherApp(
    val id: String,
    val label: String,
    val icon: ImageVector?,
    val image: DrawableResource?,
    val color: Color,
    val onOpen: () -> Unit,
)

private data class DesktopShortcut(
    val label: String,
    val image: DrawableResource,
    val onOpen: () -> Unit,
    val onConfigure: () -> Unit = {},
)

// Default pin sets. Dock and Start menu pins are fully independent.
private val defaultDockPins = listOf("assistant", "terminal", "files", "sandbox", "models", "settings")
private val defaultStartPins = listOf("assistant", "terminal", "files", "settings")

// Stable ids for the desktop icons' persisted launch links.
private val desktopIconIds = listOf(
    "Internet", "Computer", "Documents", "Files", "Apps",
    "Security", "Search", "Settings", "Recycle Bin",
)

@Composable
fun LauncherScreen(
    onOpenChat: () -> Unit,
    onOpenShell: () -> Unit,
    onOpenFiles: () -> Unit,
    onOpenFilesAt: (String) -> Unit,
    onOpenSandbox: () -> Unit,
    onOpenModels: () -> Unit,
    onOpenLauncherSettings: () -> Unit,
    onOpenStub: (String, String) -> Unit,
    newsPage: @Composable () -> Unit,
) {
    val settings = koinInject<AppSettings>()
    val wallpaperColors = remember {
        launcherWallpapers.firstOrNull { it.first == settings.getLauncherWallpaper() }
            ?.second ?: launcherWallpapers.first().second
    }
    val showLabels = remember { settings.isLauncherLabelsShown() }
    val orbStyle = remember { settings.getLauncherOrbStyle() }
    val uriHandler = LocalUriHandler.current
    var showDrawer by remember { mutableStateOf(false) }

    // The launcher's app catalog: every app the drawer can offer.
    val catalog = remember(onOpenChat, onOpenShell, onOpenFiles, onOpenSandbox, onOpenModels, onOpenLauncherSettings) {
        listOf(
            LauncherApp("assistant", "Assistant", null, Res.drawable.ns_mascot_face, Color(0xFF050507), onOpenChat),
            LauncherApp("terminal", "Terminal", Icons.Filled.Terminal, null, Color(0xFF2B2D31), onOpenShell),
            LauncherApp("files", "Files", Icons.Filled.FolderOpen, null, Color(0xFF1C7FE0), onOpenFiles),
            LauncherApp("sandbox", "Sandbox", Icons.Filled.Inventory2, null, Color(0xFFE2557A), onOpenSandbox),
            LauncherApp("models", "Models", Icons.Filled.SmartToy, null, Color(0xFF8A6CFF), onOpenModels),
            LauncherApp("settings", "Settings", Icons.Filled.Settings, null, Color(0xFF6B7077), onOpenLauncherSettings),
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

    // Per-icon launch links (URL / sandbox path / app package), persisted.
    // Long-press an icon to set; tap then launches the linked target.
    val iconLinks = remember {
        mutableStateMapOf<String, String>().apply {
            desktopIconIds.forEach { id ->
                settings.getLauncherIconLink(id).takeIf { it.isNotBlank() }?.let { put(id, it) }
            }
        }
    }
    var linkDialogFor by remember { mutableStateOf<String?>(null) }

    fun openLink(target: String) {
        when {
            target.startsWith("http://") || target.startsWith("https://") -> openUrl(target)
            target.startsWith("/") -> onOpenFilesAt(target)
            else -> if (!launchApp(target)) openUrl(target)
        }
    }

    // Wraps a built-in action so a user-assigned link wins when present.
    fun shortcut(label: String, image: DrawableResource, builtIn: () -> Unit) =
        DesktopShortcut(
            label = label,
            image = image,
            onOpen = {
                val link = iconLinks[label]
                if (link.isNullOrBlank()) builtIn() else openLink(link)
            },
            onConfigure = { linkDialogFor = label },
        )

    val shortcuts = listOf(
        shortcut("Internet", Res.drawable.ic_desk_internet) {
            runCatching { uriHandler.openUri("https://www.google.com") }
        },
        shortcut("Computer", Res.drawable.ic_desk_computer, onOpenShell),
        shortcut("Documents", Res.drawable.ic_desk_documents, onOpenFiles),
        shortcut("Files", Res.drawable.ic_desk_folder, onOpenFiles),
        shortcut("Apps", Res.drawable.ic_desk_apps) { showDrawer = true },
        shortcut("Security", Res.drawable.ic_desk_security) {
            onOpenStub("Security", "NeverSoft Security — coming soon.")
        },
        shortcut("Search", Res.drawable.ic_desk_search) {
            onOpenStub("Spotlight", "Search — coming soon.")
        },
        shortcut("Settings", Res.drawable.ic_desk_settings, onOpenLauncherSettings),
        shortcut("Recycle Bin", Res.drawable.ic_desk_trash) {
            onOpenStub("Recycle Bin", "The Recycle Bin is empty.")
        },
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(wallpaperColors)),
    ) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            MacMenuBar(onMascotClick = onOpenChat)

            val pagerState = rememberPagerState(initialPage = 1) { 3 }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    when (page) {
                        0 -> newsPage()
                        1 -> DesktopPage(shortcuts, showLabels)
                        else -> EmptyDesktopPage()
                    }
                }
                PageDots(
                    count = 3,
                    current = pagerState.currentPage,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                )
                DesktopClock(
                    onClick = { onOpenStub("Notifications", "No new notifications.") },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 4.dp),
                )
            }

            // Dock: Start orb + user-pinned apps.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StartOrb(style = orbStyle) { showDrawer = true }
                    dockPins.mapNotNull { byId[it] }.forEach {
                        DockIcon(it.icon, it.image, it.label, it.color, it.onOpen)
                    }
                }
            }
        }

        if (showDrawer) {
            StartDrawer(
                apps = catalog,
                startPins = startPins,
                dockPins = dockPins,
                onToggleStartPin = ::toggleStartPin,
                onToggleDockPin = ::toggleDockPin,
                onClose = { showDrawer = false },
            )
        }

        linkDialogFor?.let { iconId ->
            var text by remember(iconId) { mutableStateOf(iconLinks[iconId] ?: "") }
            AlertDialog(
                onDismissRequest = { linkDialogFor = null },
                title = { Text("Link “$iconId”") },
                text = {
                    Column {
                        Text(
                            "Paste what this icon should launch:\n" +
                                "• https:// web link\n" +
                                "• /path — opens Files at that sandbox path\n" +
                                "• app package (e.g. com.android.chrome)",
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            singleLine = true,
                            placeholder = { Text("https://…  /root/…  com.app…") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        settings.setLauncherIconLink(iconId, text)
                        if (text.isBlank()) iconLinks.remove(iconId) else iconLinks[iconId] = text
                        linkDialogFor = null
                    }) { Text("Save") }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            settings.setLauncherIconLink(iconId, "")
                            iconLinks.remove(iconId)
                            linkDialogFor = null
                        }) { Text("Clear") }
                        TextButton(onClick = { linkDialogFor = null }) { Text("Cancel") }
                    }
                },
            )
        }
    }
}

/** Center "home" page: the far-left classic icons over the wallpaper. */
@Composable
private fun DesktopPage(
    shortcuts: List<DesktopShortcut>,
    showLabels: Boolean,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Windows-style icon flow: fill a column to the screen height, then
        // wrap into the next column so nothing hides behind the dock.
        val cellHeight = 96.dp
        val perColumn = maxOf(3, (maxHeight / cellHeight).toInt())
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 10.dp, start = 6.dp),
        ) {
            shortcuts.chunked(perColumn).forEach { columnIcons ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    columnIcons.forEach {
                        DesktopIcon(it.image, it.label, showLabels, it.onOpen, it.onConfigure)
                    }
                }
            }
        }
    }
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
private fun DesktopClock(onClick: () -> Unit, modifier: Modifier) {
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
        Text(time, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text(date, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DesktopIcon(
    image: DrawableResource,
    label: String,
    showLabel: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .padding(vertical = 6.dp)
            .width(76.dp)
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(image),
            contentDescription = label,
            modifier = Modifier.size(48.dp),
        )
        if (showLabel) {
            Spacer(Modifier.height(3.dp))
            Text(
                label,
                color = Color.White,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

/**
 * Chrome wrapper for an opened app — a thin macOS-style title bar with a
 * traffic-light "close" dot that returns to the desktop, hosting any engine
 * screen below it.
 */
@Composable
fun LauncherAppShell(
    title: String,
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1B1D22))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFFF5F57))
                    .clickable { onClose() },
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFFEBC2E)),
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF28C840)),
            )
            Spacer(Modifier.width(14.dp))
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

@Composable
private fun MacMenuBar(onMascotClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xCC141418))
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("NeverSoft", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Text("🔍   🔋   📶", color = Color.White, fontSize = 12.sp)
        Spacer(Modifier.width(10.dp))
        // The NS guy lives at the top right of the bar; tap to summon him.
        Image(
            painter = painterResource(Res.drawable.ns_mascot_face),
            contentDescription = "NeverSoft assistant",
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(5.dp))
                .clickable { onMascotClick() },
        )
    }
}

/** The Start orb — customizable via Launcher Settings (mascot / grid / logo). */
@Composable
private fun StartOrb(style: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .size(46.dp)
            .clip(RoundedCornerShape(50))
            .background(
                Brush.verticalGradient(listOf(Color(0xFF20242B), Color(0xFF0E1014))),
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        when (style) {
            "grid" -> Icon(
                imageVector = Icons.Filled.Apps,
                contentDescription = "Start",
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )

            "logo" -> Image(
                painter = painterResource(Res.drawable.logo),
                contentDescription = "Start",
                modifier = Modifier.size(28.dp),
            )

            else -> Image(
                painter = painterResource(Res.drawable.ns_mascot_face),
                contentDescription = "Start",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun DockIcon(
    icon: ImageVector?,
    image: DrawableResource?,
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .size(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(color.copy(alpha = 0.92f), color)))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            Image(
                painter = painterResource(image),
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

/**
 * The Start drawer: two panels — ALL APPS and PINNED. Tap launches; long-press
 * opens pin controls. Pin-to-Start and Pin-to-Taskbar are independent.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StartDrawer(
    apps: List<LauncherApp>,
    startPins: List<String>,
    dockPins: List<String>,
    onToggleStartPin: (String) -> Unit,
    onToggleDockPin: (String) -> Unit,
    onClose: () -> Unit,
) {
    var tab by remember { mutableStateOf(0) }
    var pinDialogFor by remember { mutableStateOf<LauncherApp?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.86f))
            .clickable { onClose() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 56.dp)
                .clickable(enabled = false) {},
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Panel tabs
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .padding(4.dp),
            ) {
                listOf("ALL APPS", "PINNED").forEachIndexed { i, label ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (tab == i) Color.White.copy(alpha = 0.18f) else Color.Transparent)
                            .clickable { tab = i }
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                    ) {
                        Text(
                            label,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            val shown = if (tab == 0) apps else startPins.mapNotNull { id -> apps.firstOrNull { it.id == id } }
            if (shown.isEmpty()) {
                Text(
                    "Nothing pinned yet — long-press an app in ALL APPS.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 40.dp),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                ) {
                    items(shown, key = { it.id }) { app ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 14.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(app.color.copy(alpha = 0.92f), app.color),
                                        ),
                                    )
                                    .combinedClickable(
                                        onClick = {
                                            onClose()
                                            app.onOpen()
                                        },
                                        onLongClick = { pinDialogFor = app },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (app.image != null) {
                                    Image(
                                        painter = painterResource(app.image),
                                        contentDescription = app.label,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else if (app.icon != null) {
                                    Icon(
                                        imageVector = app.icon,
                                        contentDescription = app.label,
                                        tint = Color.White,
                                        modifier = Modifier.size(34.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(app.label, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    pinDialogFor?.let { app ->
        val inStart = startPins.contains(app.id)
        val inDock = dockPins.contains(app.id)
        AlertDialog(
            onDismissRequest = { pinDialogFor = null },
            title = { Text(app.label) },
            text = { Text("Pin to Start and Pin to Taskbar are separate — set each how you like.") },
            confirmButton = {
                Column {
                    TextButton(onClick = {
                        onToggleStartPin(app.id)
                        pinDialogFor = null
                    }) { Text(if (inStart) "Unpin from Start" else "Pin to Start") }
                    TextButton(onClick = {
                        onToggleDockPin(app.id)
                        pinDialogFor = null
                    }) { Text(if (inDock) "Unpin from Taskbar" else "Pin to Taskbar") }
                }
            },
            dismissButton = {
                TextButton(onClick = { pinDialogFor = null }) { Text("Cancel") }
            },
        )
    }
}
