package com.ether4o4.morsvitaest.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * The NeverSoft OS system accent (cyan) — used for the Start-orb glow, active
 * taskbar indicators, selection rings, and cursors across the launcher.
 */
internal val NeverSoftAccent = Color(0xFF00D4FF)

/**
 * Dark translucent fallback for glass surfaces where there is no shared Haze
 * source to blur (e.g. the cyberdeck shell's Start drawer).
 */
internal val neverSoftGlassBrush: Brush = Brush.verticalGradient(
    listOf(Color(0xCC0E1726), Color(0xCC0A1018)),
)

/**
 * Clean "see-through" glass: a thin translucent-white pane you can see the
 * desktop through. Used by the Start menu and the widgets panel.
 */
internal fun Modifier.neverSoftGlassClear(): Modifier = background(
    Brush.verticalGradient(
        listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.10f)),
    ),
)

/**
 * Real glassmorphism. When a shared [haze] state is supplied, this surface
 * blurs the wallpaper behind it and darkens it (Vista / Windows-11 frosted
 * glass); otherwise it falls back to the dark translucent gradient. Used by the
 * floating app windows (sandbox / terminal).
 */
internal fun Modifier.neverSoftGlassBlur(haze: HazeState?): Modifier = if (haze != null) {
    hazeEffect(state = haze) {
        blurRadius = 28.dp
        tints = listOf(HazeTint(Color(0xFF0A1018).copy(alpha = 0.55f)))
        noiseFactor = 0.04f
    }
} else {
    background(neverSoftGlassBrush)
}

/**
 * A launcher theme tints the three system surfaces — taskbar, Start menu, and
 * the widgets/notifications window — the same color. "Glass" keeps the
 * translucent frosted look; the rest are flat colors with a matching readable
 * content color.
 */
internal data class LauncherTheme(
    val id: String,
    val label: String,
    val panel: Color,
    val content: Color,
    val glass: Boolean,
)

internal val launcherThemes = listOf(
    LauncherTheme("glass", "Glass", Color.White.copy(alpha = 0.16f), Color.White, glass = true),
    LauncherTheme("macbook", "MacBook", Color(0xF2ECEEF1), Color(0xFF1D1D1F), glass = false),
    LauncherTheme("red", "Red", Color(0xF26E1A22), Color.White, glass = false),
    LauncherTheme("purple", "Purple", Color(0xF22E1A47), Color.White, glass = false),
    LauncherTheme("blue", "Blue", Color(0xF2143257), Color.White, glass = false),
    LauncherTheme("green", "Green", Color(0xF2123A28), Color.White, glass = false),
    LauncherTheme("black", "Black", Color(0xF20A0B0E), Color.White, glass = false),
    LauncherTheme("darkgrey", "Dark Grey", Color(0xF21E2228), Color.White, glass = false),
    LauncherTheme("lightgrey", "Light Grey", Color(0xF2C7CCD3), Color(0xFF14171C), glass = false),
    LauncherTheme("white", "White", Color(0xF2F0F2F6), Color(0xFF14171C), glass = false),
)

internal fun resolveLauncherTheme(id: String): LauncherTheme = launcherThemes.firstOrNull { it.id == id } ?: launcherThemes.first()
