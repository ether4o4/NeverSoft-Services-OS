package com.ether4o4.morsvitaest.ui.foundry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * FoundryHome — the brushed-metal landing screen (Page 1).
 *
 * Vertical, mobile-first layout (replaces the earlier side-by-side draft that
 * didn't fit small phones):
 *
 *   ┌─────────────────────────────────────────┐
 *   │            MORSVITAEST  (title plate)    │
 *   ├─────────────────────────────────────────┤
 *   │  NEWS FEED                          ↻    │  ← top, full width
 *   │  ┌───┐ headline …                        │
 *   │  │IMG│ source · summary                  │     thumbnail rows
 *   │  └───┘                                   │
 *   ├─────────────────────────────────────────┤
 *   │  SERVICES   MCP        each box has its   │
 *   │  HUGGING…   OLLAMA     own ⚙ that opens   │  ← integration boxes
 *   │  LLM CHOOSER          its config sheet    │
 *   └─────────────────────────────────────────┘
 *
 * Page 2 (the Chat / Compare / Shell workspace) is a separate screen. Each tile
 * here drives [onNavigate]; the gear on a box drives the same callback with the
 * box's *config* destination so the per-box ⚙ pattern stays consistent.
 */
@Composable
fun FoundryHome(
    onNavigate: (FoundryDestination) -> Unit,
    modifier: Modifier = Modifier,
    onRefreshFeed: () -> Unit = {},
    feedItems: List<FoundryFeedItem> = previewFeedItems,
    navigationTabBar: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(Foundry.pagePadding),
    ) {
        if (navigationTabBar != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = Foundry.gridGap),
                horizontalArrangement = Arrangement.Center,
            ) {
                navigationTabBar()
            }
        }
        TitlePlate()
        Spacer(Modifier.height(Foundry.gridGap))

        FoundryPill(
            label = "⚖  COMPARE YOUR FAVORITE LLMS",
            onClick = { onNavigate(FoundryDestination.Compare) },
            intent = FoundryIntent.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Foundry.gridGap))

        // News feed sits at the top and takes the remaining vertical space.
        NewsFeedCard(
            items = feedItems,
            onRefresh = onRefreshFeed,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )

        Spacer(Modifier.height(Foundry.gridGap))

        // Integration boxes — each opens its own config via the ⚙.
        IntegrationBoxes(onNavigate = onNavigate)
    }
}

// ────────────────────────────────────────────────────────────────────
// Title plate
// ────────────────────────────────────────────────────────────────────

@Composable
private fun TitlePlate() {
    FoundryCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
        shape = Foundry.cardShapeLarge,
    ) {
        Text(
            text = "MORSVITAEST",
            color = Foundry.wordmark,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            letterSpacing = 2.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "N E V E R S O F T   S E R V I C E S",
            color = Foundry.labelSecondary,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            letterSpacing = 3.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

// ────────────────────────────────────────────────────────────────────
// News feed (top)
// ────────────────────────────────────────────────────────────────────

/** A single feed row. [thumbnailUrl] is reserved for live wiring; the layout
 *  renders a thumbnail frame either way so rows stay aligned. */
data class FoundryFeedItem(
    val title: String,
    val source: String,
    val summary: String,
    val thumbnailUrl: String? = null,
)

@Composable
private fun NewsFeedCard(
    items: List<FoundryFeedItem>,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FoundryCard(
        modifier = modifier,
        contentPadding = PaddingValues(8.dp),
        shape = Foundry.cardShapeLarge,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "NEWS FEED",
                color = Foundry.labelPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.5.sp,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp, vertical = 6.dp),
            )
            // Tap-to-refresh. Gesture pull-to-refresh lands when the feed is
            // wired to live Heartbeat data.
            FoundryIconChip(
                glyph = "↻",
                onClick = onRefresh,
                size = 34.dp,
            )
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items) { item -> FeedRow(item) }
        }
    }
}

@Composable
private fun FeedRow(item: FoundryFeedItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Foundry.tileShape)
            .background(Color(0xFF1A1A1A), Foundry.tileShape)
            .padding(10.dp),
    ) {
        // Thumbnail frame — placeholder fill until live images are wired.
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF262626), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = item.source.take(1).uppercase(),
                color = Foundry.labelMuted,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = Foundry.labelPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.source,
                color = Color(0xFFE53935),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.summary,
                color = Foundry.labelSecondary,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────
// Integration boxes (bottom) — each with its own ⚙
// ────────────────────────────────────────────────────────────────────

private data class IntegrationBox(
    val title: String,
    val subtitle: String,
    val open: FoundryDestination,
    val config: FoundryDestination,
)

private val integrationBoxes = listOf(
    IntegrationBox("SERVICES", "API keys", FoundryDestination.Services, FoundryDestination.Services),
    IntegrationBox("MCP", "Tool servers", FoundryDestination.Mcp, FoundryDestination.Mcp),
    IntegrationBox("HUGGING FACE", "Models · papers", FoundryDestination.HuggingFace, FoundryDestination.HuggingFace),
    IntegrationBox("OLLAMA", "Local runtime", FoundryDestination.Ollama, FoundryDestination.Ollama),
    IntegrationBox("LLM CHOOSER", "Pick your models", FoundryDestination.LlmChooser, FoundryDestination.LlmChooser),
)

@Composable
private fun IntegrationBoxes(onNavigate: (FoundryDestination) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Foundry.gridGap)) {
        // Two-per-row grid; the final odd box spans the full width.
        integrationBoxes.chunked(2).forEach { rowBoxes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Foundry.gridGap),
            ) {
                rowBoxes.forEach { box ->
                    IntegrationTile(
                        box = box,
                        onNavigate = onNavigate,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowBoxes.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun IntegrationTile(
    box: IntegrationBox,
    onNavigate: (FoundryDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    FoundryCard(
        modifier = modifier,
        onClick = { onNavigate(box.open) },
        contentPadding = PaddingValues(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = box.title,
                    color = Foundry.labelPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = box.subtitle,
                    color = Foundry.labelSecondary,
                    fontSize = 11.sp,
                )
            }
            // Per-box ⚙ — opens this box's own config.
            FoundryIconChip(
                glyph = "⚙",
                onClick = { onNavigate(box.config) },
                size = 34.dp,
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────
// Destinations + preview data
// ────────────────────────────────────────────────────────────────────

/** Destinations the home grid can drive. The host (App.kt) maps these to routes. */
enum class FoundryDestination {
    Chat,
    Compare,
    Shell,
    Projects,
    Settings,
    Services,
    Mcp,
    HuggingFace,
    Ollama,
    LlmChooser,
    FeedConfig,
}

private val previewFeedItems = listOf(
    FoundryFeedItem(
        title = "Add your own sources",
        source = "MorsVitaEst",
        summary = "Paste HuggingFace papers, GitHub trending, RSS, or any link. Pull down to refresh.",
    ),
    FoundryFeedItem(
        title = "Heartbeat surfaces here",
        source = "Roadmap",
        summary = "Trending integrations, model drops, new MCP servers, and your pinned notes.",
    ),
    FoundryFeedItem(
        title = "On-device models, one tap",
        source = "Local",
        summary = "Included 1B for instant replies; pull a 3B tool-caller as a DLC when you want more.",
    ),
)
