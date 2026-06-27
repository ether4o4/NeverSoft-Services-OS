@file:OptIn(ExperimentalMaterial3Api::class)

package com.ether4o4.morsvitaest.ui.foundry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ether4o4.morsvitaest.data.AppSettings
import com.ether4o4.morsvitaest.data.DataRepository
import com.ether4o4.morsvitaest.data.HeartbeatRunState
import com.ether4o4.morsvitaest.data.HeartbeatStatus
import com.ether4o4.morsvitaest.ui.launcher.resolveLauncherTheme
import com.ether4o4.morsvitaest.ui.launcher.surfaceBrush
import org.koin.compose.koinInject

/**
 * FoundryHome — the MorsVitaEst landing screen (Page 1).
 *
 * The page and its two boxes are tinted with the **launcher theme** (the same
 * color that paints the taskbar, Start menu, keyboard, and widgets), so the home
 * matches the rest of the system and switches live when the theme changes.
 *
 *   ┌─────────────────────────────────────────┐
 *   │            MORS VITA EST  (wordmark)     │
 *   │     ⚖  COMPARE YOUR FAVORITE LLMS        │
 *   ├─────────────────────────────────────────┤
 *   │  NEWS                          ＋   ↻    │  ← top box (50%)
 *   │  ┌──────┐ headline …                     │
 *   │  │ IMG  │ source · summary               │     real article thumbnails
 *   │  └──────┘                                │     tap a row → open article
 *   ├─────────────────────────────────────────┤
 *   │  HEARTBEAT                          ↻    │  ← bottom box (50%)
 *   │  ┌───┐ what's-new update …               │     the assistant's updates
 *   │  └───┘                                   │
 *   └─────────────────────────────────────────┘
 *
 * The provider/MCP integration boxes were moved out of here — they live in MVE
 * Settings (reachable from the settings gear) so this page stays the news/home.
 */
@Composable
fun FoundryHome(
    onNavigate: (FoundryDestination) -> Unit,
    modifier: Modifier = Modifier,
    onRefreshFeed: () -> Unit = {},
    isRefreshing: Boolean = false,
    feedItems: List<FoundryFeedItem> = previewFeedItems,
    newsItems: List<FoundryFeedItem> = previewNewsItems,
    isNewsRefreshing: Boolean = false,
    onRefreshNews: () -> Unit = {},
    newsSources: List<String> = emptyList(),
    onAddNewsSource: (String) -> Unit = {},
    onRemoveNewsSource: (String) -> Unit = {},
    navigationTabBar: (@Composable () -> Unit)? = null,
) {
    // Resolve the live launcher theme so the home matches the taskbar / Start menu
    // / keyboard and re-tints the instant the theme changes anywhere in the OS.
    val settings = koinInject<AppSettings>()
    // Recomputed each composition (cheap settings + clock read), so returning to the home
    // after toggling scheduling / budget shows the current heartbeat status.
    val heartbeatStatus = koinInject<DataRepository>().getHeartbeatStatus()
    val appearance by settings.launcherAppearanceFlow.collectAsStateWithLifecycle()
    val theme = remember(appearance) { resolveLauncherTheme(settings.getLauncherTheme()) }
    val surface = theme.surfaceBrush()
    val content = theme.content

    var showSources by remember { mutableStateOf(false) }

    // No full-screen background panel: the News box, Heartbeat box, and the pill
    // float on their own (each carries the theme), with nothing boxed behind them.
    Column(
        modifier = modifier
            .fillMaxSize()
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

        // News box (top) — real stories with article thumbnails; manual refresh.
        NewsBox(
            items = newsItems,
            isRefreshing = isNewsRefreshing,
            onRefresh = onRefreshNews,
            onManageSources = { showSources = true },
            surface = surface,
            content = content,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )

        Spacer(Modifier.height(Foundry.gridGap))

        // Heartbeat box (bottom) — the assistant's "what's new" updates.
        HeartbeatBox(
            items = feedItems,
            status = heartbeatStatus,
            onRefresh = onRefreshFeed,
            isRefreshing = isRefreshing,
            surface = surface,
            content = content,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
    }

    if (showSources) {
        NewsSourcesSheet(
            sources = newsSources,
            onAdd = onAddNewsSource,
            onRemove = onRemoveNewsSource,
            onDismiss = { showSources = false },
        )
    }
}

// ────────────────────────────────────────────────────────────────────
// Feed model + themed boxes
// ────────────────────────────────────────────────────────────────────

/** A single feed row. [thumbnailUrl] is the story's own image when present;
 *  the layout renders a thumbnail frame either way so rows stay aligned. [link],
 *  when set, makes the row open the article in the browser. */
data class FoundryFeedItem(
    val title: String,
    val source: String,
    val summary: String,
    val thumbnailUrl: String? = null,
    val link: String? = null,
    // Full report text for heartbeat entries — shown when the card is expanded so a
    // multi-bullet digest is readable in full instead of crushed into the summary line.
    val body: String? = null,
)

/** A themed surface box that carries the launcher theme (opaque base + theme
 *  brush + hairline border), used by both the News and Heartbeat boxes. */
@Composable
private fun ThemedBox(
    surface: Brush,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(Foundry.cardShapeLarge)
            .background(Color(0xFF11151B), Foundry.cardShapeLarge)
            .background(surface, Foundry.cardShapeLarge)
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), Foundry.cardShapeLarge)
            .padding(10.dp),
        content = content,
    )
}

@Composable
private fun BoxHeader(
    label: String,
    content: Color,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = content,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.5.sp,
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
        )
        trailing()
    }
}

// ────────────────────────────────────────────────────────────────────
// News box (top)
// ────────────────────────────────────────────────────────────────────

@Composable
private fun NewsBox(
    items: List<FoundryFeedItem>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onManageSources: () -> Unit,
    surface: Brush,
    content: Color,
    modifier: Modifier = Modifier,
) {
    ThemedBox(surface = surface, modifier = modifier) {
        BoxHeader(label = "NEWS", content = content) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = content,
                )
                Spacer(Modifier.width(8.dp))
            }
            // Manage the RSS/Atom sources the box pulls from.
            FoundryIconChip(glyph = "＋", onClick = onManageSources, size = 34.dp, contentDescription = "Add news source")
            Spacer(Modifier.width(6.dp))
            // Manual refresh — the news box does not auto-poll.
            FoundryIconChip(glyph = "↻", onClick = onRefresh, size = 34.dp, contentDescription = "Refresh news")
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (items.isEmpty()) {
                item {
                    EmptyRow(
                        text = "No stories yet — tap ↻ to load, or ＋ to add an RSS source.",
                        content = content,
                    )
                }
            } else {
                items(items) { item -> FeedRow(item = item, content = content, thumbWidth = 78.dp, thumbHeight = 58.dp) }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────
// Heartbeat box (bottom)
// ────────────────────────────────────────────────────────────────────

@Composable
private fun HeartbeatBox(
    items: List<FoundryFeedItem>,
    status: HeartbeatStatus,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
    surface: Brush,
    content: Color,
    modifier: Modifier = Modifier,
) {
    ThemedBox(surface = surface, modifier = modifier) {
        BoxHeader(label = "HEARTBEAT", content = content) {
            FoundryIconChip(glyph = "↻", onClick = onRefresh, size = 34.dp, contentDescription = "Refresh heartbeat")
        }
        // Surface a pause/asleep reason at the top so a quiet heartbeat is never a mystery.
        val statusMessage = heartbeatStatusMessage(status)
        if (statusMessage != null) {
            HeartbeatStatusBanner(
                text = statusMessage,
                alert = status.state != HeartbeatRunState.ASLEEP,
                content = content,
            )
            Spacer(Modifier.height(8.dp))
        }
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (items.isEmpty()) {
                    item {
                        EmptyRow(
                            text = if (statusMessage != null) {
                                "No updates yet."
                            } else {
                                "No updates yet — pull down to refresh, or turn on Heartbeat in Settings."
                            },
                            content = content,
                        )
                    }
                } else {
                    items(items) { item -> HeartbeatReportRow(item = item, content = content) }
                }
            }
        }
    }
}

/** Plain-language reason the heartbeat isn't running, with the fix. Null when it's
 *  running normally (ACTIVE) and nothing needs to be said. */
private fun heartbeatStatusMessage(status: HeartbeatStatus): String? = when (status.state) {
    HeartbeatRunState.ACTIVE -> null
    HeartbeatRunState.SCHEDULING_OFF ->
        "Paused — background scheduling is off. Turn it on in Settings → Agent."
    HeartbeatRunState.HEARTBEAT_OFF ->
        "Heartbeat is off. Enable it in Settings → Agent."
    HeartbeatRunState.BUDGET_PAUSED ->
        "Paused — daily token budget reached. Resumes after midnight, or raise it in Settings → Budget."
    HeartbeatRunState.KILL_SWITCH ->
        "Paused — autonomous activity is switched off. Re-enable it in Settings → Budget."
    HeartbeatRunState.ASLEEP ->
        "Sleeping until ${status.activeHoursStart}:00 — outside active hours."
}

/** Status pill at the top of the heartbeat box. [alert] tints it amber for real pauses;
 *  the sleeping (outside-hours) case stays neutral since it resumes on its own. */
@Composable
private fun HeartbeatStatusBanner(text: String, alert: Boolean, content: Color) {
    val tint = if (alert) Color(0xFFFFB74D) else content.copy(alpha = 0.7f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Foundry.tileShape)
            .background(tint.copy(alpha = 0.16f), Foundry.tileShape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (alert) "⏸" else "🌙",
            fontSize = 12.sp,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            color = content.copy(alpha = 0.9f),
            fontSize = 11.sp,
        )
    }
}

// ────────────────────────────────────────────────────────────────────
// Shared rows
// ────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyRow(text: String, content: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Foundry.tileShape)
            .background(Color.Black.copy(alpha = 0.20f), Foundry.tileShape)
            .padding(12.dp),
    ) {
        Text(text = text, color = content.copy(alpha = 0.7f), fontSize = 12.sp)
    }
}

@Composable
private fun FeedRow(
    item: FoundryFeedItem,
    content: Color,
    thumbWidth: Dp,
    thumbHeight: Dp,
) {
    val uriHandler = LocalUriHandler.current
    val rowModifier = Modifier
        .fillMaxWidth()
        .clip(Foundry.tileShape)
        .background(Color.Black.copy(alpha = 0.20f), Foundry.tileShape)
        .let { base ->
            val link = item.link
            if (link != null) base.clickable { runCatching { uriHandler.openUri(link) } } else base
        }
        .padding(10.dp)
    Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
        // Thumbnail. The source initial sits behind as a fallback; a real article
        // image (when present) loads on top and covers it.
        Box(
            modifier = Modifier
                .size(width = thumbWidth, height = thumbHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF262626), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = item.source.take(1).uppercase(),
                color = Color.White.copy(alpha = 0.55f),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            )
            if (item.thumbnailUrl != null) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = content,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.source,
                color = content.copy(alpha = 0.6f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.summary,
                color = content.copy(alpha = 0.75f),
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * A heartbeat entry rendered as a tappable report card. Collapsed it shows the headline
 * plus a one-line preview; tapping expands it to the full digest with any source URLs
 * turned into tappable links — so a multi-bullet research report is readable in place
 * instead of being crushed into the summary line.
 */
@Composable
private fun HeartbeatReportRow(item: FoundryFeedItem, content: Color) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Foundry.tileShape)
            .background(Color.Black.copy(alpha = 0.20f), Foundry.tileShape)
            .clickable { expanded = !expanded }
            .padding(10.dp),
    ) {
        Text(
            text = item.title,
            color = content,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = if (expanded) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "HEARTBEAT",
            color = content.copy(alpha = 0.6f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(4.dp))
        if (expanded) {
            Text(
                text = linkifiedReport(item.body ?: item.summary, content),
                color = content.copy(alpha = 0.82f),
                fontSize = 11.sp,
            )
        } else {
            Text(
                text = item.summary,
                color = content.copy(alpha = 0.75f),
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val REPORT_URL = Regex("""https?://[^\s)]+""")

/** Builds an annotated string from [text] with bare http(s) URLs rendered as tappable,
 *  underlined links. Plain function (not composable) — cheap enough to run on expand. */
private fun linkifiedReport(text: String, content: Color): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (match in REPORT_URL.findAll(text)) {
        if (match.range.first > last) append(text.substring(last, match.range.first))
        withLink(LinkAnnotation.Url(match.value)) {
            withStyle(SpanStyle(color = content, textDecoration = TextDecoration.Underline)) {
                append(match.value)
            }
        }
        last = match.range.last + 1
    }
    if (last < text.length) append(text.substring(last))
}

// ────────────────────────────────────────────────────────────────────
// News sources sheet — add / remove the RSS/Atom links the box pulls from
// ────────────────────────────────────────────────────────────────────

@Composable
private fun NewsSourcesSheet(
    sources: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf("") }
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
                text = "NEWS SOURCES",
                color = Foundry.labelPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Paste an RSS or Atom feed link. Each story shows its own article picture; tap ↻ on the box to refresh.",
                color = Foundry.labelSecondary,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    placeholder = { Text("https://example.com/rss", color = Foundry.labelMuted, fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF202020),
                        unfocusedContainerColor = Color(0xFF202020),
                        focusedTextColor = Foundry.labelPrimary,
                        unfocusedTextColor = Foundry.labelPrimary,
                    ),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                FoundryIconChip(
                    glyph = "＋",
                    onClick = {
                        onAdd(draft)
                        draft = ""
                    },
                    size = 44.dp,
                    contentDescription = "Add source",
                )
            }
            Spacer(Modifier.height(14.dp))
            if (sources.isEmpty()) {
                Text(
                    text = "Using the built-in default sources.",
                    color = Foundry.labelMuted,
                    fontSize = 12.sp,
                )
            } else {
                sources.forEach { src ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = src,
                            color = Foundry.labelPrimary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        FoundryIconChip(
                            glyph = "✕",
                            onClick = { onRemove(src) },
                            size = 32.dp,
                            contentDescription = "Remove source",
                        )
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────
// Destinations + preview data
// ────────────────────────────────────────────────────────────────────

/** Destinations the home can drive. The host (App.kt) maps these to routes. The
 *  provider/MCP entries are still reached from Settings even though the home no
 *  longer shows integration boxes. */
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

private val previewNewsItems = listOf(
    FoundryFeedItem(
        title = "Tap refresh to pull the latest stories",
        source = "News",
        summary = "Each story shows its own article thumbnail, not the site's logo.",
    ),
    FoundryFeedItem(
        title = "Add your own RSS or Atom sources",
        source = "Sources",
        summary = "Use ＋ to paste a feed link; remove the defaults any time.",
    ),
)

private val previewFeedItems = listOf(
    FoundryFeedItem(
        title = "Heartbeat surfaces here",
        source = "Heartbeat",
        summary = "Trending integrations, model drops, new MCP servers, and your pinned notes.",
    ),
    FoundryFeedItem(
        title = "On-device models, one tap",
        source = "Local",
        summary = "Included 1B for instant replies; pull a 3B tool-caller as a DLC when you want more.",
    ),
)
