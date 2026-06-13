package com.ether4o4.morsvitaest.ui.launcher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
 * The NeverSoft OS app drawer: two panels — ALL APPS and PINNED. Tap launches;
 * long-press opens pin controls. Pin-to-Start and Pin-to-Taskbar are
 * independent. Shared by the desktop Start orb and the shell's Start button so
 * both are identical.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun StartDrawer(
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
