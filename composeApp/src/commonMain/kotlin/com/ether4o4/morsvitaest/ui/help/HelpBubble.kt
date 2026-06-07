@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.ether4o4.morsvitaest.ui.help

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ether4o4.morsvitaest.ui.foundry.Foundry
import com.ether4o4.morsvitaest.ui.foundry.FoundryIconChip
import com.ether4o4.morsvitaest.ui.foundry.FoundryIntent
import com.ether4o4.morsvitaest.ui.foundry.FoundryPill
import com.ether4o4.morsvitaest.ui.handCursor
import org.koin.compose.viewmodel.koinViewModel

/**
 * The always-present "tap for help" chat bubble — a small brushed-metal disc the
 * caller pins to a corner. Tapping it opens the [HelpAssistantSheet].
 */
@Composable
fun HelpBubble(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(Foundry.pillShape)
            .background(brush = Foundry.intentPrimary, shape = Foundry.pillShape)
            .background(brush = Foundry.glossOverlay, shape = Foundry.pillShape)
            .border(Foundry.bevelThick, Foundry.pillShape)
            .handCursor()
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "Open help"
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
    }
}

/**
 * The help panel: a short over-simplified guide, quick buttons that jump straight
 * to the setup screens, prompt suggestions, and a live single-turn chat with the
 * built-in free assistant.
 */
@Composable
fun HelpAssistantSheet(
    onDismiss: () -> Unit,
    onOpenServices: () -> Unit,
    onOpenMcp: () -> Unit,
    onReplayTour: () -> Unit,
    viewModel: HelpAssistantViewModel = koinViewModel(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val state by viewModel.state.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = "Help & setup",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
            Spacer(Modifier.size(4.dp))
            Text(
                text = "The basics, in one line each:",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Spacer(Modifier.size(8.dp))
            GuideLine("Chat", "talk to the built-in AI — it's free and already on.")
            GuideLine("Multi chat", "put two models head-to-head on the same question.")
            GuideLine("Shell", "a safe sandbox terminal the AI can run commands in.")
            GuideLine("Services", "add your own AI keys (OpenAI, Gemini, Groq…).")
            GuideLine("MCP", "plug in tool servers so the AI can do more.")

            Spacer(Modifier.size(12.dp))
            // Quick actions — the buttons that actually set things up for the user.
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FoundryPill(
                    label = "➕  ADD AN AI",
                    onClick = {
                        onDismiss()
                        onOpenServices()
                    },
                    intent = FoundryIntent.Secondary,
                    minHeight = 38.dp,
                )
                FoundryPill(
                    label = "🔌  CONNECT MCP",
                    onClick = {
                        onDismiss()
                        onOpenMcp()
                    },
                    intent = FoundryIntent.Secondary,
                    minHeight = 38.dp,
                )
                FoundryPill(
                    label = "↺  REPLAY TOUR",
                    onClick = {
                        onDismiss()
                        onReplayTour()
                    },
                    intent = FoundryIntent.Neutral,
                    minHeight = 38.dp,
                )
            }

            Spacer(Modifier.size(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.size(12.dp))

            // Live chat with the built-in assistant.
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 260.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.messages) { message -> HelpBubbleRow(message) }
                if (state.isLoading) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.size(8.dp))
                            Text(
                                text = "Thinking…",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }

            state.error?.let { error ->
                Spacer(Modifier.size(6.dp))
                Text(text = error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }

            Spacer(Modifier.size(10.dp))
            // Prompt suggestions — one tap to ask a common setup question.
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SUGGESTIONS.forEach { suggestion ->
                    SuggestionChip(text = suggestion, enabled = !state.isLoading) {
                        viewModel.ask(suggestion)
                    }
                }
            }

            Spacer(Modifier.size(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    BasicTextField(
                        value = input,
                        onValueChange = { input = it },
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (input.isEmpty()) {
                                Text(
                                    text = "Ask for help…",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp,
                                )
                            }
                            inner()
                        },
                    )
                }
                FoundryIconChip(
                    glyph = "➤",
                    onClick = {
                        if (input.isNotBlank() && !state.isLoading) {
                            viewModel.ask(input)
                            input = ""
                        }
                    },
                    size = 44.dp,
                    tint = if (input.isNotBlank() && !state.isLoading) Foundry.labelPrimary else Foundry.labelMuted,
                    contentDescription = "Send",
                )
            }
        }
    }
}

@Composable
private fun GuideLine(term: String, definition: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = "$term — ",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
        )
        Text(
            text = definition,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun HelpBubbleRow(message: HelpAssistantViewModel.Message) {
    val alignment = if (message.fromUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (message.fromUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }
    val textColor = if (message.fromUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Text(
            text = message.text,
            color = textColor,
            fontSize = 13.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(bubbleColor, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun SuggestionChip(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(Foundry.pillShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), Foundry.pillShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, Foundry.pillShape)
            .handCursor()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(text = text, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
    }
}

private val SUGGESTIONS = listOf(
    "How do I add my own AI?",
    "Connect an MCP server",
    "What can the Shell do?",
    "What's Multi chat?",
)
