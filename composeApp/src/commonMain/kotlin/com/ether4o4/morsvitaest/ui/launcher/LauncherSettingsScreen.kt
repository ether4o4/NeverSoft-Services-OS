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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ether4o4.morsvitaest.Platform
import com.ether4o4.morsvitaest.SystemSetting
import com.ether4o4.morsvitaest.currentPlatform
import com.ether4o4.morsvitaest.data.AppSettings
import com.ether4o4.morsvitaest.openSystemSetting
import com.ether4o4.morsvitaest.saveLauncherImage
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.random.Random

/** Wallpaper presets for the NeverSoft OS desktop. */
internal val launcherWallpapers = listOf(
    // NeverSoft OS — Vista-Aero blue glass, the default desktop.
    "aurora" to listOf(Color(0xFF2C5AA0), Color(0xFF3D7AB8), Color(0xFF1A3A5C), Color(0xFF0D2137)),
    "sunset" to listOf(Color(0xFF3B4B86), Color(0xFF8A6F9E), Color(0xFFC98C86), Color(0xFFE9A86B)),
    "daybreak" to listOf(Color(0xFF8EC5FC), Color(0xFFA9B7F0), Color(0xFFE0C3FC)),
    "midnight" to listOf(Color(0xFF13203A), Color(0xFF0B1426), Color(0xFF05080F)),
    "dark" to listOf(Color(0xFF0C0F14), Color(0xFF060709), Color(0xFF000000)),
)

/**
 * Launcher Settings — the NeverSoft OS shell's own settings (wallpaper, desktop
 * icons), opened from the dock gear and the app drawer. Distinct from the MVE
 * engine's AI settings (model / API keys / heartbeat), which live in the blue
 * tabs behind the Assistant.
 *
 * Kept as a full-screen entry; the window-hostable body lives in
 * [LauncherSettingsContent].
 */
@Composable
fun LauncherSettingsScreen(
    onClose: () -> Unit,
    onOpenAiSettings: () -> Unit,
) {
    LauncherAppShell(title = "Launcher Settings", onClose = onClose) {
        LauncherSettingsContent(onOpenAiSettings = onOpenAiSettings)
    }
}

/**
 * The Launcher Settings body without full-screen chrome, so it can render inside
 * a NeverSoft OS window. [onOpenAiSettings] opens the MVE engine settings.
 */
@Composable
fun LauncherSettingsContent(
    onOpenAiSettings: () -> Unit,
) {
    val settings = koinInject<AppSettings>()
    val scope = rememberCoroutineScope()
    var wallpaper by remember { mutableStateOf(settings.getLauncherWallpaper()) }
    var showLabels by remember { mutableStateOf(settings.isLauncherLabelsShown()) }
    var wallpaperImage by remember { mutableStateOf(settings.getLauncherWallpaperImage()) }
    var orbImage by remember { mutableStateOf(settings.getLauncherOrbImage()) }

    val wallpaperPicker = rememberFilePickerLauncher(type = FileKitType.Image) { file ->
        if (file != null) {
            scope.launch {
                val path = saveLauncherImage("wp_${Random.nextInt(1_000_000)}.img", file.readBytes())
                if (path != null) {
                    settings.setLauncherWallpaperImage(path)
                    wallpaperImage = path
                }
            }
        }
    }
    val orbPicker = rememberFilePickerLauncher(type = FileKitType.Image) { file ->
        if (file != null) {
            scope.launch {
                val path = saveLauncherImage("orb_${Random.nextInt(1_000_000)}.img", file.readBytes())
                if (path != null) {
                    settings.setLauncherOrbImage(path)
                    orbImage = path
                }
            }
        }
    }

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
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = if (selected) 0.12f else 0.05f))
                    .border(
                        width = if (selected) 2.dp else 0.dp,
                        color = if (selected) NeverSoftAccent else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
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
                if (selected && wallpaperImage.isBlank()) {
                    Text("✓", color = NeverSoftAccent, fontSize = 18.sp)
                }
            }
        }
        PhotoRow(
            label = if (wallpaperImage.isNotBlank()) "Custom photo set" else "Choose a photo…",
            active = wallpaperImage.isNotBlank(),
            onPick = { wallpaperPicker.launch() },
            onClear = {
                settings.setLauncherWallpaperImage("")
                wallpaperImage = ""
            },
        )

        Spacer(Modifier.height(18.dp))
        SectionLabel("Theme")
        var themeId by remember { mutableStateOf(settings.getLauncherTheme()) }
        Text(
            "Colors the taskbar, Start menu, and widgets window.",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp),
        )
        launcherThemes.forEach { t ->
            val selected = themeId == t.id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = if (selected) 0.12f else 0.05f))
                    .border(
                        width = if (selected) 2.dp else 0.dp,
                        color = if (selected) NeverSoftAccent else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .clickable {
                        themeId = t.id
                        settings.setLauncherTheme(t.id)
                    }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (t.glass) Color.White.copy(alpha = 0.3f) else t.panel)
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    t.label,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                )
                Spacer(Modifier.weight(1f))
                if (selected) Text("✓", color = NeverSoftAccent, fontSize = 18.sp)
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
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = if (selected) 0.12f else 0.05f))
                    .border(
                        width = if (selected) 2.dp else 0.dp,
                        color = if (selected) NeverSoftAccent else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
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
                if (selected && orbImage.isBlank()) {
                    Text("✓", color = NeverSoftAccent, fontSize = 18.sp)
                }
            }
        }
        PhotoRow(
            label = if (orbImage.isNotBlank()) "Custom photo set" else "Use a photo…",
            active = orbImage.isNotBlank(),
            onPick = { orbPicker.launch() },
            onClear = {
                settings.setLauncherOrbImage("")
                orbImage = ""
            },
        )

        Spacer(Modifier.height(18.dp))
        SectionLabel("Desktop")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
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

        if (currentPlatform is Platform.Mobile.Android) {
            Spacer(Modifier.height(18.dp))
            SectionLabel("Taskbar")
            var fullscreenLauncher by remember { mutableStateOf(settings.isFullscreenLauncherEnabled()) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Full-screen taskbar", color = Color.White, fontSize = 15.sp)
                    Text(
                        "Hides the system navigation bar on MorsVitaEst so the taskbar sits at " +
                            "the very bottom edge — no pill below it. Swipe up from the bottom to " +
                            "bring the system bar back.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Switch(
                    checked = fullscreenLauncher,
                    onCheckedChange = {
                        fullscreenLauncher = it
                        settings.setFullscreenLauncherEnabled(it)
                    },
                )
            }

            Spacer(Modifier.height(8.dp))
            var windowedApps by remember { mutableStateOf(settings.isWindowedAppsEnabled()) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Open apps in windows (experimental)", color = Color.White, fontSize = 15.sp)
                    Text(
                        "Opens apps in a resizable window that stops above the taskbar, instead of " +
                            "fullscreen — so the taskbar always stays put. Needs freeform windowing " +
                            "enabled on your device (Samsung: Good Lock ▸ MultiStar, or Developer " +
                            "options ▸ “Force activities to be resizable”).",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Switch(
                    checked = windowedApps,
                    onCheckedChange = {
                        windowedApps = it
                        settings.setWindowedAppsEnabled(it)
                    },
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .clickable { openSystemSetting(SystemSetting.InputMethods) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Set up MVE keyboard", color = Color.White, fontSize = 15.sp)
                    Text(
                        "MVE's keyboard rests on the taskbar's top edge instead of under it — so the " +
                            "bar can stay at the very bottom while you type, like a PC. Tap to open " +
                            "Keyboards, turn on “MVE keyboard”, then set it as your default. Switch back " +
                            "to your usual keyboard any time.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                    )
                }
                Text("›", color = Color.White.copy(alpha = 0.6f), fontSize = 22.sp)
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("AI Engine")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
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

@Composable
private fun PhotoRow(label: String, active: Boolean, onPick: () -> Unit, onClear: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = if (active) 0.12f else 0.05f))
                .border(
                    width = if (active) 2.dp else 0.dp,
                    color = if (active) NeverSoftAccent else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                )
                .clickable { onPick() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("🖼", fontSize = 16.sp)
            Spacer(Modifier.width(12.dp))
            Text(label, color = Color.White, fontSize = 15.sp)
        }
        if (active) {
            Spacer(Modifier.width(10.dp))
            Text(
                "Clear",
                color = Color(0xFFE5484D),
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onClear() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
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
