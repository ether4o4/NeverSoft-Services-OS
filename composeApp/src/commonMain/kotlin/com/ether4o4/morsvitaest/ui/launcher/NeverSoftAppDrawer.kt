package com.ether4o4.morsvitaest.ui.launcher

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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

/**
 * The NeverSoft OS app drawer: a searchable Start menu listing the built-in
 * NeverSoft apps and every app installed on the device, plus a PINNED tab.
 * Tap launches; long-press a NeverSoft app for pin controls (Pin-to-Start and
 * Pin-to-Taskbar are independent). Shared by the desktop Start orb and the
 * shell's Start button so both are identical.
 */
@OptIn(ExperimentalFoundationApi::class)
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
) {
    var tab by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var pinDialogFor by remember { mutableStateOf<LauncherApp?>(null) }
    val theme = resolveLauncherTheme(koinInject<AppSettings>().getLauncherTheme())
    val c = theme.content

    val q = query.trim()
    val builtInShown = apps.filter { q.isBlank() || it.label.contains(q, ignoreCase = true) }
    val installedShown = installedApps.filter { q.isBlank() || it.label.contains(q, ignoreCase = true) }
    val pinnedShown = startPins.mapNotNull { id -> apps.firstOrNull { it.id == id } }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable { onClose() },
    ) {
        val maxWpx = constraints.maxWidth.toFloat()
        val maxHpx = constraints.maxHeight.toFloat()
        var wFrac by remember { mutableFloatStateOf(0.96f) }
        var hFrac by remember { mutableFloatStateOf(0.84f) }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp)
                .fillMaxWidth(wFrac)
                .fillMaxHeight(hFrac),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .then(
                        if (theme.glass) {
                            Modifier.background(
                                Brush.verticalGradient(
                                    listOf(Color.White.copy(alpha = 0.26f), Color.White.copy(alpha = 0.13f)),
                                ),
                            )
                        } else {
                            Modifier.background(theme.panel)
                        },
                    )
                    .border(1.dp, c.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                    .clickable(enabled = false) {}
                    .padding(14.dp),
            ) {
                // Header — close on the left, resize grip lives at the top-right.
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Spacer(Modifier.width(12.dp))
                    Text("Start", color = c, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                }

                Spacer(Modifier.height(12.dp))

                // Search
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                    singleLine = true,
                    placeholder = { Text("Search apps…", color = c.copy(alpha = 0.4f)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = c.copy(alpha = 0.08f),
                        unfocusedContainerColor = c.copy(alpha = 0.08f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = c,
                        unfocusedTextColor = c,
                        cursorColor = Color(0xFF3B82F6),
                    ),
                )

                Spacer(Modifier.height(12.dp))

                // Tabs
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(c.copy(alpha = 0.08f))
                        .padding(3.dp),
                ) {
                    listOf("ALL APPS", "PINNED").forEachIndexed { i, label ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (tab == i) c.copy(alpha = 0.16f) else Color.Transparent)
                                .clickable { tab = i }
                                .padding(horizontal = 16.dp, vertical = 7.dp),
                        ) {
                            Text(
                                label,
                                color = c,
                                fontSize = 12.sp,
                                fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                if (tab == 0) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        if (builtInShown.isNotEmpty()) {
                            item { DrawerSectionLabel("NEVERSOFT", c) }
                            items(builtInShown, key = { "ns_" + it.id }) { app ->
                                AppRow(
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
                        item { DrawerSectionLabel("ALL APPS", c) }
                        if (installedApps.isEmpty()) {
                            item {
                                Text(
                                    "Loading installed apps…",
                                    color = c.copy(alpha = 0.4f),
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(12.dp),
                                )
                            }
                        } else {
                            items(installedShown, key = { it.packageName }) { app ->
                                InstalledAppRow(app, c) {
                                    onClose()
                                    onLaunchPackage(app.packageName)
                                }
                            }
                        }
                    }
                } else {
                    if (pinnedShown.isEmpty()) {
                        Text(
                            "Nothing pinned — long-press a NeverSoft app in ALL APPS.",
                            color = c.copy(alpha = 0.5f),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 30.dp),
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(pinnedShown, key = { "pin_" + it.id }) { app ->
                                AppRow(
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
                        detectDragGestures { change, drag ->
                            change.consume()
                            wFrac = (wFrac + drag.x / maxWpx).coerceIn(0.55f, 1f)
                            hFrac = (hFrac + drag.y / maxHpx).coerceIn(0.45f, 0.95f)
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
}

@Composable
private fun DrawerSectionLabel(text: String, content: Color) {
    Text(
        text,
        color = content.copy(alpha = 0.45f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 4.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppRow(
    label: String,
    color: Color,
    icon: ImageVector?,
    image: DrawableResource?,
    content: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 7.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.verticalGradient(listOf(color.copy(alpha = 0.92f), color))),
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
                Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.size(14.dp))
        Text(label, color = content, fontSize = 15.sp)
    }
}

@Composable
private fun InstalledAppRow(app: InstalledApp, content: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 7.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center,
        ) {
            val ic = app.icon
            if (ic != null) {
                Image(
                    bitmap = ic,
                    contentDescription = app.label,
                    modifier = Modifier.size(34.dp),
                )
            } else {
                Text(app.label.take(1).uppercase(), color = content, fontSize = 16.sp)
            }
        }
        Spacer(Modifier.size(14.dp))
        Text(app.label, color = content, fontSize = 15.sp, maxLines = 1)
    }
}
