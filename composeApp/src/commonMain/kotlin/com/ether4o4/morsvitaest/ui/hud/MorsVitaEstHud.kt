package com.ether4o4.morsvitaest.ui.hud

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ether4o4.morsvitaest.data.AppSettings
import com.ether4o4.morsvitaest.data.DataRepository
import com.ether4o4.morsvitaest.data.Project
import com.ether4o4.morsvitaest.ui.chat.ChatViewModel
import com.ether4o4.morsvitaest.ui.chat.History
import org.koin.compose.koinInject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Mobile-first home HUD. Vertical glass stack over a procedural aerial-
 * nature backdrop, ordered title → feed → settings strip → chat.
 *
 * The feed is user-managed: tap "+" in the feed header to paste any URL,
 * long-press a card to share it into the current chat or into one of the
 * existing projects (the active project flips, then the URL is sent as
 * a user message and the full chat screen opens so the conversation is
 * visible). Feed entries persist via [AppSettings.getHudFeedItems].
 *
 * No new chat / message / project plumbing — the share actions reuse
 * [DataRepository.setActiveProjectId] and [com.ether4o4.morsvitaest.ui.chat.ChatActions.ask]
 * exactly as ChatScreen does.
 */
@OptIn(ExperimentalUuidApi::class)
@Composable
fun MorsVitaEstHud(
    viewModel: ChatViewModel,
    onNavigateToSettings: () -> Unit,
    onOpenFullChat: () -> Unit,
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val appSettings = koinInject<AppSettings>()
    val dataRepo = koinInject<DataRepository>()

    var feedItems by remember { mutableStateOf(appSettings.getHudFeedItems()) }
    var composerText by rememberSaveable { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var shareTarget by remember { mutableStateOf<HudFeedItem?>(null) }

    fun saveFeed(next: List<HudFeedItem>) {
        feedItems = next
        appSettings.setHudFeedItems(next)
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
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .widthIn(max = 560.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TitleBar()

            FeedPanel(
                items = feedItems,
                onAddClick = { showAddDialog = true },
                onItemLongPress = { shareTarget = it },
                modifier = Modifier.weight(1f),
            )

            SettingsStrip(onAnyTab = onNavigateToSettings)

            ChatPanel(
                history = uiState.history,
                composerText = composerText,
                onComposerChange = { composerText = it },
                isLoading = uiState.isLoading,
                onSend = {
                    if (uiState.isLoading) return@ChatPanel
                    val trimmed = composerText.trim()
                    if (trimmed.isNotEmpty()) {
                        composerText = ""
                        uiState.actions.ask(trimmed)
                    }
                },
                onOpenFullChat = onOpenFullChat,
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (showAddDialog) {
        AddFeedDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { url, label ->
                val cleanedUrl = url.trim().let {
                    if (it.startsWith("http://") || it.startsWith("https://")) it else "https://$it"
                }
                val cleanedLabel = label.trim().ifBlank { shortLabelFor(cleanedUrl) }
                val newItem = HudFeedItem(
                    id = "user-${Uuid.random()}",
                    url = cleanedUrl,
                    label = cleanedLabel,
                    accentHex = accentForUrl(cleanedUrl),
                )
                // Newly added rises to the top so the user sees it land.
                saveFeed(listOf(newItem) + feedItems)
                showAddDialog = false
            },
        )
    }

    shareTarget?.let { target ->
        ShareSheet(
            item = target,
            projects = remember(dataRepo, target) { dataRepo.getProjects() },
            activeProjectId = remember(dataRepo, target) {
                dataRepo.getActiveProject()?.id ?: Project.NONE_ID
            },
            onDismiss = { shareTarget = null },
            onShareToChat = {
                shareTarget = null
                uiState.actions.ask(buildShareMessage(target))
                onOpenFullChat()
            },
            onShareToProject = { project ->
                shareTarget = null
                dataRepo.setActiveProjectId(project.id)
                uiState.actions.ask(buildShareMessage(target))
                onOpenFullChat()
            },
            onRemove = {
                shareTarget = null
                saveFeed(feedItems.filterNot { it.id == target.id })
            },
        )
    }
}

private fun buildShareMessage(item: HudFeedItem): String {
    val label = item.label.ifBlank { shortLabelFor(item.url) }
    return "From my feed — $label: ${item.url}"
}

@Composable
private fun TitleBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PulseOrb()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Mors Vita Est",
                color = Color(0xFFFF3D5A),
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
            )
            Text(
                text = "Private Local Intelligence",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                letterSpacing = 1.2.sp,
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
    Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(Color(0xFFE53935).copy(alpha = alpha * 0.25f)),
        )
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color(0xFFE53935).copy(alpha = 0.95f)),
        )
    }
}

@Composable
private fun FeedPanel(
    items: List<HudFeedItem>,
    onAddClick: () -> Unit,
    onItemLongPress: (HudFeedItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassSurface(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Live Feed",
                    color = Color(0xFFFF5A72),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.8.sp,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${items.size} source${if (items.size == 1) "" else "s"}",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 10.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935).copy(alpha = 0.85f))
                        .clickable(onClick = onAddClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add URL",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            if (items.isEmpty()) {
                FeedEmptyState(onAddClick)
            } else {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(items, key = { it.id }) { item ->
                        FeedItemRow(item = item, onLongPress = { onItemLongPress(item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedEmptyState(onAddClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Feed is empty",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Tap + to paste any URL.",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 11.sp,
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onAddClick) {
                Text("Add a link", color = Color(0xFFFF5A72), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun FeedItemRow(item: HudFeedItem, onLongPress: () -> Unit) {
    val uri = LocalUriHandler.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .combinedFeedClickable(
                onClick = { uri.openUri(item.url) },
                onLongClick = onLongPress,
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FeedThumb(item)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = shortLabelFor(item.url).uppercase(),
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 9.sp,
                letterSpacing = 1.2.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.label.ifBlank { item.url },
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Long-press to share",
                color = Color.White.copy(alpha = 0.40f),
                fontSize = 10.sp,
                fontStyle = FontStyle.Italic,
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = "→",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 14.sp,
        )
    }
}

/**
 * Compose's foundation.clickable doesn't expose long-press in the
 * commonMain stable API used by this app, so we hand-roll it via the
 * combined-clickable helper that ships with foundation. Wrapped here
 * to keep the call site readable and to opt into the experimental API
 * in one place.
 */
@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.combinedFeedClickable(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
): Modifier = this.combinedClickable(
    onClick = onClick,
    onLongClick = onLongClick,
)

@Composable
private fun FeedThumb(item: HudFeedItem) {
    val accent = Color(item.accentHex)
    val initial = shortLabelFor(item.url).firstOrNull()?.uppercase() ?: "?"
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.85f), accent.copy(alpha = 0.30f)),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SettingsStrip(onAnyTab: () -> Unit) {
    // Strip mirrors the labels used in the Settings screen so the user
    // recognises where each name leads. Every tap navigates to Settings
    // — picking the actual tab still happens inside Settings, no deep-
    // link plumbing changed for v1.
    val tabs = listOf(
        "Tools", "Local LLM", "Integrations", "General",
        "Agent", "Projects", "Services", "Compare",
    )
    Box(
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
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp)),
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { i, label ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onAnyTab() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = label,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                if (i < tabs.lastIndex) {
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.20f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatPanel(
    history: List<History>,
    composerText: String,
    onComposerChange: (String) -> Unit,
    isLoading: Boolean,
    onSend: () -> Unit,
    onOpenFullChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Show the tail of the conversation as bubbles so the HUD reads like
    // a live chat surface, not a stale preview. Latest message at the
    // bottom; auto-scrolls when new messages arrive.
    val visible = remember(history) {
        history.filter { it.role == History.Role.USER || (it.role == History.Role.ASSISTANT && it.content.isNotBlank()) }
            .takeLast(6)
    }
    GlassSurface(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
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
                    text = when {
                        isLoading -> "Thinking…"
                        visible.isNotEmpty() -> "Live"
                        else -> "Idle"
                    },
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenFullChat)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "Open chat",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            val listState = rememberLazyListState()
            LaunchedEffect(visible.size, isLoading) {
                if (visible.isNotEmpty()) {
                    listState.animateScrollToItem(visible.lastIndex)
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (visible.isEmpty()) {
                    item("empty") {
                        Text(
                            text = "Quiet here. Type below to wake the local mind.",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                        )
                    }
                } else {
                    items(visible, key = { it.id }) { msg ->
                        MessageBubble(msg)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            ComposerRow(
                value = composerText,
                onValueChange = onComposerChange,
                isLoading = isLoading,
                onSend = onSend,
            )
        }
    }
}

@Composable
private fun MessageBubble(msg: History) {
    val isUser = msg.role == History.Role.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        val accent = if (isUser) Color(0xFFE53935) else Color(0xFF7CB342)
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(
                    if (isUser) RoundedCornerShape(14.dp, 14.dp, 4.dp, 14.dp)
                    else RoundedCornerShape(14.dp, 14.dp, 14.dp, 4.dp),
                )
                .background(
                    if (isUser) Color(0xFFE53935).copy(alpha = 0.14f)
                    else Color.White.copy(alpha = 0.06f),
                )
                .border(
                    1.dp,
                    if (isUser) Color(0xFFE53935).copy(alpha = 0.30f)
                    else Color.White.copy(alpha = 0.12f),
                    if (isUser) RoundedCornerShape(14.dp, 14.dp, 4.dp, 14.dp)
                    else RoundedCornerShape(14.dp, 14.dp, 14.dp, 4.dp),
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Text(
                text = if (isUser) "YOU" else "ASSISTANT",
                color = accent.copy(alpha = 0.85f),
                fontSize = 9.sp,
                letterSpacing = 1.4.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = msg.content,
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 13.sp,
                lineHeight = 17.sp,
                maxLines = 8,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ComposerRow(
    value: String,
    onValueChange: (String) -> Unit,
    isLoading: Boolean,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
            cursorBrush = SolidColor(Color(0xFFE53935)),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            text = "Speak into the session…",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 14.sp,
                        )
                    }
                    inner()
                }
            },
        )
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFFF213F), Color(0xFF9C0A1F)),
                    ),
                )
                .clickable(enabled = !isLoading, onClick = onSend),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color.White,
                )
            } else {
                Text(
                    text = "↑",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun AddFeedDialog(
    onDismiss: () -> Unit,
    onConfirm: (url: String, label: String) -> Unit,
) {
    var url by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add to feed",
                color = Color(0xFFFF3D5A),
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column {
                Text(
                    text = "Paste any URL. It joins your live feed and stays across launches. Long-press a card to share it into chat or a project.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    placeholder = { Text("https://…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label (optional)") },
                    placeholder = { Text("Short title for the card") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(url, label) },
                enabled = url.isNotBlank(),
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun ShareSheet(
    item: HudFeedItem,
    projects: List<Project>,
    activeProjectId: String,
    onDismiss: () -> Unit,
    onShareToChat: () -> Unit,
    onShareToProject: (Project) -> Unit,
    onRemove: () -> Unit,
) {
    val uri = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Share",
                color = Color(0xFFFF3D5A),
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column {
                Text(
                    text = item.label.ifBlank { item.url },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.url,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(14.dp))

                // Share to current chat
                ShareRowButton(label = "Share to current chat", onClick = onShareToChat)

                if (projects.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Share to a project",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(6.dp))
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 180.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(projects, key = { it.id }) { project ->
                            ShareRowButton(
                                label = project.name,
                                trailing = if (project.id == activeProjectId) "active" else null,
                                onClick = { onShareToProject(project) },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(Modifier.height(8.dp))
                Row {
                    TextButton(onClick = {
                        uri.openUri(item.url)
                        onDismiss()
                    }) {
                        Text("Open link")
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = onRemove,
                    ) {
                        Text(
                            text = "Remove",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}

@Composable
private fun ShareRowButton(
    label: String,
    trailing: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (trailing != null) {
            Text(
                text = trailing,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun GlassSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
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
 * Procedural aerial-nature backdrop drawn with layered radial gradients
 * on the modifier's draw surface — three deep-forest hue blobs at
 * different positions plus a soft vignette so the glass panels read
 * clearly in the centre. No external image asset.
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
