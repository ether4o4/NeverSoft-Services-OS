package com.ether4o4.morsvitaest.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
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
    var errorResult by remember { mutableStateOf<GgufServerManager.GenericResult?>(null) }

    suspend fun refresh() {
        // Defensive: don't overwrite `status` or `models` with empty/failed
        // results — if the script-install race causes runQuick to return the
        // SCRIPT_INSTALL_FAILED_JSON sentinel, the resulting Status has
        // provisioned=false and ListModelsResult has empty models. Overwriting
        // a previously-correct state with that wipes the UI back to "not built
        // yet" + no downloaded models even though both are actually fine on
        // disk. Only commit a new status if the call returned a non-default
        // shape; only commit a new model list if the call reported ok.
        val newStatus = manager.status()
        if (newStatus.provisioned || newStatus.running || status == null) {
            status = newStatus
        }
        val newModels = manager.listModels()
        if (newModels.ok || (status?.provisioned != true)) {
            models = newModels.models
        }
    }

    LaunchedEffect(sandboxStatus.ready) {
        if (sandboxStatus.ready) refresh()
    }

    // Fire-and-forget a long sandbox op while showing a single busy state.
    // The block sets `message` (toast-style) or `errorResult` (dialog) directly.
    fun runOp(label: String, block: suspend () -> Unit) {
        if (busy) return
        busy = true
        busyLabel = label
        message = null
        errorResult = null
        scope.launch {
            try {
                block()
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
                        if (r.ok) message = "Engine ready" else errorResult = r
                    }
                },
                modifier = Modifier.handCursor(),
            ) { Text("Set up engine") }
        } else {
            // Quick-install buttons: curated GGUF models that are known to work
            // with the current llama.cpp build. Removes the "what do I type"
            // friction for new users — one tap and the right repo id is filled in.
            Text(
                text = "Quick install — tap to fill in a known-working model:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                OutlinedButton(
                    onClick = { repoInput = "bartowski/Qwen2.5-0.5B-Instruct-GGUF" },
                    modifier = Modifier.weight(1f).handCursor(),
                ) { Text("Tiny\n(0.5B • ~400MB)", style = MaterialTheme.typography.bodySmall) }
                OutlinedButton(
                    onClick = { repoInput = "bartowski/Qwen2.5-3B-Instruct-GGUF" },
                    modifier = Modifier.weight(1f).handCursor(),
                ) { Text("Recommended\n(3B • ~2GB)", style = MaterialTheme.typography.bodySmall) }
                OutlinedButton(
                    onClick = { repoInput = "bartowski/Qwen2.5-7B-Instruct-GGUF" },
                    modifier = Modifier.weight(1f).handCursor(),
                ) { Text("Big\n(7B • ~4.5GB)", style = MaterialTheme.typography.bodySmall) }
            }
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = repoInput,
                onValueChange = { repoInput = it },
                label = { Text("HuggingFace repo, repo URL, or .gguf URL") },
                placeholder = { Text("bartowski/Qwen2.5-0.5B-Instruct-GGUF") },
                singleLine = true,
                supportingText = {
                    Text(
                        text = "Must be a GGUF repo (e.g. bartowski/…-GGUF or litert-community/…). Vanilla model repos like 'gpt2' don't contain .gguf files and won't work.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = quantInput,
                onValueChange = { quantInput = it },
                label = { Text("Quant (optional — default picks Q4_K_M)") },
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
                        // Normalize: strip a full HuggingFace URL down to a repo id
                        // so users can paste either "owner/repo", "https://huggingface.co/owner/repo",
                        // or the file URL and it just works.
                        val raw = repoInput.trim()
                        val normalized = when {
                            raw.startsWith("https://huggingface.co/") || raw.startsWith("http://huggingface.co/") -> {
                                val path = raw.substringAfter("huggingface.co/").trimEnd('/')
                                // Direct .gguf download URL: keep as-is, the script handles it.
                                if (path.contains("/resolve/") && path.endsWith(".gguf", ignoreCase = true)) raw
                                // Otherwise reduce to owner/repo (drop any /tree/main/... or /blob/... suffix)
                                else path.split("/").take(2).joinToString("/")
                            }
                            else -> raw
                        }
                        val r = manager.pull(normalized, quantInput.trim().ifBlank { null })
                        if (r.ok) message = "Downloaded ${r.file ?: "model"}" else errorResult = r
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
                                        val r = manager.stop()
                                        if (r.ok) message = "Stopped" else errorResult = r
                                    }
                                },
                                modifier = Modifier.handCursor(),
                            ) { Text("Stop") }
                        } else {
                            Button(
                                onClick = {
                                    runOp("Starting ${m.name}…") {
                                        val r = manager.serve(m.name)
                                        if (r.ok) message = "Running. Tap \"Add as service\" below." else errorResult = r
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

    val err = errorResult
    if (err != null) {
        ProvisionErrorDialog(
            result = err,
            manager = manager,
            onDismiss = { errorResult = null },
        )
    }
}

@Composable
private fun ProvisionErrorDialog(
    result: GgufServerManager.GenericResult,
    manager: GgufServerManager,
    onDismiss: () -> Unit,
) {
    var logTail by remember(result) { mutableStateOf<String?>(null) }
    var loadingLog by remember(result) { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(result) {
        val path = result.logPath
        if (!path.isNullOrBlank()) {
            loadingLog = true
            logTail = runCatching { manager.readLogTail(path) }.getOrNull()
            loadingLog = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Error: ${result.error ?: "unknown"}") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                result.detail?.takeIf { it.isNotBlank() }?.let {
                    Text("Detail", style = MaterialTheme.typography.titleSmall)
                    Text(it, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                }
                result.hint?.takeIf { it.isNotBlank() }?.let {
                    Text("Suggested fix", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                    Spacer(Modifier.height(12.dp))
                }
                when {
                    loadingLog -> {
                        Text("Log", style = MaterialTheme.typography.titleSmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("loading…", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    !logTail.isNullOrBlank() -> {
                        Text("Log (last 8KB)", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = logTail.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.heightIn(max = 240.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val text = buildString {
                    appendLine("Error: ${result.error ?: "unknown"}")
                    result.detail?.takeIf { it.isNotBlank() }?.let { appendLine("Detail: $it") }
                    result.hint?.takeIf { it.isNotBlank() }?.let { appendLine("Hint: $it") }
                    logTail?.takeIf { it.isNotBlank() }?.let { appendLine("Log:\n$it") }
                }
                clipboard.setText(AnnotatedString(text))
            }) { Text("Copy") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        },
    )
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
