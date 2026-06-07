@file:OptIn(ExperimentalMaterial3Api::class)

package com.ether4o4.morsvitaest.ui.foundry

import androidx.compose.foundation.Image
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import morsvitaest.composeapp.generated.resources.Res
import morsvitaest.composeapp.generated.resources.title_plate
import org.jetbrains.compose.resources.painterResource

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
    isRefreshing: Boolean = false,
    feedItems: List<FoundryFeedItem> = previewFeedItems,
    navigationTabBar: (@Composable () -> Unit)? = null,
) {
    // Which integration box's glass config sheet is open (null = none).
    var sheetBox by remember { mutableStateOf<IntegrationBox?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Foundry.background)
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
            isRefreshing = isRefreshing,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )

        Spacer(Modifier.height(Foundry.gridGap))

        // Integration boxes — body taps deep-link; each ⚙ opens a glass config sheet.
        IntegrationBoxes(onNavigate = onNavigate, onConfig = { sheetBox = it })
    }

    sheetBox?.let { box ->
        IntegrationBoxSheet(
            box = box,
            onOpen = onNavigate,
            onDismiss = { sheetBox = null },
        )
    }
}

// ────────────────────────────────────────────────────────────────────
// Title plate
// ────────────────────────────────────────────────────────────────────

@Composable
private fun TitlePlate() {
    // The brand logo already contains the wordmark + "Neversoft Services" + the
    // metal frame, with its black background knocked out so it sits on the red.
    Image(
        painter = painterResource(Res.drawable.title_plate),
        contentDescription = "MorsVitaEst — Neversoft Services",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp),
    )
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
    isRefreshing: Boolean,
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
            // Tap-to-refresh (works everywhere); the list also supports pull-to-refresh.
            FoundryIconChip(
                glyph = "↻",
                onClick = onRefresh,
                size = 34.dp,
                contentDescription = "Refresh feed",
            )
        }
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (items.isEmpty()) {
                    item { EmptyFeedRow() }
                } else {
                    items(items) { item -> FeedRow(item) }
                }
            }
        }
    }
}

@Composable
private fun EmptyFeedRow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Foundry.tileShape)
            .background(Color(0xFF1A1A1A), Foundry.tileShape)
            .padding(12.dp),
    ) {
        Text(
            text = "No updates yet — pull down to refresh, or turn on Heartbeat in Settings.",
            color = Foundry.labelSecondary,
            fontSize = 12.sp,
        )
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
        // Thumbnail frame. The source initial sits behind as the fallback; a real
        // image (when the feed item carries one) loads on top and covers it. If the
        // image is missing or fails to load, the initial shows through.
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
            if (item.thumbnailUrl != null) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                )
            }
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
    val blurb: String,
    val tips: List<String>,
    val actionLabel: String,
)

private val integrationBoxes = listOf(
    IntegrationBox(
        title = "SERVICES",
        subtitle = "API keys",
        open = FoundryDestination.Services,
        config = FoundryDestination.Services,
        blurb = "Bring your own AI accounts. You're already running on the Free AI — add a provider " +
            "here only if you want your own (often faster or smarter).",
        tips = listOf("Pick a provider", "Paste its API key", "It's ready everywhere"),
        actionLabel = "OPEN SERVICES",
    ),
    IntegrationBox(
        title = "MCP",
        subtitle = "Tool servers",
        open = FoundryDestination.Mcp,
        config = FoundryDestination.Mcp,
        blurb = "Plug-ins that give your AI new abilities — web search, your files, a calendar, and more.",
        tips = listOf("Add a server URL", "Pick a popular one", "Its tools show up for the AI"),
        actionLabel = "OPEN MCP",
    ),
    IntegrationBox(
        title = "HUGGING FACE",
        subtitle = "Models · papers",
        open = FoundryDestination.HuggingFace,
        config = FoundryDestination.HuggingFace,
        blurb = "Pull on-device models from the Hugging Face hub to run locally, no account needed.",
        tips = listOf("Browse on-device models", "Download one", "Run it fully offline"),
        actionLabel = "BROWSE MODELS",
    ),
    IntegrationBox(
        title = "OLLAMA",
        subtitle = "Local runtime",
        open = FoundryDestination.Ollama,
        config = FoundryDestination.Ollama,
        blurb = "Already run Ollama on your machine or network? Point MorsVitaEst at it as a service.",
        tips = listOf("Add Ollama as a service", "Set its base URL", "Pick a pulled model"),
        actionLabel = "CONNECT OLLAMA",
    ),
    IntegrationBox(
        title = "LLM CHOOSER",
        subtitle = "Pick your models",
        open = FoundryDestination.LlmChooser,
        config = FoundryDestination.LlmChooser,
        blurb = "See everything you've connected in one place and choose which models you actually use.",
        tips = listOf("Review your services", "Enable the ones you want", "Reorder your fallback chain"),
        actionLabel = "MANAGE MODELS",
    ),
)

@Composable
private fun IntegrationBoxes(
    onNavigate: (FoundryDestination) -> Unit,
    onConfig: (IntegrationBox) -> Unit,
) {
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
                        onConfig = onConfig,
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
    onConfig: (IntegrationBox) -> Unit,
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
            // Per-box ⚙ — opens this box's own glass config sheet.
            FoundryIconChip(
                glyph = "⚙",
                onClick = { onConfig(box) },
                size = 34.dp,
                contentDescription = "Configure ${box.title}",
            )
        }
    }
}

/**
 * Per-box config sheet — the "fancy glass" panel a box's ⚙ opens. It explains the
 * box in plain language with a tiny numbered recipe, then drops the user into the
 * matching settings only when they choose to. Keeps the gear from dead-ending
 * straight onto a dense settings screen.
 */
@Composable
private fun IntegrationBoxSheet(
    box: IntegrationBox,
    onOpen: (FoundryDestination) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF161616),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = box.title,
                color = Foundry.labelPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = box.blurb,
                color = Foundry.labelSecondary,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(14.dp))
            box.tips.forEachIndexed { index, tip ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(Foundry.pillShape)
                            .background(brush = Foundry.brushedRadial, shape = Foundry.pillShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = Foundry.labelPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(text = tip, color = Foundry.labelPrimary, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(18.dp))
            FoundryPill(
                label = box.actionLabel,
                onClick = {
                    onDismiss()
                    onOpen(box.open)
                },
                intent = FoundryIntent.Primary,
                modifier = Modifier.fillMaxWidth(),
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
