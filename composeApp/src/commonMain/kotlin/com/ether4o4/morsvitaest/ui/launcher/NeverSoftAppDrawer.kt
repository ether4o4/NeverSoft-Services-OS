package com.ether4o4.morsvitaest.ui.launcher

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ether4o4.morsvitaest.InstalledApp
import com.ether4o4.morsvitaest.data.AppSettings
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

/** A launchable app entry shared by the NeverSoft OS dock/drawer and the shell. */
internal data class LauncherApp(
    val id: String,
    val label: String,
    val icon: ImageVector?,
    val image: DrawableResource?,
    val color: Color,
    val onOpen: () -> Unit,
)

// Default pin sets. The taskbar starts empty — the user pins their own apps.
// The Start menu keeps a few handy defaults.
internal val defaultDockPins = emptyList<String>()
internal val defaultStartPins = listOf("assistant", "terminal", "files", "settings")

/** One fixed quick-launch slot at the bottom of the Start menu. */
private data class QuickSlot(val id: String, val label: String, val glyph: String)

private val quickSlots = listOf(
    QuickSlot("photos", "Photos", "🖼️"),
    QuickSlot("music", "Music", "🎵"),
    QuickSlot("files", "Files", "🗂️"),
    QuickSlot("settings", "Settings", "⚙️"),
)

/**
 * The NeverSoft OS Start menu, top to bottom: a two-card Control Panel (launcher
 * customization + the full agent/system settings), the user's pinned apps, an
 * "All apps" section split into auto-sorted category boxes (Connect / Discover /
 * Utilities / Create / Media / All), and a fixed quick-launch bar (multi-LLM
 * chat plus four user-assignable app slots). Typing in the search box collapses
 * everything into a flat results grid. Shared by the desktop Start orb and the
 * shell's Start button so both are identical.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun StartDrawer(
    apps: List<LauncherApp>,
    installedApps: List<InstalledApp>,
    startPins: List<String>,
    dockPins: List<String>,
    onToggleStartPin: (String) -> Unit,
    onToggleDockPin: (String) -> Unit,
    onLaunchPackage: (String) -> Unit,
    onClose: () -> Unit,
    onOpenGuide: () -> Unit = {},
    showGuideButton: Boolean = false,
    onOpenLauncherCustomize: () -> Unit = {},
    onOpenAgentSettings: () -> Unit = {},
    onLaunchChat: () -> Unit = {},
) {
    val settings = koinInject<AppSettings>()
    var query by remember { mutableStateOf("") }
    var pinDialogFor by remember { mutableStateOf<LauncherApp?>(null) }
    var moveDialogFor by remember { mutableStateOf<InstalledApp?>(null) }
    var pickerForSlot by remember { mutableStateOf<String?>(null) }
    // Bumped after a category override or quick-launch pick so derived groupings
    // recompose against the freshly saved settings.
    var dataVersion by remember { mutableStateOf(0) }
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    val theme = resolveLauncherTheme(settings.getLauncherTheme())
    val c = theme.content

    // Spring open: fade + scale up about the screen center it opens at.
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 380f)) }

    val q = query.trim()
    val builtInShown = apps.filter { q.isBlank() || it.label.contains(q, ignoreCase = true) }
    val installedShown = installedApps.filter { q.isBlank() || it.label.contains(q, ignoreCase = true) }
    val pinnedShown = startPins.mapNotNull { id -> apps.firstOrNull { it.id == id } }

    // Installed apps grouped into the five named boxes (recomputed when an
    // override changes). "All" always lists everything.
    val grouped = remember(installedApps, dataVersion) {
        installedApps.groupBy { effectiveCategory(it, settings) }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable { onClose() },
    ) {
        val maxWpx = constraints.maxWidth.toFloat()
        val maxHpx = constraints.maxHeight.toFloat()
        val savedSize = remember { settings.getStartMenuSize(0.66f, 0.74f) }
        var wFrac by remember { mutableFloatStateOf(savedSize.first) }
        var hFrac by remember { mutableFloatStateOf(savedSize.second) }

        // Bottom-anchored, a small gap above the taskbar. The top-right grip
        // grows it up + out (the bottom edge stays put); the size is persisted
        // so it reopens exactly where it was left.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .systemBarsPadding()
                .padding(bottom = 54.dp)
                .fillMaxWidth(wFrac)
                .fillMaxHeight(hFrac)
                .graphicsLayer {
                    val p = reveal.value
                    alpha = p
                    scaleX = 0.96f + 0.04f * p
                    scaleY = 0.96f + 0.04f * p
                    transformOrigin = TransformOrigin(0.5f, 1f)
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
                    .padding(14.dp),
            ) {
                // Compact top row — close (left); the resize grip overlays top-right.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(50))
                            .background(c.copy(alpha = 0.12f))
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("✕", color = c, fontSize = 13.sp)
                    }
                    Spacer(Modifier.weight(1f))
                }

                Spacer(Modifier.height(12.dp))

                // Search bar — Windows 11 style, full width, frosted, pill-rounded.
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)),
                    singleLine = true,
                    leadingIcon = { Text("🔍", fontSize = 14.sp) },
                    placeholder = {
                        Text(
                            "Search for apps, settings, and documents",
                            color = c.copy(alpha = 0.4f),
                            fontSize = 13.sp,
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = c.copy(alpha = 0.08f),
                        unfocusedContainerColor = c.copy(alpha = 0.08f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = c,
                        unfocusedTextColor = c,
                        cursorColor = NeverSoftAccent,
                    ),
                )

                Spacer(Modifier.height(12.dp))

                // Scrolling body. Blank query → the designed layout; otherwise a
                // flat results grid across built-in + installed apps.
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                ) {
                    if (q.isBlank()) {
                        DrawerSectionLabel("Control Panel", c)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            ControlPanelCard(
                                title = "Customize",
                                lines = listOf("Themes", "Wallpaper", "Start button", "Launcher settings"),
                                content = c,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    onClose()
                                    onOpenLauncherCustomize()
                                },
                            )
                            ControlPanelCard(
                                title = "Agent & System",
                                lines = listOf("AI models & API keys", "Linux shell & engine", "Heartbeat 24/7 & config"),
                                content = c,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    onClose()
                                    onOpenAgentSettings()
                                },
                            )
                        }

                        if (pinnedShown.isNotEmpty()) {
                            DrawerSectionLabel("Pinned", c)
                            FlowRow(modifier = Modifier.fillMaxWidth()) {
                                pinnedShown.forEach { app ->
                                    AppGridTile(
                                        label = app.label,
                                        color = app.color,
                                        icon = app.icon,
                                        image = app.image,
                                        content = c,
                                        onClick = {
                                            onClose()
                                            app.onOpen()
                                        },
                                        onLongClick = { pinDialogFor = app },
                                    )
                                }
                            }
                        }

                        DrawerSectionLabel("All apps", c)
                        if (installedApps.isEmpty()) {
                            Text(
                                "Loading installed apps…",
                                color = c.copy(alpha = 0.4f),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(8.dp),
                            )
                        } else {
                            // Five named boxes in two rows, then a full-width "All".
                            val rows = listOf(
                                listOf(AppCategory.Connect, AppCategory.Discover, AppCategory.Utilities),
                                listOf(AppCategory.Create, AppCategory.Media),
                            )
                            rows.forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    row.forEach { cat ->
                                        CategoryBox(
                                            title = cat.label,
                                            apps = grouped[cat].orEmpty(),
                                            content = c,
                                            expanded = expanded[cat.id] == true,
                                            modifier = Modifier.weight(1f),
                                            onToggle = { expanded[cat.id] = expanded[cat.id] != true },
                                            onLaunch = {
                                                onClose()
                                                onLaunchPackage(it.packageName)
                                            },
                                            onMove = { moveDialogFor = it },
                                        )
                                    }
                                    // Pad the short second row so its boxes match width.
                                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                            CategoryBox(
                                title = "All",
                                apps = installedApps,
                                content = c,
                                expanded = expanded["all"] == true,
                                modifier = Modifier.fillMaxWidth(),
                                onToggle = { expanded["all"] = expanded["all"] != true },
                                onLaunch = {
                                    onClose()
                                    onLaunchPackage(it.packageName)
                                },
                                onMove = { moveDialogFor = it },
                            )
                        }
                    } else {
                        FlowRow(modifier = Modifier.fillMaxWidth()) {
                            builtInShown.forEach { app ->
                                AppGridTile(
                                    label = app.label,
                                    color = app.color,
                                    icon = app.icon,
                                    image = app.image,
                                    content = c,
                                    onClick = {
                                        onClose()
                                        app.onOpen()
                                    },
                                    onLongClick = { pinDialogFor = app },
                                )
                            }
                            installedShown.forEach { app ->
                                AppGridTile(
                                    label = app.label,
                                    color = Color.White.copy(alpha = 0.06f),
                                    icon = null,
                                    image = null,
                                    content = c,
                                    bitmapApp = app,
                                    onClick = {
                                        onClose()
                                        onLaunchPackage(app.packageName)
                                    },
                                    onLongClick = { moveDialogFor = app },
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Quick-launch bar: multi-LLM chat (fixed) + four assignable slots.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    if (showGuideButton) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(c.copy(alpha = 0.10f))
                                .clickable {
                                    onClose()
                                    onOpenGuide()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("?", color = c.copy(alpha = 0.85f), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuickLaunchSlot(
                            label = "Chat",
                            glyph = "💬",
                            iconApp = null,
                            content = c,
                            onClick = {
                                onClose()
                                onLaunchChat()
                            },
                            onLongClick = {
                                onClose()
                                onLaunchChat()
                            },
                        )
                        quickSlots.forEach { slot ->
                            val saved = settings.getQuickLaunchApp(slot.id).let {
                                if (it.isBlank() && slot.id == "files") settings.getDefaultFileExplorer() else it
                            }
                            val iconApp = installedApps.firstOrNull { it.packageName == saved }
                            QuickLaunchSlot(
                                label = slot.label,
                                glyph = slot.glyph,
                                iconApp = iconApp,
                                content = c,
                                onClick = {
                                    if (saved.isBlank()) {
                                        pickerForSlot = slot.id
                                    } else {
                                        onClose()
                                        onLaunchPackage(saved)
                                    }
                                },
                                onLongClick = { pickerForSlot = slot.id },
                            )
                        }
                    }
                }
            }

            // Drag this top-right grip to resize the Start menu.
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(c.copy(alpha = 0.18f))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = { settings.setStartMenuSize(wFrac, hFrac) },
                        ) { change, drag ->
                            change.consume()
                            // Bottom-anchored: drag right grows width about the
                            // center; drag up grows height upward (bottom fixed).
                            wFrac = (wFrac + 2f * drag.x / maxWpx).coerceIn(0.45f, 1f)
                            hFrac = (hFrac - drag.y / maxHpx).coerceIn(0.4f, 0.92f)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("⤡", color = c, fontSize = 16.sp)
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

    moveDialogFor?.let { app ->
        AlertDialog(
            onDismissRequest = { moveDialogFor = null },
            title = { Text(app.label) },
            text = { Text("Move this app to a different box. ‘Auto’ uses the system category; ‘All only’ keeps it out of every box.") },
            confirmButton = {
                Column {
                    AppCategory.entries.forEach { cat ->
                        TextButton(onClick = {
                            settings.setAppCategoryOverride(app.packageName, cat.id)
                            dataVersion++
                            moveDialogFor = null
                        }) { Text(cat.label) }
                    }
                    TextButton(onClick = {
                        settings.setAppCategoryOverride(app.packageName, UNCATEGORIZED_ID)
                        dataVersion++
                        moveDialogFor = null
                    }) { Text("All only") }
                    TextButton(onClick = {
                        settings.setAppCategoryOverride(app.packageName, "")
                        dataVersion++
                        moveDialogFor = null
                    }) { Text("Auto") }
                }
            },
            dismissButton = {
                TextButton(onClick = { moveDialogFor = null }) { Text("Cancel") }
            },
        )
    }

    pickerForSlot?.let { slot ->
        AlertDialog(
            onDismissRequest = { pickerForSlot = null },
            title = { Text("Choose an app") },
            text = {
                Column(modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    if (installedApps.isEmpty()) {
                        Text("No installed apps found.", color = c.copy(alpha = 0.6f))
                    }
                    installedApps.forEach { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settings.setQuickLaunchApp(slot, app.packageName)
                                    dataVersion++
                                    pickerForSlot = null
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val ic = app.icon
                            if (ic != null) {
                                Image(bitmap = ic, contentDescription = app.label, modifier = Modifier.size(28.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(app.label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    settings.setQuickLaunchApp(slot, "")
                    dataVersion++
                    pickerForSlot = null
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { pickerForSlot = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun DrawerSectionLabel(text: String, content: Color) {
    Text(
        text,
        color = content.copy(alpha = 0.45f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp, start = 4.dp),
    )
}

/**
 * One Control-Panel card — a titled glass tile listing what it opens; tapping
 * anywhere opens the matching full screen.
 */
@Composable
private fun ControlPanelCard(
    title: String,
    lines: List<String>,
    content: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(content.copy(alpha = 0.07f))
            .border(1.dp, content.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(10.dp),
    ) {
        Text(title, color = content, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        lines.forEach { line ->
            Text("• $line", color = content.copy(alpha = 0.6f), fontSize = 11.sp, maxLines = 1)
        }
    }
}

/**
 * One "All apps" category box — a titled card whose installed apps wrap below.
 * Collapsed shows the first few with a “+N” chip; tapping the header expands to
 * the full set. Long-press an app to move it to a different box.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryBox(
    title: String,
    apps: List<InstalledApp>,
    content: Color,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit,
    onLaunch: (InstalledApp) -> Unit,
    onMove: (InstalledApp) -> Unit,
) {
    val cap = 8
    val shown = if (expanded) apps else apps.take(cap)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(content.copy(alpha = 0.05f))
            .border(1.dp, content.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .padding(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, color = content, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            Text("${apps.size}", color = content.copy(alpha = 0.4f), fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
            if (apps.size > cap) {
                Text(if (expanded) "▴" else "▾", color = content.copy(alpha = 0.5f), fontSize = 12.sp)
            }
        }
        if (apps.isEmpty()) {
            Text("Empty", color = content.copy(alpha = 0.3f), fontSize = 11.sp)
        } else {
            FlowRow(modifier = Modifier.fillMaxWidth()) {
                shown.forEach { app ->
                    AppMiniTile(app, content, onClick = { onLaunch(app) }, onLongClick = { onMove(app) })
                }
                if (!expanded && apps.size > cap) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onToggle() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("+${apps.size - cap}", color = content.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/** A compact, icon-only installed-app tile used inside the category boxes. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppMiniTile(
    app: InstalledApp,
    content: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(4.dp)
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        val ic = app.icon
        if (ic != null) {
            Image(bitmap = ic, contentDescription = app.label, modifier = Modifier.size(30.dp))
        } else {
            Text(app.label.take(1).uppercase(), color = content, fontSize = 14.sp)
        }
    }
}

/** A quick-launch slot tile — shows the chosen app's icon, or a placeholder glyph. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickLaunchSlot(
    label: String,
    glyph: String,
    iconApp: InstalledApp?,
    content: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(content.copy(alpha = 0.10f))
                .border(1.dp, content.copy(alpha = 0.16f), RoundedCornerShape(10.dp))
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
            contentAlignment = Alignment.Center,
        ) {
            val ic = iconApp?.icon
            if (ic != null) {
                Image(bitmap = ic, contentDescription = label, modifier = Modifier.size(28.dp))
            } else {
                Text(glyph, fontSize = 17.sp)
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(label, color = content.copy(alpha = 0.7f), fontSize = 9.sp, maxLines = 1)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppGridTile(
    label: String,
    color: Color,
    icon: ImageVector?,
    image: DrawableResource?,
    content: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    bitmapApp: InstalledApp? = null,
) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.verticalGradient(listOf(color.copy(alpha = 0.92f), color))),
            contentAlignment = Alignment.Center,
        ) {
            val bmp = bitmapApp?.icon
            if (bmp != null) {
                Image(bitmap = bmp, contentDescription = label, modifier = Modifier.size(38.dp))
            } else if (image != null) {
                Image(
                    painter = painterResource(image),
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (icon != null) {
                Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            label,
            color = content,
            fontSize = 11.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}
