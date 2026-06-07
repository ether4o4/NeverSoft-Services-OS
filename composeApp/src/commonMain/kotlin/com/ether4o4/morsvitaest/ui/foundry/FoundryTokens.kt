package com.ether4o4.morsvitaest.ui.foundry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Foundry — brushed-metal finish tokens for the MorsVitaEst home redesign.
 *
 * The look has three building blocks every component reuses:
 *   - a fill brush (the brushed steel sweep — horizontal on pills/text fields,
 *     radial on square cards)
 *   - a bevel border (bright top edge fading to dark bottom edge — this is
 *     what gives the depth; the fill alone reads as flat gray)
 *   - a corner shape (pills = fully rounded, cards = soft radius)
 *
 * Plus four glossy "intent" brushes for action pills (Primary / Secondary /
 * Critical / Neutral) and a soft inner-highlight overlay to apply across the
 * top half of buttons for the gloss lip.
 */
object Foundry {

    // ---- Steel palette (dark-first; light-mode tints later) ------------

    private val steelHighlight = Color(0xFFB8B8B8)
    private val steelLight = Color(0xFF8A8A8A)
    private val steelMid = Color(0xFF555555)
    private val steelDark = Color(0xFF2B2B2B)
    private val steelShadow = Color(0xFF161616)

    // ---- Fill brushes -------------------------------------------------

    /** Horizontal brushed-steel sweep for pills, text fields, search bars. */
    val brushedHorizontal: Brush = Brush.horizontalGradient(
        colors = listOf(
            steelDark,
            steelMid,
            steelLight,
            steelMid,
            steelDark,
            steelMid,
            steelShadow,
        ),
    )

    /** Radial brushed-steel polish for square cards (concentric finish). */
    val brushedRadial: Brush = Brush.radialGradient(
        colors = listOf(
            steelLight,
            steelMid,
            steelDark,
            steelShadow,
        ),
        center = Offset.Unspecified,
        radius = Float.POSITIVE_INFINITY,
    )

    // ---- Bevel borders -------------------------------------------------

    /** Bright top → dark bottom — gives the "raised" metal-plate feel. */
    val bevelBrush: Brush = Brush.verticalGradient(
        colors = listOf(
            steelHighlight,
            steelLight,
            steelMid,
            steelDark,
            steelShadow,
        ),
    )

    val bevel: BorderStroke = BorderStroke(width = 1.5.dp, brush = bevelBrush)
    val bevelThick: BorderStroke = BorderStroke(width = 2.dp, brush = bevelBrush)

    // ---- Intent brushes (action pills) --------------------------------

    /** Primary action — red glossy. "Activate Core" in the mockup. */
    val intentPrimary: Brush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFF6B6B),
            Color(0xFFE53935),
            Color(0xFFB71C1C),
            Color(0xFF7F1010),
        ),
    )

    /** Secondary action — blue glossy. "Analyze Projections" in the mockup. */
    val intentSecondary: Brush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF64B5F6),
            Color(0xFF1E88E5),
            Color(0xFF1565C0),
            Color(0xFF0D3F7F),
        ),
    )

    /** Critical / upgrade action — purple gradient. "Initialize Upgrade". */
    val intentCritical: Brush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFCE93D8),
            Color(0xFF8E24AA),
            Color(0xFF5E35B1),
            Color(0xFF311B92),
        ),
    )

    /** Neutral metal action — same as steel pill but slightly brighter. */
    val intentNeutral: Brush get() = brushedHorizontal

    // ---- Gloss overlay -------------------------------------------------

    /** Top-half highlight to layer on top of intent fills for the gloss lip. */
    val glossOverlay: Brush = Brush.verticalGradient(
        colors = listOf(
            Color(0x66FFFFFF),
            Color(0x22FFFFFF),
            Color(0x00FFFFFF),
            Color(0x00000000),
        ),
    )

    // ---- Shapes --------------------------------------------------------

    val pillShape: Shape = RoundedCornerShape(percent = 50)
    val cardShape: Shape = RoundedCornerShape(16.dp)
    val cardShapeLarge: Shape = RoundedCornerShape(20.dp)
    val tileShape: Shape = RoundedCornerShape(14.dp)

    // ---- Spacing -------------------------------------------------------

    val gridGap: Dp = 10.dp
    val pagePadding: Dp = 12.dp
    val cardPadding: Dp = 12.dp

    // ---- Text colors --------------------------------------------------

    /** Pure white for primary labels on the dark metal. */
    val labelPrimary: Color = Color(0xFFF5F5F5)
    val labelSecondary: Color = Color(0xFFB8B8B8)
    val labelMuted: Color = Color(0xFF7C7C7C)

    /** Color the brand wordmark uses (the same off-white as the title plate). */
    val wordmark: Color = Color(0xFFE8E2D5)

    /** Dark-red page backdrop, shared with the dark color scheme so the home,
     *  Compare, and the chat/settings workspace all sit on the same red. */
    val background: Color = Color(0xFF260A0A)
}

/** Convenience: which intent brush corresponds to a given semantic role. */
enum class FoundryIntent { Primary, Secondary, Critical, Neutral }

@Composable
@ReadOnlyComposable
fun FoundryIntent.brush(): Brush = when (this) {
    FoundryIntent.Primary -> Foundry.intentPrimary
    FoundryIntent.Secondary -> Foundry.intentSecondary
    FoundryIntent.Critical -> Foundry.intentCritical
    FoundryIntent.Neutral -> Foundry.intentNeutral
}
