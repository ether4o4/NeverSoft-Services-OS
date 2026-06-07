@file:OptIn(ExperimentalMaterial3Api::class)

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ether4o4.morsvitaest.BackIcon
import com.ether4o4.morsvitaest.ui.handCursor
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
        onClear = viewModel::clear,
    )
}

@Composable
private fun CompareContent(
    state: CompareUiState,
    onBack: () -> Unit,
    onSelectPane: (ComparePane, String) -> Unit,
    onToggleMerge: (Boolean) -> Unit,
    onSend: (String) -> Unit,
    onClear: () -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack, modifier = Modifier.handCursor()) {
                    Icon(BackIcon, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                }
                Text(
                    text = "Compare LLMs",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "Merge",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(6.dp))
                Switch(checked = state.merge, onCheckedChange = onToggleMerge, modifier = Modifier.handCursor())
                Spacer(Modifier.width(8.dp))
            }

            Text(
                text = if (state.merge) {
                    "Merge on — the two models reply to each other, 2 turns each."
                } else {
                    "Both models answer the same prompt, side by side."
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
            )

            HorizontalDivider()

            if (state.entries.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Add services in Settings to compare your favorite LLMs.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            } else {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    PaneColumn(ComparePane.A, state, onSelectPane, Modifier.weight(1f))
                    VerticalDivider()
                    PaneColumn(ComparePane.B, state, onSelectPane, Modifier.weight(1f))
                }
            }

            state.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(if (state.merge) "Start a debate…" else "Ask both…") },
                    enabled = !state.isLoading && state.entries.isNotEmpty(),
                    maxLines = 4,
                )
                Spacer(Modifier.width(8.dp))
                if (state.isLoading) {
                    CircularProgressIndicator(Modifier.size(28.dp))
                } else {
                    val enabled = input.isNotBlank() && state.canSend
                    IconButton(
                        onClick = {
                            onSend(input)
                            input = ""
                        },
                        enabled = enabled,
                        modifier = Modifier.handCursor(),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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

    Column(modifier.fillMaxHeight().padding(8.dp)) {
        Box(Modifier.fillMaxWidth()) {
            Surface(
                onClick = { expanded = true },
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().handCursor(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = state.labelFor(selectedId),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }
            if (state.isLoading && messages.isEmpty()) {
                item {
                    CircularProgressIndicator(Modifier.size(20.dp).padding(2.dp))
                }
            }
        }
    }
}
