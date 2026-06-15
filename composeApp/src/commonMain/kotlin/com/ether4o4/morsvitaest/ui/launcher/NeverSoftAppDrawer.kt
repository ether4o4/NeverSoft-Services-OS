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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import dev.chrisbanes.haze.HazeState
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
    haze: HazeState? = null,
) {
    var query by remember { mutableStateOf("") }
    var pinDialogFor by remember { mutableStateOf<LauncherApp?>(null) }
    val theme = resolveLauncherTheme(koinInject<AppSettings>().getLauncherTheme())
    val c = theme.content

    // Spring open: slide up + fade + scale from the bottom-left corner it hugs.
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 380f)) }

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
        var wFrac by remember { mutableFloatStateOf(0.66f) }
        var hFrac by remember { mutableFloatStateOf(0.74f) }

        // Hugs the bottom-left; the bottom-left corner is fixed and the
        // top-right grip grows it out to the right / upward.
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 62.dp)
                .fillMaxWidth(wFrac)
                .fillMaxHeight(hFrac)
                .graphicsLayer {
                    val p = reveal.value
                    alpha = p
                    scaleX = 0.95f + 0.05f * p
                    scaleY = 0.95f + 0.05f * p
                    translationY = (1f - p) * 60f
                    transformOrigin = TransformOrigin(0f, 1f)
                },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .then(
                        if (theme.glass) {
                            Modifier.neverSoftGlass(haze)
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

                Spacer(Modifier.height(14.dp))

                // Pinned grid + All-apps grid in one scroll — Windows 11 layout.
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 76.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (pinnedShown.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) { DrawerSectionLabel("Pinned", c) }
                        items(pinnedShown, key = { "pin_" + it.id }) { app ->
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

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            DrawerSectionLabel("All apps", c)
                            Spacer(Modifier.weight(1f))
                            Text("View: Category", color = c.copy(alpha = 0.45f), fontSize = 11.sp)
                        }
                    }
                    items(builtInShown, key = { "ns_" + it.id }) { app ->
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
                    if (installedApps.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                "Loading installed apps…",
                                color = c.copy(alpha = 0.4f),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    } else {
                        items(installedShown, key = { it.packageName }) { app ->
                            InstalledAppGridTile(app, c) {
                                onClose()
                                onLaunchPackage(app.packageName)
                            }
                        }
                    }
                }

                // Account + power row pinned to the bottom (Windows 11 style).
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(50))
                            .background(c.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("N", color = c, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("NeverSoft", color = c, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("⏻", color = c.copy(alpha = 0.8f), fontSize = 16.sp)
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
                            // Bottom-left is fixed: drag right widens, drag up grows taller.
                            wFrac = (wFrac + drag.x / maxWpx).coerceIn(0.45f, 1f)
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
private fun AppGridTile(
    label: String,
    color: Color,
    icon: ImageVector?,
    image: DrawableResource?,
    content: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        modifier = Modifier
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
            if (image != null) {
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

@Composable
private fun InstalledAppGridTile(app: InstalledApp, content: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center,
        ) {
            val ic = app.icon
            if (ic != null) {
                Image(
                    bitmap = ic,
                    contentDescription = app.label,
                    modifier = Modifier.size(38.dp),
                )
            } else {
                Text(app.label.take(1).uppercase(), color = content, fontSize = 16.sp)
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            app.label,
            color = content,
            fontSize = 11.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}
