package com.ether4o4.morsvitaest.ui.launcher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
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
import coil3.compose.AsyncImage
import com.ether4o4.morsvitaest.InstalledApp
import com.ether4o4.morsvitaest.data.AppSettings
import com.ether4o4.morsvitaest.getInstalledApps
import com.ether4o4.morsvitaest.launchApp
import com.ether4o4.morsvitaest.openUrl
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
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * NeverSoft launcher — a macOS-style shell (top menu bar + far-left desktop
 * icons + bottom Dock with a Start orb + two-panel app drawer) on a sleek dark
 * desktop. Each tile opens a real Mors Vita Est engine screen, so the shell
 * runs on the actual local-GGUF / sandbox / settings stack rather than a mock.
 */
private data class DesktopShortcut(
    val label: String,
    val image: DrawableResource,
    val onOpen: () -> Unit,
    val onConfigure: () -> Unit = {},
)

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
    onOpenNotifications: () -> Unit,
    onOpenSpotlight: () -> Unit,
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
    val wallpaperImage = remember { settings.getLauncherWallpaperImage() }
    val orbImage = remember { settings.getLauncherOrbImage() }
    val uriHandler = LocalUriHandler.current
    var showDrawer by remember { mutableStateOf(false) }
    var showFileChooser by remember { mutableStateOf(false) }
    var defaultExplorer by remember { mutableStateOf(settings.getDefaultFileExplorer()) }

    // Installed device apps for the drawer (loaded off the main thread).
    var installedApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    LaunchedEffect(Unit) { installedApps = getInstalledApps() }

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
    fun shortcut(label: String, image: DrawableResource, builtIn: () -> Unit) = DesktopShortcut(
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
        shortcut("Search", Res.drawable.ic_desk_search) { onOpenSpotlight() },
        shortcut("Settings", Res.drawable.ic_desk_settings, onOpenLauncherSettings),
        shortcut("Recycle Bin", Res.drawable.ic_desk_trash) {
            onOpenStub("Recycle Bin", "The Recycle Bin is empty.")
        },
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Wallpaper: a chosen photo, else the preset gradient.
        if (wallpaperImage.isNotBlank()) {
            AsyncImage(
                model = "file://$wallpaperImage",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(wallpaperColors)))
        }

        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            MacMenuBar(onMascotClick = onOpenChat)

            // Empty homescreen — the user places their own icons. The
            // side pages keep the news feed and a widgets page.
            val pagerState = rememberPagerState(initialPage = 1) { 3 }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    when (page) {
                        0 -> newsPage()
                        1 -> Box(Modifier.fillMaxSize())
                        else -> EmptyDesktopPage()
                    }
                }
                PageDots(
                    count = 3,
                    current = pagerState.currentPage,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                )
            }

            // Edge-to-edge glass taskbar.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.10f)),
                        ),
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StartOrb(style = orbStyle, imagePath = orbImage) { showDrawer = true }
                Spacer(Modifier.width(6.dp))
                TaskbarButton(icon = Icons.Filled.Search, label = "Spotlight", onClick = onOpenSpotlight)
                Spacer(Modifier.width(6.dp))
                TaskbarButton(
                    icon = Icons.Filled.FolderOpen,
                    label = "Files",
                    onClick = {
                        if (defaultExplorer.isNotBlank()) launchApp(defaultExplorer)
                        else showFileChooser = true
                    },
                    onLongClick = { showFileChooser = true },
                )
                dockPins.mapNotNull { byId[it] }.forEach {
                    Spacer(Modifier.width(6.dp))
                    DockIcon(it.icon, it.image, it.label, it.color, it.onOpen)
                }
                Spacer(Modifier.weight(1f))
                DesktopClock(onClick = onOpenNotifications, modifier = Modifier.padding(end = 6.dp))
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
            )
        }

        if (showFileChooser) {
            FileExplorerChooser(
                installedApps = installedApps,
                onPick = { pkg ->
                    settings.setDefaultFileExplorer(pkg)
                    defaultExplorer = pkg
                    showFileChooser = false
                    launchApp(pkg)
                },
                onClose = { showFileChooser = false },
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

/** A taskbar button — glass tile with an icon; supports tap + long-press. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskbarButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.26f), Color.White.copy(alpha = 0.12f)),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.32f), RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
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
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Frosted-glass tile holding the icon.
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.28f), Color.White.copy(alpha = 0.12f)),
                    ),
                )
                .border(1.dp, Color.White.copy(alpha = 0.38f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(image),
                contentDescription = label,
                modifier = Modifier.size(40.dp),
            )
        }
        if (showLabel) {
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
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

/** The Start orb — a chosen photo, or a style (mascot / grid / logo). */
@Composable
private fun StartOrb(style: String, imagePath: String, onClick: () -> Unit) {
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
            .clip(RoundedCornerShape(13.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.14f)),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.40f), RoundedCornerShape(13.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            Image(
                painter = painterResource(image),
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(13.dp)),
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
