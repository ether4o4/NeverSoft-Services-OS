package com.ether4o4.morsvitaest.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ether4o4.morsvitaest.data.AppSettings
import com.ether4o4.morsvitaest.ui.DarkColorScheme
import com.ether4o4.morsvitaest.ui.Theme
import com.ether4o4.morsvitaest.ui.launcher.DesktopClock
import com.ether4o4.morsvitaest.ui.launcher.DockIcon
import com.ether4o4.morsvitaest.ui.launcher.GlassDockIcon
import com.ether4o4.morsvitaest.ui.launcher.LauncherApp
import com.ether4o4.morsvitaest.ui.launcher.StartOrb
import com.ether4o4.morsvitaest.ui.launcher.defaultDockPins
import com.ether4o4.morsvitaest.ui.launcher.resolveLauncherTheme
import com.ether4o4.morsvitaest.ui.launcher.surfaceBrush
import morsvitaest.composeapp.generated.resources.Res
import morsvitaest.composeapp.generated.resources.ic_glass_messages
import morsvitaest.composeapp.generated.resources.ic_glass_phone
import morsvitaest.composeapp.generated.resources.ns_mascot_face
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

/**
 * The persistent taskbar drawn in the overlay, hosting the **same** glass bar the in-app
 * launcher uses (theme gradient, top-edge highlight, pinned-app icons, Start orb, Phone /
 * Messages glass icons, clock) — so the over-apps taskbar is identical to the launcher's.
 *
 * Unlike the in-app bar there are no MVE windows over other apps, so it omits the
 * open-window buttons; everything else matches. The window is sized taller than the 48dp
 * bar so it sits flush to the bottom over the gesture pill — the bar fills that strip too.
 */
@Composable
fun OverlayTaskbarBar(
    onOrb: () -> Unit,
    onClock: () -> Unit,
    onPhone: () -> Unit,
    onMessages: () -> Unit,
    onDockPin: () -> Unit,
) {
    KoinContext {
        Theme(colorScheme = DarkColorScheme) {
            val settings = koinInject<AppSettings>()
            val theme = remember { resolveLauncherTheme(settings.getLauncherTheme()) }
            val orbStyle = remember { settings.getLauncherOrbStyle() }
            val orbImage = remember { settings.getLauncherOrbImage() }

            // Built-in apps, for resolving pinned dock icons. Over other apps they bring
            // the launcher forward rather than opening an MVE window.
            val catalog = remember {
                listOf(
                    LauncherApp("assistant", "Assistant", null, Res.drawable.ns_mascot_face, Color(0xFF050507), onDockPin),
                    LauncherApp("terminal", "Terminal", Icons.Filled.Terminal, null, Color(0xFF2B2D31), onDockPin),
                    LauncherApp("files", "Files", Icons.Filled.FolderOpen, null, Color(0xFF1C7FE0), onDockPin),
                    LauncherApp("sandbox", "Sandbox", Icons.Filled.Inventory2, null, Color(0xFFE2557A), onDockPin),
                    LauncherApp("models", "Models", Icons.Filled.SmartToy, null, Color(0xFF8A6CFF), onDockPin),
                    LauncherApp("settings", "Settings", Icons.Filled.Settings, null, Color(0xFF6B7077), onDockPin),
                    LauncherApp("internet", "Internet", Icons.Filled.Language, null, Color(0xFF1769AA), onDockPin),
                )
            }
            val byId = remember(catalog) { catalog.associateBy { it.id } }
            val dockPins = remember { settings.getLauncherDockPins(defaultDockPins).filter { byId.containsKey(it) } }

            // The whole window (bar + the bottom gesture-pill strip) carries the theme.
            // An opaque base sits under the theme brush so a translucent "glass" theme
            // stays readable over other apps (there's no wallpaper to frost here).
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0E1117))
                    .background(theme.surfaceBrush()),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .align(Alignment.TopCenter)
                        .drawBehind {
                            drawLine(
                                color = Color.White.copy(alpha = 0.5f),
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                strokeWidth = 1f,
                            )
                        }
                        .padding(horizontal = 8.dp),
                ) {
                    Row(
                        modifier = Modifier.align(Alignment.CenterStart),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        dockPins.mapNotNull { byId[it] }.forEach {
                            DockIcon(it.icon, it.image, it.label, it.color, it.onOpen)
                        }
                    }

                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        StartOrb(style = orbStyle, imagePath = orbImage, onClick = onOrb)
                        GlassDockIcon(Res.drawable.ic_glass_phone, "Phone", onPhone)
                        GlassDockIcon(Res.drawable.ic_glass_messages, "Messages", onMessages)
                    }

                    DesktopClock(
                        onClick = onClock,
                        content = theme.content,
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp),
                    )
                }
            }
        }
    }
}
