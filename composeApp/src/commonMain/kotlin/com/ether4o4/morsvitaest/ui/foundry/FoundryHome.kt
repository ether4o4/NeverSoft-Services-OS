package com.ether4o4.morsvitaest.ui.foundry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * FoundryHome — brushed-metal landing screen.
 *
 * Layout (Phase 1, dark-first):
 *   ┌─────────────────────────────────────────┐
 *   │           MORSVITAEST                   │   ← title plate
 *   │         NEVERSOFT SERVICES              │
 *   ├──────────────────┬──────────────────────┤
 *   │                  │ [Activate] [Analyze] │
 *   │                  │ [Active Model ▼]     │
 *   │  News Feed       │ [GGUF Search]        │
 *   │  (placeholder    │ [Chat]               │
 *   │   — Phase 2      │ [QuickActions]       │
 *   │   widgets)       │ [Agents] [Plugins]   │
 *   │                  ├──────────────────────┤
 *   │                  │  shell|files|pkgs|*  │
 *   │                  │  (Shell tile)        │
 *   └──────────────────┴──────────────────────┘
 *
 * Phase 1 wires "Activate Core" → existing chat (via onNavigateToChat).
 * All other tiles open the placeholder screen via the navigator passed in.
 */
@Composable
fun FoundryHome(
    onNavigate: (FoundryDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
            .padding(Foundry.pagePadding),
    ) {
        TitlePlate()
        Spacer(Modifier.height(Foundry.gridGap))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Foundry.gridGap),
        ) {
            // Left half — news feed surface (placeholder for Phase 2).
            NewsFeedColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )

            // Right half — controls + shell tile.
            RightControlColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onNavigate = onNavigate,
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────
// Title plate (top wordmark)
// ────────────────────────────────────────────────────────────────────

@Composable
private fun TitlePlate() {
    // Phase 1: text-based wordmark rendered inside a brushed-metal frame.
    // Phase 2 swaps the body for the bundled PNG title-plate asset.
    FoundryCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        shape = Foundry.cardShapeLarge,
    ) {
        Text(
            text = "MORSVITAEST",
            color = Foundry.wordmark,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
            letterSpacing = 2.sp,
            modifier = Modifier
                .fillMaxWidth(),
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
// Left column — news feed (Phase 2 lands real widgets here)
// ────────────────────────────────────────────────────────────────────

private data class FeedStub(val title: String, val source: String, val summary: String)

private val phase2Stubs = listOf(
    FeedStub(
        title = "News feed coming in Phase 2",
        source = "MorsVitaEst",
        summary = "Customizable URL-embeddable feed. Add HuggingFace papers, GitHub trending, RSS, or paste any link.",
    ),
    FeedStub(
        title = "Foundry Phase 1 shipped",
        source = "Build notes",
        summary = "Brushed metal home + title plate + tile navigation. Phase 2 wires the news feed widget surface.",
    ),
    FeedStub(
        title = "Heartbeat will surface here",
        source = "Roadmap",
        summary = "Trending AI integrations, model drops, new MCP servers, and your own pinned notes.",
    ),
)

@Composable
private fun NewsFeedColumn(modifier: Modifier = Modifier) {
    FoundryCard(
        modifier = modifier,
        contentPadding = PaddingValues(8.dp),
        shape = Foundry.cardShapeLarge,
    ) {
        Text(
            text = "NEWS FEED",
            color = Foundry.labelPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(phase2Stubs) { stub ->
                FeedItem(stub)
            }
        }
    }
}

@Composable
private fun FeedItem(stub: FeedStub) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Foundry.tileShape)
            .background(Color(0xFF1A1A1A), Foundry.tileShape)
            .padding(10.dp),
    ) {
        Column {
            Text(
                text = stub.title,
                color = Foundry.labelPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stub.source,
                color = Color(0xFFE53935),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stub.summary,
                color = Foundry.labelSecondary,
                fontSize = 11.sp,
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────
// Right column — controls + shell tile
// ────────────────────────────────────────────────────────────────────

@Composable
private fun RightControlColumn(
    modifier: Modifier = Modifier,
    onNavigate: (FoundryDestination) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Foundry.gridGap),
    ) {
        // Top action pills: Activate Core / Analyze Projections.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Foundry.gridGap),
        ) {
            FoundryPill(
                label = "ACTIVATE",
                onClick = { onNavigate(FoundryDestination.Chat) },
                intent = FoundryIntent.Primary,
                modifier = Modifier.weight(1f),
            )
            FoundryPill(
                label = "ANALYZE",
                onClick = { onNavigate(FoundryDestination.Diagnostics) },
                intent = FoundryIntent.Secondary,
                modifier = Modifier.weight(1f),
            )
        }

        // Active Model dropdown placeholder.
        FoundryTextFieldPill(
            label = "Active Model",
            onClick = { onNavigate(FoundryDestination.ActiveModel) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = { Text("▼", color = Foundry.labelSecondary, fontSize = 12.sp) },
        )

        // GGUF / LLM search pill.
        FoundrySteelPill(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onNavigate(FoundryDestination.LlmSearch) },
        ) {
            Text(
                text = "🔍  GGUF / LLM Search",
                color = Foundry.labelPrimary,
                fontSize = 14.sp,
            )
        }

        // Chat button (right above the shell tile per the layout spec).
        FoundryPill(
            label = "CHAT",
            onClick = { onNavigate(FoundryDestination.Chat) },
            intent = FoundryIntent.Neutral,
            modifier = Modifier.fillMaxWidth(),
        )

        // Two small side-by-side tiles for Agents / Plugins.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Foundry.gridGap),
        ) {
            SmallTile(
                title = "AGENTS",
                subtitle = "5 modes",
                onClick = { onNavigate(FoundryDestination.Agents) },
                modifier = Modifier.weight(1f),
            )
            SmallTile(
                title = "PLUGINS",
                subtitle = "MCP",
                onClick = { onNavigate(FoundryDestination.Plugins) },
                modifier = Modifier.weight(1f),
            )
        }

        // Shell tile occupies the bottom 1/4 of the right column.
        ShellTile(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            onClick = { onNavigate(FoundryDestination.Shell) },
        )
    }
}

@Composable
private fun SmallTile(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FoundryCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = PaddingValues(10.dp),
    ) {
        FoundryCardHeader(
            title = title,
            subtitle = subtitle,
            indicatorColor = Foundry.labelSecondary,
        )
    }
}

@Composable
private fun ShellTile(modifier: Modifier = Modifier, onClick: () -> Unit) {
    FoundryCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = PaddingValues(10.dp),
    ) {
        // Tab strip: shell | files | pkgs | *
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TabLabel("shell", active = true)
            TabLabel("files")
            TabLabel("pkgs")
            TabLabel("*")
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0A0A0A), RoundedCornerShape(8.dp))
                .padding(10.dp),
        ) {
            Text(
                text = "Default project\nNetwork logs\nProject files",
                color = Foundry.labelMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 0.dp)
                    .align(Alignment.BottomStart),
            ) {
                Text(
                    text = "mve\$  enter command…",
                    color = Color(0xFFE53935),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun TabLabel(text: String, active: Boolean = false) {
    Text(
        text = text,
        color = if (active) Foundry.labelPrimary else Foundry.labelMuted,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
    )
}

/** Tile destinations the home grid can navigate to. */
enum class FoundryDestination {
    Chat,
    Diagnostics,
    ActiveModel,
    LlmSearch,
    Agents,
    Plugins,
    Shell,
    Profile,
}
