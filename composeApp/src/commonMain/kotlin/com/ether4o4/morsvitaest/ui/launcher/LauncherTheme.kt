package com.ether4o4.morsvitaest.ui.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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

// ─── Shared glossy-glass surface ────────────────────────────────────────────
// One source of truth for the "clean shiny glass" look. Performant by design:
// translucency + gradients, no live blur — so applying it broadly doesn't jank.

/** Specular top-lip highlight — a bright sheen concentrated at the very top edge
 *  (like light catching the curved lip of real glass), falling off fast. */
internal val glassGloss: Brush = Brush.verticalGradient(
    0.0f to Color.White.copy(alpha = 0.30f),
    0.10f to Color.White.copy(alpha = 0.10f),
    0.28f to Color.White.copy(alpha = 0.02f),
    1.0f to Color.Transparent,
)

/** Bright hairline edge for glass panels — bright at the top, faint toward the bottom. */
internal val glassHairline: BorderStroke = BorderStroke(
    1.dp,
    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.08f))),
)

/**
 * Opaque base under [glassPanel]. Keeping the panel opaque (not see-through) means the
 * GPU doesn't blend the whole wallpaper through it — critical for performance: full
 * translucency over the wallpaper multiplies overdraw and stutters weaker devices.
 * The gloss + hairline still read as glossy glass over this solid base.
 */
internal val glassBase: Color = Color(0xFF11151B)

/**
 * The glossy glass-panel fill used across the app: an opaque base, the themed [surface]
 * sweep, and a bright specular top lip, clipped to [shape]. Pair with
 * `.border(glassHairline, shape)` for the bright edge. Deliberately NOT translucent and
 * NO live blur — overdraw is the enemy on mobile, so this stays cheap to draw everywhere.
 */
internal fun Modifier.glassPanel(surface: Brush, shape: Shape): Modifier = this
    .clip(shape)
    .background(glassBase, shape)
    .background(surface, shape)
    .background(glassGloss, shape)

/**
 * The themed surface fill shared by the taskbar, Start menu and widgets window.
 * "Glass" stays a clean translucent-white pane; every colored theme becomes a
 * color + glass gradient — a translucent white sheen easing into the theme color
 * so the wallpaper still shows through.
 */
internal fun LauncherTheme.surfaceBrush(): Brush = if (glass) {
    Brush.verticalGradient(
        listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.10f)),
    )
} else {
    Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.20f),
            panel.copy(alpha = 0.82f),
            panel.copy(alpha = 0.92f),
        ),
    )
}
