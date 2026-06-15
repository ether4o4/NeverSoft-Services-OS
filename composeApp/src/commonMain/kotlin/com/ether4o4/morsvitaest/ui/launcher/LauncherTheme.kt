package com.ether4o4.morsvitaest.ui.launcher

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * The NeverSoft OS system accent (cyan) — used for the Start-orb glow, active
 * taskbar indicators, selection rings, and cursors across the launcher.
 */
internal val NeverSoftAccent = Color(0xFF00D4FF)

/**
 * The single NeverSoft OS frosted-glass fill shared by the Start menu and the
 * widgets/notifications window so every translucent surface reads as the same
 * material: a bright top reflection fading to a fainter base (Vista-Aero glass).
 */
internal val neverSoftGlass: Brush = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.24f), Color.White.copy(alpha = 0.12f)),
)

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

internal fun resolveLauncherTheme(id: String): LauncherTheme =
    launcherThemes.firstOrNull { it.id == id } ?: launcherThemes.first()
