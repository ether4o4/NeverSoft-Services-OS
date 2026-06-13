package com.ether4o4.morsvitaest.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ether4o4.morsvitaest.data.AppSettings
import org.koin.compose.koinInject

/** Wallpaper presets for the NeverSoft OS desktop. */
internal val launcherWallpapers = listOf(
    "dark" to listOf(Color(0xFF0C0F14), Color(0xFF060709), Color(0xFF000000)),
    "midnight" to listOf(Color(0xFF13203A), Color(0xFF0B1426), Color(0xFF05080F)),
    "blue" to listOf(Color(0xFF2C5AA0), Color(0xFF1A3A5C), Color(0xFF0D2137)),
)

/**
 * Launcher Settings — the NeverSoft OS shell's own settings (wallpaper, desktop
 * icons), opened from the dock gear and the app drawer. Distinct from the MVE
 * engine's AI settings (model / API keys / heartbeat), which live in the blue
 * tabs behind the Assistant.
 */
@Composable
fun LauncherSettingsScreen(
    onClose: () -> Unit,
    onOpenAiSettings: () -> Unit,
) {
    val settings = koinInject<AppSettings>()
    var wallpaper by remember { mutableStateOf(settings.getLauncherWallpaper()) }
    var showLabels by remember { mutableStateOf(settings.isLauncherLabelsShown()) }

    LauncherAppShell(title = "Launcher Settings", onClose = onClose) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0D11))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            SectionLabel("Wallpaper")
            launcherWallpapers.forEach { (id, colors) ->
                val selected = wallpaper == id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = if (selected) 0.12f else 0.05f))
                        .border(
                            width = if (selected) 2.dp else 0.dp,
                            color = if (selected) Color(0xFF3B82F6) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .clickable {
                            wallpaper = id
                            settings.setLauncherWallpaper(id)
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(androidx.compose.ui.graphics.Brush.verticalGradient(colors)),
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        id.replaceFirstChar { it.uppercase() },
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    )
                    Spacer(Modifier.weight(1f))
                    if (selected) Text("✓", color = Color(0xFF3B82F6), fontSize = 18.sp)
                }
            }

            Spacer(Modifier.height(18.dp))
            SectionLabel("Start Orb")
            var orb by remember { mutableStateOf(settings.getLauncherOrbStyle()) }
            listOf("mascot" to "NS Mascot", "grid" to "App Grid", "logo" to "NeverSoft Logo").forEach { (id, label) ->
                val selected = orb == id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = if (selected) 0.12f else 0.05f))
                        .border(
                            width = if (selected) 2.dp else 0.dp,
                            color = if (selected) Color(0xFF3B82F6) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .clickable {
                            orb = id
                            settings.setLauncherOrbStyle(id)
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        label,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    )
                    Spacer(Modifier.weight(1f))
                    if (selected) Text("✓", color = Color(0xFF3B82F6), fontSize = 18.sp)
                }
            }

            Spacer(Modifier.height(18.dp))
            SectionLabel("Desktop")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Show icon labels", color = Color.White, fontSize = 15.sp)
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = showLabels,
                    onCheckedChange = {
                        showLabels = it
                        settings.setLauncherLabelsShown(it)
                    },
                )
            }

            Spacer(Modifier.height(18.dp))
            SectionLabel("AI Engine")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .clickable { onOpenAiSettings() }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("MVE AI Settings", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(
                        "Model, API keys, heartbeat — opens the Assistant settings",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                    )
                }
                Text("›", color = Color.White.copy(alpha = 0.6f), fontSize = 22.sp)
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "NeverSoft OS · Mors Vita Est",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = Color(0xFF8A9099),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp),
    )
}
