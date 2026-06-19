package com.ether4o4.morsvitaest.ui.overlay

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.ether4o4.morsvitaest.InstalledApp
import com.ether4o4.morsvitaest.data.AppSettings
import com.ether4o4.morsvitaest.getInstalledApps
import com.ether4o4.morsvitaest.launchApp
import com.ether4o4.morsvitaest.ui.DarkColorScheme
import com.ether4o4.morsvitaest.ui.Theme
import com.ether4o4.morsvitaest.ui.launcher.LauncherApp
import com.ether4o4.morsvitaest.ui.launcher.StartDrawer
import com.ether4o4.morsvitaest.ui.launcher.defaultDockPins
import com.ether4o4.morsvitaest.ui.launcher.defaultStartPins
import morsvitaest.composeapp.generated.resources.Res
import morsvitaest.composeapp.generated.resources.ns_mascot_face
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

/**
 * The Start menu shown as a floating system overlay (over other apps), toggled by the
 * taskbar's Start orb. It hosts the **exact same** [StartDrawer] the in-app launcher
 * uses, so it looks and launches identically — just drawn in the overlay layer so it
 * sits on top of other apps (the in-app launcher's window can't).
 *
 * [StartDrawer] is hosted with `inOverlay = true`: a Compose pop-up dialog can't be
 * shown from a Service overlay, so the long-press pin/move dialogs are suppressed here;
 * MVE's own apps and any editing route to the full launcher via [onOpenMve].
 *
 * @param onClose hide this overlay.
 * @param onOpenMve bring the MorsVitaEst launcher to the front.
 */
@Composable
fun OverlayStartMenu(onClose: () -> Unit, onOpenMve: () -> Unit) {
    KoinContext {
        Theme(colorScheme = DarkColorScheme) {
            val settings = koinInject<AppSettings>()

            var installed by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
            LaunchedEffect(Unit) { installed = getInstalledApps() }

            // The built-in NeverSoft apps. Over other apps there are no MVE windows to
            // open into, so each just brings the launcher forward.
            val catalog = remember {
                listOf(
                    LauncherApp("assistant", "Assistant", null, Res.drawable.ns_mascot_face, Color(0xFF050507), onOpenMve),
                    LauncherApp("terminal", "Terminal", Icons.Filled.Terminal, null, Color(0xFF2B2D31), onOpenMve),
                    LauncherApp("files", "Files", Icons.Filled.FolderOpen, null, Color(0xFF1C7FE0), onOpenMve),
                    LauncherApp("sandbox", "Sandbox", Icons.Filled.Inventory2, null, Color(0xFFE2557A), onOpenMve),
                    LauncherApp("models", "Models", Icons.Filled.SmartToy, null, Color(0xFF8A6CFF), onOpenMve),
                    LauncherApp("settings", "Settings", Icons.Filled.Settings, null, Color(0xFF6B7077), onOpenMve),
                    LauncherApp("internet", "Internet", Icons.Filled.Language, null, Color(0xFF1769AA), onOpenMve),
                )
            }
            val byId = remember(catalog) { catalog.associateBy { it.id } }

            var startPins by remember {
                mutableStateOf(settings.getLauncherStartPins(defaultStartPins).filter { byId.containsKey(it) })
            }
            var dockPins by remember {
                mutableStateOf(settings.getLauncherDockPins(defaultDockPins).filter { byId.containsKey(it) })
            }

            StartDrawer(
                apps = catalog,
                installedApps = installed,
                startPins = startPins,
                dockPins = dockPins,
                onToggleStartPin = { id ->
                    startPins = if (startPins.contains(id)) startPins - id else startPins + id
                    settings.setLauncherStartPins(startPins)
                },
                onToggleDockPin = { id ->
                    dockPins = if (dockPins.contains(id)) dockPins - id else dockPins + id
                    settings.setLauncherDockPins(dockPins)
                },
                onLaunchPackage = { launchApp(it) },
                onClose = onClose,
                onOpenGuide = onOpenMve,
                showGuideButton = false,
                onOpenLauncherCustomize = onOpenMve,
                onOpenAgentSettings = onOpenMve,
                onLaunchChat = onOpenMve,
                onLaunchShell = onOpenMve,
                allowDesktopShortcut = false,
                inOverlay = true,
            )
        }
    }
}
