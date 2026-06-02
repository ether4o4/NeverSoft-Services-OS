package com.ether4o4.morsvitaest.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ether4o4.morsvitaest.data.DataRepository
import com.ether4o4.morsvitaest.ui.handCursor
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Multi-model comparison surface.
 *
 * Pick 2+ enabled services, type a prompt, hit Run. The same prompt fires at
 * every selected service in parallel via DataRepository.askSilentlyWithInstance
 * — bypassing the main chat history so the user can A/B/C responses without
 * polluting their working conversation. Results stream in as each provider
 * returns; each card shows the service name + response + copy / use buttons.
 *
 * Heartbeat-derived feature: the Odysseus scan flagged "multi-model side-by-
 * side comparison" as a winning capability. This is the mobile-native take.
 */
@Composable
internal fun CompareContent(uiState: SettingsUiState, actions: SettingsActions) {
    val dataRepository = koinInject<DataRepository>()
    val scope = rememberCoroutineScope()

    var prompt by remember { mutableStateOf("") }
    val selected = remember { mutableStateMapOf<String, Boolean>() }
    val results: SnapshotStateMap<String, CompareCellState> = remember { mutableStateMapOf() }
    var running by remember { mutableStateOf(false) }

    // Only enabled services should appear here — disabled ones are
    // intentionally hidden from chat send paths too. Use the configured
    // service list filtered by enabled flag.
    val enabledServices = uiState.configuredServices.filter { it.enabled }

    LaunchedEffect(enabledServices) {
        // Default selection: first 2 enabled services so users have something
        // ready-to-go on first visit. Won't override an existing selection.
        if (selected.isEmpty()) {
            enabledServices.take(2).forEach { selected[it.instanceId] = true }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SettingsCard {
            Text(
                text = "Compare models",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Fire the same prompt at 2+ enabled services in parallel and see their responses side-by-side. Doesn't touch your main chat history.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (enabledServices.size < 2) {
            SettingsCard {
                Text(
                    text = "Need at least 2 enabled services to compare. Enable more in Settings → Services.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return
        }

        SettingsCard {
            Text(
                text = "Pick services",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            enabledServices.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .handCursor(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = selected[entry.instanceId] == true,
                        onCheckedChange = { selected[entry.instanceId] = it },
                    )
                    Spacer(Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.service.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        entry.selectedModel?.let {
                            Text(
                                text = it.id,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        SettingsCard {
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Prompt") },
                placeholder = { Text("Type something you want every selected model to answer…") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = !running && prompt.isNotBlank() && selected.count { it.value } >= 2,
                    onClick = {
                        running = true
                        results.clear()
                        val chosen = selected.filter { it.value }.keys.toList()
                        chosen.forEach { id -> results[id] = CompareCellState.Loading }
                        scope.launch {
                            try {
                                chosen.map { id ->
                                    async {
                                        val response = runCatching {
                                            dataRepository.askSilentlyWithInstance(id, prompt, timeoutMs = 60_000L)
                                        }
                                        results[id] = response.fold(
                                            onSuccess = { CompareCellState.Done(it.ifBlank { "(empty response)" }) },
                                            onFailure = { CompareCellState.Error(it.message ?: "Unknown error") },
                                        )
                                    }
                                }.awaitAll()
                            } finally {
                                running = false
                            }
                        }
                    },
                    modifier = Modifier.handCursor(),
                ) {
                    if (running) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Running…")
                    } else {
                        Text("Run all")
                    }
                }
                if (results.isNotEmpty() && !running) {
                    TextButton(onClick = { results.clear() }, modifier = Modifier.handCursor()) {
                        Text("Clear")
                    }
                }
            }
        }

        // Results
        enabledServices.filter { (selected[it.instanceId] == true) || results.containsKey(it.instanceId) }
            .forEach { entry ->
                val state = results[entry.instanceId] ?: return@forEach
                CompareResultCard(
                    serviceName = entry.service.displayName,
                    modelId = entry.selectedModel?.id,
                    state = state,
                )
            }
    }
}

private sealed interface CompareCellState {
    data object Loading : CompareCellState
    data class Done(val text: String) : CompareCellState
    data class Error(val message: String) : CompareCellState
}

@Composable
private fun CompareResultCard(
    serviceName: String,
    modelId: String?,
    state: CompareCellState,
) {
    SettingsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = serviceName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (!modelId.isNullOrBlank()) {
                    Text(
                        text = modelId,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        when (state) {
            is CompareCellState.Loading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Waiting on response…", style = MaterialTheme.typography.bodySmall)
                }
            }
            is CompareCellState.Done -> {
                Text(
                    text = state.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            is CompareCellState.Error -> {
                Text(
                    text = "Error: ${state.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
