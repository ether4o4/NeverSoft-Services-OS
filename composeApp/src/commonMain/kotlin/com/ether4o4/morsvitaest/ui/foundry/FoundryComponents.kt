package com.ether4o4.morsvitaest.ui.foundry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Foundry — brushed-metal Compose primitives. Every component in this file
 * is dark-first, uses the [Foundry] token sweep + bevel for its surface,
 * and is meant to be composed directly into [com.ether4o4.morsvitaest.ui.foundry.FoundryHome].
 *
 * Not Material3 wrappers — these intentionally bypass the Material theme so
 * the metal look isn't fighting onSurface/onSurfaceVariant tinting.
 */

/**
 * Primary action pill: red/blue/purple glossy with a top gloss lip.
 *
 *  [Primary | Secondary | Critical] tap → [onClick]
 */
@Composable
fun FoundryPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    intent: FoundryIntent = FoundryIntent.Neutral,
    enabled: Boolean = true,
    minHeight: Dp = 44.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .heightIn(min = minHeight)
            .clip(Foundry.pillShape)
            .background(brush = intent.brush(), shape = Foundry.pillShape)
            .background(brush = Foundry.glossOverlay, shape = Foundry.pillShape)
            .border(Foundry.bevel, Foundry.pillShape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = Color.White),
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Neutral steel pill with a brushed sweep. Used for the search bar and the
 * "Icon button" style.
 */
@Composable
fun FoundrySteelPill(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    minHeight: Dp = 44.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val baseModifier = modifier
        .heightIn(min = minHeight)
        .clip(Foundry.pillShape)
        .background(brush = Foundry.brushedHorizontal, shape = Foundry.pillShape)
        .border(Foundry.bevel, Foundry.pillShape)
    val finalModifier = if (onClick != null) {
        baseModifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(color = Color.White),
            onClick = onClick,
        )
    } else {
        baseModifier
    }
    Box(
        modifier = finalModifier.padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart,
        content = content,
    )
}

/**
 * Square brushed-metal card with a radial polished finish. The standard tile
 * shape — used for the agent / health / hub / shell tiles in the home grid.
 *
 * Pass [onClick] to make the whole card a nav destination.
 */
@Composable
fun FoundryCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(Foundry.cardPadding),
    shape: Shape = Foundry.cardShape,
    content: @Composable ColumnScope.() -> Unit,
) {
    val base = modifier
        .clip(shape)
        .background(brush = Foundry.brushedRadial, shape = shape)
        .border(Foundry.bevel, shape)
    val withClick = if (onClick != null) {
        base.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(color = Color.White),
            onClick = onClick,
        )
    } else {
        base
    }
    Column(
        modifier = withClick.padding(contentPadding),
        content = content,
    )
}

/**
 * Header for a card — title + optional indicator dot in the top-right.
 */
@Composable
fun FoundryCardHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    indicatorColor: Color? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.padding(end = 8.dp)) {
            Text(
                text = title,
                color = Foundry.labelPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = Foundry.labelSecondary,
                    fontSize = 11.sp,
                )
            }
        }
        if (indicatorColor != null) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(Foundry.pillShape)
                    .border(BorderStroke(width = 1.5.dp, color = indicatorColor), Foundry.pillShape),
            )
        }
    }
}

/**
 * Brushed text-field pill. Read-only display surface (matches the mockup's
 * static "Text Field" pills with dropdown / sparkle icons); tap surfaces a
 * real editor on the destination page.
 */
@Composable
fun FoundryTextFieldPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    FoundrySteelPill(modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                color = Foundry.labelPrimary,
                fontSize = 14.sp,
                modifier = Modifier.padding(end = 8.dp),
            )
            if (trailingIcon != null) trailingIcon()
        }
    }
}

/**
 * Small metallic icon button (the "Switch" trio's star / plus / equals buttons
 * in the mockup). Pass a single character/glyph as [glyph] — the brushed
 * surface does the work.
 */
@Composable
fun FoundryIconChip(
    glyph: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tint: Color = Foundry.labelPrimary,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(Foundry.tileShape)
            .background(brush = Foundry.brushedRadial, shape = Foundry.tileShape)
            .border(Foundry.bevel, Foundry.tileShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.White),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = glyph, color = tint, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}
