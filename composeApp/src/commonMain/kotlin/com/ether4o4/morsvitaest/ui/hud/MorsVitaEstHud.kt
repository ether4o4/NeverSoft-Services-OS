package com.ether4o4.morsvitaest.ui.hud

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ether4o4.morsvitaest.ui.chat.ChatViewModel
import com.ether4o4.morsvitaest.ui.chat.History
import com.ether4o4.morsvitaest.ui.chat.lastRenderedAssistant

/**
 * Mobile-first home HUD. Vertical glass stack centered on the screen over a
 * procedural aerial-nature backdrop: title header with pulse orb, a thin top
 * menu, one glass panel for the chat session (status + message preview +
 * composer + red primary action), one glass panel for a curated link feed,
 * then a compact bottom dock for app-level controls.
 *
 * No new feature logic — the composer routes the same `ChatActions.ask` the
 * existing ChatScreen uses, and every other tap is either a navigation
 * callback or a visual placeholder.
 */
@Composable
fun MorsVitaEstHud(
    viewModel: ChatViewModel,
    onNavigateToSettings: () -> Unit,
    onOpenFullChat: () -> Unit,
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    var composerText by rememberSaveable { mutableStateOf("") }

    val lastAssistant = remember(uiState.history) {
        uiState.history.lastRenderedAssistant()
    }
    val hasMessages = remember(uiState.history) {
        uiState.history.any { it.role == History.Role.USER || it.role == History.Role.ASSISTANT }
    }

    val statusLabel = when {
        uiState.isLoading -> "Thinking…"
        hasMessages -> "Ready"
        else -> "Idle"
    }
    val primaryLabel = when {
        uiState.isLoading -> "Working"
        composerText.isNotBlank() -> "Send"
        hasMessages -> "Wake"
        else -> "Begin"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060A08))
            .natureBackdrop(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .widthIn(max = 560.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HeaderRow()
            TopMenuStrip(
                onTab = { tab ->
                    when (tab) {
                        HudTab.Chat -> onOpenFullChat()
                        HudTab.Settings -> onNavigateToSettings()
                        // Engine / Models / Files / History all live inside
                        // Settings today — navigating there is the safe,
                        // non-feature-changing thing to do for v1.
                        else -> onNavigateToSettings()
                    }
                },
            )
            ChatGlassPanel(
                statusLabel = statusLabel,
                isLoading = uiState.isLoading,
                lastAssistantPreview = lastAssistant?.content,
                composerText = composerText,
                onComposerChange = { composerText = it },
                primaryLabel = primaryLabel,
                onPrimary = {
                    if (uiState.isLoading) return@ChatGlassPanel
                    val trimmed = composerText.trim()
                    if (trimmed.isNotEmpty()) {
                        composerText = ""
                        uiState.actions.ask(trimmed)
                    } else if (!hasMessages) {
                        onOpenFullChat()
                    }
                },
                onOpenFullChat = onOpenFullChat,
            )
            FeedGlassPanel(items = curatedFeed)
            Spacer(Modifier.height(4.dp))
            BottomDock(onSettings = onNavigateToSettings)
        }
    }
}

@Composable
private fun HeaderRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PulseOrb()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Mors Vita Est",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Private Local Intelligence",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun PulseOrb() {
    val transition = rememberInfiniteTransition(label = "hud-pulse")
    val scale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 1400, easing = LinearEasing),
            RepeatMode.Reverse,
        ),
        label = "scale",
    )
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 1400, easing = LinearEasing),
            RepeatMode.Reverse,
        ),
        label = "alpha",
    )
    Box(
        modifier = Modifier
            .size(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Soft halo
        Box(
            modifier = Modifier
                .size(24.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(Color(0xFFE53935).copy(alpha = alpha * 0.25f)),
        )
        // Hard orb
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color(0xFFE53935).copy(alpha = 0.95f)),
        )
    }
}

private enum class HudTab { Chat, Engine, Models, Files, History, Settings }

@Composable
private fun TopMenuStrip(onTab: (HudTab) -> Unit) {
    val entries = listOf(
        HudTab.Chat to "Chat",
        HudTab.Engine to "Engine",
        HudTab.Models to "Models",
        HudTab.Files to "Files",
        HudTab.History to "History",
        HudTab.Settings to "Settings",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.05f),
                        Color.White.copy(alpha = 0.02f),
                    ),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        entries.forEach { (tab, label) ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onTab(tab) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = label,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun ChatGlassPanel(
    statusLabel: String,
    isLoading: Boolean,
    lastAssistantPreview: String?,
    composerText: String,
    onComposerChange: (String) -> Unit,
    primaryLabel: String,
    onPrimary: () -> Unit,
    onOpenFullChat: () -> Unit,
) {
    GlassSurface {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = Color(0xFFE53935),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF7CB342)),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = statusLabel,
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenFullChat() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "Open chat",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                    )
                }
            }

            val preview = lastAssistantPreview?.takeIf { it.isNotBlank() }
            if (preview != null) {
                Text(
                    text = preview,
                    color = Color.White.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Text(
                    text = "Quiet here. Type below to wake the local mind.",
                    color = Color.White.copy(alpha = 0.55f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                BasicTextField(
                    value = composerText,
                    onValueChange = onComposerChange,
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    cursorBrush = SolidColor(Color(0xFFE53935)),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.10f),
                            RoundedCornerShape(14.dp),
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (composerText.isEmpty()) {
                                Text(
                                    text = "Speak…",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 14.sp,
                                )
                            }
                            inner()
                        }
                    },
                )
                Spacer(Modifier.width(10.dp))
                PrimaryRedAction(
                    label = primaryLabel,
                    enabled = !isLoading,
                    onClick = onPrimary,
                )
            }
        }
    }
}

@Composable
private fun PrimaryRedAction(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
        modifier = Modifier
            .defaultMinSize(minHeight = 44.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFE53935),
            contentColor = Color.White,
            disabledContainerColor = Color(0xFFE53935).copy(alpha = 0.4f),
            disabledContentColor = Color.White.copy(alpha = 0.7f),
        ),
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
        )
    }
}

private data class FeedItem(
    val title: String,
    val source: String,
    val status: String,
    val accent: Color,
    val url: String,
)

private val curatedFeed = listOf(
    FeedItem(
        title = "MVE preview build",
        source = "github releases",
        status = "Fresh",
        accent = Color(0xFFE53935),
        url = "https://github.com/ether4o4/MorsVitaEst/releases/tag/android-preview-latest",
    ),
    FeedItem(
        title = "Dolphin 3.0 — Llama 3.2 3B",
        source = "huggingface.co",
        status = "Live",
        accent = Color(0xFF7CB342),
        url = "https://huggingface.co/cognitivecomputations/Dolphin3.0-Llama3.2-3B-GGUF",
    ),
    FeedItem(
        title = "llama.cpp",
        source = "github.com",
        status = "Engine",
        accent = Color(0xFFFFB300),
        url = "https://github.com/ggerganov/llama.cpp",
    ),
    FeedItem(
        title = "Model Context Protocol",
        source = "modelcontextprotocol.io",
        status = "Spec",
        accent = Color(0xFF42A5F5),
        url = "https://modelcontextprotocol.io",
    ),
    FeedItem(
        title = "Awesome MCP servers",
        source = "github.com",
        status = "Browse",
        accent = Color(0xFFAB47BC),
        url = "https://github.com/punkpeye/awesome-mcp-servers",
    ),
)

@Composable
private fun FeedGlassPanel(items: List<FeedItem>) {
    GlassSurface {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Signals",
                    color = Color.White.copy(alpha = 0.95f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "curated · local",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                )
            }
            Spacer(Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
            ) {
                items(items) { item ->
                    FeedCard(item)
                }
            }
        }
    }
}

@Composable
private fun FeedCard(item: FeedItem) {
    val uri = LocalUriHandler.current
    Column(
        modifier = Modifier
            .width(170.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(
                1.dp,
                Color.White.copy(alpha = 0.10f),
                RoundedCornerShape(14.dp),
            )
            .clickable { uri.openUri(item.url) }
            .padding(10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            item.accent.copy(alpha = 0.7f),
                            item.accent.copy(alpha = 0.25f),
                        ),
                    ),
                ),
            contentAlignment = Alignment.BottomStart,
        ) {
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = item.status,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = item.title,
            color = Color.White.copy(alpha = 0.95f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item.source,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(item.accent),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = "Open",
                color = Color(0xFFE53935),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "→",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun BottomDock(onSettings: () -> Unit) {
    // Sound / Theme / Saver are visual placeholders for the redesign — no
    // feature wiring yet, so each is a no-op clickable chip. Gear navigates
    // to the existing Settings screen, which is the one real action here.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .border(
                1.dp,
                Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(24.dp),
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DockChip(label = "Sound", onClick = { /* placeholder */ })
        DockChip(label = "Theme", onClick = { /* placeholder */ })
        DockChip(label = "Saver", onClick = { /* placeholder */ })
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFE53935).copy(alpha = 0.85f))
                .clickable { onSettings() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings",
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun DockChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun GlassSurface(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.03f),
                    ),
                ),
            )
            .border(
                1.dp,
                Color.White.copy(alpha = 0.12f),
                RoundedCornerShape(20.dp),
            ),
    ) {
        content()
    }
}

/**
 * Procedural "blurred aerial nature" backdrop drawn with layered radial
 * gradients on the modifier's draw surface. No external image asset —
 * three deep-forest hues blob at different positions, plus a soft vignette
 * to keep glass panels readable in the center of the screen.
 */
private fun Modifier.natureBackdrop(): Modifier = drawBehind {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF1A3F2B).copy(alpha = 0.55f),
                Color.Transparent,
            ),
            center = Offset(size.width * 0.22f, size.height * 0.18f),
            radius = size.width * 0.85f,
        ),
    )
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF2D4022).copy(alpha = 0.40f),
                Color.Transparent,
            ),
            center = Offset(size.width * 0.82f, size.height * 0.78f),
            radius = size.width * 0.95f,
        ),
    )
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF103428).copy(alpha = 0.35f),
                Color.Transparent,
            ),
            center = Offset(size.width * 0.55f, size.height * 0.50f),
            radius = size.width * 0.70f,
        ),
    )
    // Vignette — darken the corners so glass panels stand out from the
    // backdrop without needing an actual blur pass.
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                Color.Black.copy(alpha = 0.55f),
            ),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = size.maxDimension * 0.75f,
        ),
    )
}
