package com.ether4o4.morsvitaest.ui.launcher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ether4o4.morsvitaest.InstalledApp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** A launchable app entry shared by the NeverSoft OS dock/drawer and the shell. */
internal data class LauncherApp(
    val id: String,
    val label: String,
    val icon: ImageVector?,
    val image: DrawableResource?,
    val color: Color,
    val onOpen: () -> Unit,
)

// Default pin sets. Dock and Start menu pins are fully independent. The
// assistant lives in the notifications panel (and menu bar) rather than the
// dock by default — pin him back anytime from the drawer.
internal val defaultDockPins = listOf("terminal", "files", "sandbox", "models", "settings")
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

    val q = query.trim()
    val builtInShown = apps.filter { q.isBlank() || it.label.contains(q, ignoreCase = true) }
    val installedShown = installedApps.filter { q.isBlank() || it.label.contains(q, ignoreCase = true) }
    val pinnedShown = startPins.mapNotNull { id -> apps.firstOrNull { it.id == id } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable { onClose() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 44.dp, start = 10.dp, end = 10.dp, bottom = 10.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(listOf(Color(0xF21A1D24), Color(0xF20C0E12))),
                )
                .clickable(enabled = false) {}
                .padding(14.dp),
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Start", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.10f))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✕", color = Color.White, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Search
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                singleLine = true,
                placeholder = { Text("Search apps…", color = Color.White.copy(alpha = 0.4f)) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.08f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFF3B82F6),
                ),
            )

            Spacer(Modifier.height(12.dp))

            // Tabs
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(3.dp),
            ) {
                listOf("ALL APPS", "PINNED").forEachIndexed { i, label ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (tab == i) Color.White.copy(alpha = 0.16f) else Color.Transparent)
                            .clickable { tab = i }
                            .padding(horizontal = 16.dp, vertical = 7.dp),
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

            Spacer(Modifier.height(10.dp))

            if (tab == 0) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (builtInShown.isNotEmpty()) {
                        item { DrawerSectionLabel("NEVERSOFT") }
                        items(builtInShown, key = { "ns_" + it.id }) { app ->
                            AppRow(
                                label = app.label,
                                color = app.color,
                                icon = app.icon,
                                image = app.image,
                                onClick = {
                                    onClose()
                                    app.onOpen()
                                },
                                onLongClick = { pinDialogFor = app },
                            )
                        }
                    }
                    item { DrawerSectionLabel("ALL APPS") }
                    if (installedApps.isEmpty()) {
                        item {
                            Text(
                                "Loading installed apps…",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    } else {
                        items(installedShown, key = { it.packageName }) { app ->
                            InstalledAppRow(app) {
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
                        color = Color.White.copy(alpha = 0.5f),
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
private fun DrawerSectionLabel(text: String) {
    Text(
        text,
        color = Color.White.copy(alpha = 0.45f),
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
        Text(label, color = Color.White, fontSize = 15.sp)
    }
}

@Composable
private fun InstalledAppRow(app: InstalledApp, onClick: () -> Unit) {
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
                Text(app.label.take(1).uppercase(), color = Color.White, fontSize = 16.sp)
            }
        }
        Spacer(Modifier.size(14.dp))
        Text(app.label, color = Color.White, fontSize = 15.sp, maxLines = 1)
    }
}
