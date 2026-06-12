package com.ether4o4.morsvitaest.ui.launcher

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * NeverSoft launcher — a macOS-style shell (top menu bar + bottom Dock +
 * Launchpad) that boots the app. Each tile opens a real Mors Vita Est engine
 * screen, so the desktop runs on the actual local-GGUF / sandbox / settings
 * stack rather than a mock.
 */
private data class LauncherApp(
    val label: String,
    val glyph: String,
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
        LauncherApp("Assistant", "💬", Color(0xFF1EC64A), onOpenChat),
        LauncherApp("Terminal", "🖥", Color(0xFF222428), onOpenShell),
        LauncherApp("Files", "🗂", Color(0xFF1C7FE0), onOpenFiles),
        LauncherApp("Sandbox", "📦", Color(0xFFE2557A), onOpenSandbox),
        LauncherApp("Models", "🤖", Color(0xFF8A6CFF), onOpenModels),
        LauncherApp("Settings", "⚙", Color(0xFF6B7077), onOpenSettings),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF2C5AA0), Color(0xFF1A3A5C), Color(0xFF0D2137)),
                ),
            ),
    ) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            MacMenuBar()

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
                        .background(Color.White.copy(alpha = 0.16f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    apps.forEach { DockIcon(it.glyph, it.color, it.onOpen) }
                    DockIcon("🚀", Color(0xFF5A5F68)) { showLaunchpad = true }
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
private fun DockIcon(glyph: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .size(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, fontSize = 24.sp, color = Color.White)
    }
}

@Composable
private fun Launchpad(apps: List<LauncherApp>, onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
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
                                    .background(app.color)
                                    .clickable {
                                        onClose()
                                        app.onOpen()
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(app.glyph, fontSize = 30.sp, color = Color.White)
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
