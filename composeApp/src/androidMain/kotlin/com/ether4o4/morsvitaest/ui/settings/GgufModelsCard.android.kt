package com.ether4o4.morsvitaest.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ether4o4.morsvitaest.SandboxController
import com.ether4o4.morsvitaest.data.DataRepository
import com.ether4o4.morsvitaest.data.Service
import com.ether4o4.morsvitaest.sandbox.GgufServerManager
import com.ether4o4.morsvitaest.ui.handCursor
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Android implementation: download and run GGUF models through
 * [GgufServerManager] (llama.cpp inside the Linux sandbox), then one tap to
 * register the loopback server as an OpenAI-Compatible service.
 */
@Composable
actual fun PlatformGgufModelsCard() {
    val manager = koinInject<GgufServerManager>()
    val dataRepository = koinInject<DataRepository>()
    val sandboxController = koinInject<SandboxController>()
    val sandboxStatus by sandboxController.status.collectAsState()
    val scope = rememberCoroutineScope()

    var status by remember { mutableStateOf<GgufServerManager.Status?>(null) }
    var models by remember { mutableStateOf<List<GgufServerManager.ModelFile>>(emptyList()) }
    var repoInput by remember { mutableStateOf("") }
    var quantInput by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var busyLabel by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() {
        status = manager.status()
        models = manager.listModels().models
    }

    androidx.compose.runtime.LaunchedEffect(sandboxStatus.ready) {
        if (sandboxStatus.ready) refresh()
    }

    // Fire-and-forget a long sandbox op while showing a single busy state.
    fun runOp(label: String, block: suspend () -> String?) {
        if (busy) return
        busy = true
        busyLabel = label
        message = null
        scope.launch {
            try {
                block()?.let { message = it }
                refresh()
            } finally {
                busy = false
                busyLabel = ""
            }
        }
    }

    SettingsCard {
        Text(
            text = "Local Models (GGUF)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))

        if (!sandboxStatus.ready) {
            Text(
                text = "Set up the Alpine Linux sandbox above first — the model engine runs inside it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@SettingsCard
        }

        val st = status
        Text(
            text = "Engine: " + if (st?.provisioned == true) "ready" else "not built yet",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (st?.running == true) {
            Text(
                text = "Serving ${st.model} at ${st.baseUrl}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.height(12.dp))

        if (busy) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = busyLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else if (st == null || !st.provisioned) {
            Text(
                text = "First run compiles llama.cpp inside the sandbox. One-time, and slow on a phone (can take 10–30 min).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    runOp("Building engine… one-time, may take 10–30 min") {
                        val r = manager.provision()
                        if (r.ok) {
                            "Engine ready"
                        } else {
                            val code = r.error ?: "unknown"
                            val detail = r.detail?.takeIf { it.isNotBlank() }
                            if (detail != null) "Provision failed: $code — $detail" else "Provision failed: $code"
                        }
                    }
                },
                modifier = Modifier.handCursor(),
            ) { Text("Set up engine") }
        } else {
            OutlinedTextField(
                value = repoInput,
                onValueChange = { repoInput = it },
                label = { Text("HuggingFace repo or .gguf URL") },
                placeholder = { Text("bartowski/Qwen2.5-0.5B-Instruct-GGUF") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = quantInput,
                onValueChange = { quantInput = it },
                label = { Text("Quant (optional)") },
                placeholder = { Text("Q4_K_M") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Button(
                enabled = repoInput.isNotBlank(),
                onClick = {
                    runOp("Downloading model…") {
                        val r = manager.pull(repoInput.trim(), quantInput.trim().ifBlank { null })
                        if (r.ok) "Downloaded ${r.file ?: "model"}" else "Download failed: ${r.error ?: "unknown"}"
                    }
                },
                modifier = Modifier.handCursor(),
            ) { Text("Download") }

            if (models.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Downloaded models",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(4.dp))
                models.forEach { m ->
                    val isRunning = st.running && st.model == m.name
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = m.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                text = humanSize(m.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        if (isRunning) {
                            OutlinedButton(
                                onClick = {
                                    runOp("Stopping…") {
                                        manager.stop()
                                        "Stopped"
                                    }
                                },
                                modifier = Modifier.handCursor(),
                            ) { Text("Stop") }
                        } else {
                            Button(
                                onClick = {
                                    runOp("Starting ${m.name}…") {
                                        val r = manager.serve(m.name)
                                        if (r.ok) "Running. Tap \"Add as service\" below." else "Start failed: ${r.error ?: "unknown"}"
                                    }
                                },
                                modifier = Modifier.handCursor(),
                            ) { Text("Run") }
                        }
                    }
                }
            }

            if (st.running) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val baseUrl = manager.openAiBaseUrl
                        val existing = dataRepository.getConfiguredServiceInstances().firstOrNull {
                            it.serviceId == Service.OpenAICompatible.id &&
                                dataRepository.getInstanceBaseUrl(it.instanceId, Service.OpenAICompatible) == baseUrl
                        }
                        val instance = existing ?: dataRepository.addConfiguredService(Service.OpenAICompatible.id)
                            .also { dataRepository.updateInstanceBaseUrl(it.instanceId, baseUrl) }
                        message = "Added OpenAI-Compatible service → open Services to pick the model"
                        scope.launch {
                            runCatching { dataRepository.validateConnection(Service.OpenAICompatible, instance.instanceId) }
                        }
                    },
                    modifier = Modifier.handCursor(),
                ) { Text("Add as service") }
            }
        }

        message?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun humanSize(bytes: Long): String {
    if (bytes <= 0) return "—"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) {
        val gb = mb / 1024.0
        "${(gb * 10).toLong() / 10.0} GB"
    } else {
        "${mb.toLong()} MB"
    }
}
