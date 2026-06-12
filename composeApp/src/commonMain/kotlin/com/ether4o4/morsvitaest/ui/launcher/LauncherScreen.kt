package com.ether4o4.morsvitaest.ui.launcher

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import morsvitaest.composeapp.generated.resources.Res
import morsvitaest.composeapp.generated.resources.ns_mascot
import morsvitaest.composeapp.generated.resources.ns_mascot_face
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * NeverSoft launcher — a macOS-style shell (top menu bar + bottom Dock +
 * Launchpad) that boots the app on a sleek dark desktop. Each tile opens a real
 * Mors Vita Est engine screen, so the shell runs on the actual local-GGUF /
 * sandbox / settings stack rather than a mock. The Assistant tile is the
 * NeverSoft mascot; the rest use vector icons so they render crisply at every
 * density.
 */
private data class LauncherApp(
    val label: String,
    val icon: ImageVector?,
    val image: DrawableResource?,
    val color: Color,
    val onOpen: () -> Unit,
)

@Composable
fun LauncherScreen(
    onOpenChat: () -> Unit,
    onOpenShell: () -> Unit,
    onOpenFiles: () -> Unit,
    onOpenSandbox: () -> Unit,
    onOpenModels: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var showLaunchpad by remember { mutableStateOf(false) }

    val apps = listOf(
        LauncherApp("Assistant", null, Res.drawable.ns_mascot_face, Color(0xFF050507), onOpenChat),
        LauncherApp("Terminal", Icons.Filled.Terminal, null, Color(0xFF2B2D31), onOpenShell),
        LauncherApp("Files", Icons.Filled.FolderOpen, null, Color(0xFF1C7FE0), onOpenFiles),
        LauncherApp("Sandbox", Icons.Filled.Inventory2, null, Color(0xFFE2557A), onOpenSandbox),
        LauncherApp("Models", Icons.Filled.SmartToy, null, Color(0xFF8A6CFF), onOpenModels),
        LauncherApp("Settings", Icons.Filled.Settings, null, Color(0xFF6B7077), onOpenSettings),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0C0F14), Color(0xFF060709), Color(0xFF000000)),
                ),
            ),
    ) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            MacMenuBar()

            Spacer(modifier = Modifier.weight(1f))

            // Mascot centerpiece — the NeverSoft assistant greeting you.
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(Res.drawable.ns_mascot),
                    contentDescription = "NeverSoft assistant",
                    modifier = Modifier.size(260.dp),
                )
                Text(
                    "NeverSoft OS",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Mors Vita Est",
                    color = Color(0xFFE5484D).copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Dock
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
                    apps.forEach { DockIcon(it.icon, it.image, it.label, it.color, it.onOpen) }
                    DockIcon(Icons.Filled.Apps, null, "Launchpad", Color(0xFF5A5F68)) {
                        showLaunchpad = true
                    }
                }
            }
        }

        if (showLaunchpad) {
            Launchpad(apps) { showLaunchpad = false }
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
private fun MacMenuBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xCC141418))
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(Res.drawable.ns_mascot_face),
            contentDescription = null,
            modifier = Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)),
        )
        Spacer(Modifier.width(7.dp))
        Text("NeverSoft", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(16.dp))
        Text("File", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
        Spacer(Modifier.width(14.dp))
        Text("Edit", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
        Spacer(Modifier.width(14.dp))
        Text("View", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
        Spacer(Modifier.weight(1f))
        Text("🔍   🔋   📶", color = Color.White, fontSize = 12.sp)
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
            .background(
                Brush.verticalGradient(listOf(color.copy(alpha = 0.92f), color)),
            )
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

@Composable
private fun Launchpad(apps: List<LauncherApp>, onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.86f))
            .clickable { onClose() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            apps.chunked(4).forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    row.forEach { app ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(app.color.copy(alpha = 0.92f), app.color),
                                        ),
                                    )
                                    .clickable {
                                        onClose()
                                        app.onOpen()
                                    },
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
}
