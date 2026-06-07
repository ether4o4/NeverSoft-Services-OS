package com.ether4o4.morsvitaest.ui.compare

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ether4o4.morsvitaest.ui.foundry.Foundry
import com.ether4o4.morsvitaest.ui.foundry.FoundryCard
import com.ether4o4.morsvitaest.ui.foundry.FoundryIconChip
import com.ether4o4.morsvitaest.ui.foundry.FoundryIntent
import com.ether4o4.morsvitaest.ui.foundry.FoundryPill
import com.ether4o4.morsvitaest.ui.foundry.FoundrySteelPill
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CompareScreen(
    onNavigateBack: () -> Unit,
    viewModel: CompareViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CompareContent(
        state = state,
        onBack = onNavigateBack,
        onSelectPane = viewModel::selectPane,
        onToggleMerge = viewModel::setMerge,
        onSend = viewModel::send,
    )
}

@Composable
private fun CompareContent(
    state: CompareUiState,
    onBack: () -> Unit,
    onSelectPane: (ComparePane, String) -> Unit,
    onToggleMerge: (Boolean) -> Unit,
    onSend: (String) -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }

    Box(
        Modifier
            .fillMaxSize()
            .background(Foundry.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(Foundry.pagePadding),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FoundryIconChip(glyph = "←", onClick = onBack, size = 40.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "COMPARE LLMS",
                    color = Foundry.wordmark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.weight(1f),
                )
                FoundryPill(
                    label = if (state.merge) "MERGE ON" else "MERGE OFF",
                    onClick = { onToggleMerge(!state.merge) },
                    intent = if (state.merge) FoundryIntent.Primary else FoundryIntent.Neutral,
                    minHeight = 40.dp,
                )
            }

            Spacer(Modifier.height(6.dp))
            Text(
                text = if (state.merge) {
                    "The two models reply to each other — 2 turns each, then it waits for you."
                } else {
                    "Both models answer the same prompt, side by side."
                },
                color = Foundry.labelSecondary,
                fontSize = 11.sp,
            )
            Spacer(Modifier.height(Foundry.gridGap))

            if (state.entries.isEmpty()) {
                FoundryCard(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    shape = Foundry.cardShapeLarge,
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Add services in Settings to compare your favorite LLMs.",
                            color = Foundry.labelSecondary,
                            fontSize = 13.sp,
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Foundry.gridGap),
                ) {
                    PaneColumn(ComparePane.A, state, onSelectPane, Modifier.weight(1f))
                    PaneColumn(ComparePane.B, state, onSelectPane, Modifier.weight(1f))
                }
            }

            state.error?.let { error ->
                Spacer(Modifier.height(6.dp))
                Text(
                    text = error,
                    color = Color(0xFFE57373),
                    fontSize = 12.sp,
                )
            }

            Spacer(Modifier.height(Foundry.gridGap))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FoundrySteelPill(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = input,
                        onValueChange = { input = it },
                        textStyle = TextStyle(color = Foundry.labelPrimary, fontSize = 14.sp),
                        cursorBrush = SolidColor(Foundry.labelPrimary),
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (input.isEmpty()) {
                                Text(
                                    text = if (state.merge) "Start a debate…" else "Ask both…",
                                    color = Foundry.labelMuted,
                                    fontSize = 14.sp,
                                )
                            }
                            inner()
                        },
                    )
                }
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                } else {
                    FoundryIconChip(
                        glyph = "➤",
                        onClick = {
                            if (input.isNotBlank() && state.canSend) {
                                onSend(input)
                                input = ""
                            }
                        },
                        size = 44.dp,
                        tint = if (input.isNotBlank() && state.canSend) Foundry.labelPrimary else Foundry.labelMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun PaneColumn(
    pane: ComparePane,
    state: CompareUiState,
    onSelectPane: (ComparePane, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedId = if (pane == ComparePane.A) state.paneAInstanceId else state.paneBInstanceId
    var expanded by remember { mutableStateOf(false) }

    FoundryCard(modifier = modifier.fillMaxHeight(), shape = Foundry.cardShapeLarge) {
        Box(Modifier.fillMaxWidth()) {
            FoundrySteelPill(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                minHeight = 40.dp,
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = state.labelFor(selectedId),
                        color = Foundry.labelPrimary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(text = "▼", color = Foundry.labelSecondary, fontSize = 12.sp)
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                state.entries.forEach { entry ->
                    DropdownMenuItem(
                        text = { Text("${entry.serviceName} · ${entry.modelId}") },
                        onClick = {
                            onSelectPane(pane, entry.instanceId)
                            expanded = false
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        val messages = remember(state.messages, pane) { state.messages.filter { it.pane == pane } }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(messages) { message ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(Foundry.tileShape)
                        .background(Color(0xFF1A1A1A), Foundry.tileShape)
                        .padding(10.dp),
                ) {
                    Text(
                        text = message.text,
                        color = Foundry.labelPrimary,
                        fontSize = 12.sp,
                    )
                }
            }
            if (state.isLoading && messages.isEmpty()) {
                item {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }
        }
    }
}
