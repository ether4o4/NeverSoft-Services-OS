package com.ether4o4.morsvitaest.ui.settings

import android.app.ActivityManager
import android.content.Context
import android.os.StatFs
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
import androidx.compose.ui.platform.LocalContext
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

    // Re-fetch every time the card composes — covers the case where a
    // background pull / provision finished while the user was on another
    // screen, so the model list and engine status show the latest truth
    // when they return without needing a manual refresh.
    LaunchedEffect(Unit) {
        if (sandboxStatus.ready) refresh()
    }

    // Fire-and-forget a long sandbox op while showing a single busy state.
    // The block sets `message` (toast-style) or `errorResult` (dialog) directly.
    // Launches on the GgufServerManager's long-lived scope (not the composable's
    // scope) so the underlying download / build keeps running even if the user
    // navigates away from the card mid-operation. The composable-scoped `busy`
    // flag still tracks UI state for the visible session; when the user returns
    // the next composition starts fresh and the LaunchedEffect above re-pulls
    // real state from the script.
    fun runOp(label: String, block: suspend () -> Unit) {
        if (busy) return
        busy = true
        busyLabel = label
        message = null
        errorResult = null
        manager.backgroundScope.launch {
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
            // Read device specs so we can recommend a GGUF that actually fits
            // this phone instead of asking the user to guess. Total RAM drives
            // which parameter count is realistic; free storage gates downloads.
            //
            // Wrapped in runCatching as a safety net: ActivityManager /
            // StatFs calls *shouldn't* throw on any modern Android, but if
            // they ever do on a quirky device the whole Sandbox tab would
            // crash on entry. Falling back to a static catalog keeps the
            // download UI usable.
            //
            // TODO(phase 2 hardware rec): factor in CPU core count + arch
            // (some quants are aarch64-only, some run faster with NEON).
            // TODO(phase 2 hardware rec): warm-state probe — check actual
            // free RAM instead of total, since OS + MVE + sandbox already
            // claim ~1 GB at idle.
            val context = LocalContext.current
            val deviceProfile = remember(context) {
                runCatching { readDeviceProfile(context) }.getOrDefault(
                    DeviceProfile(totalRamBytes = 0L, freeStorageBytes = 0L),
                )
            }
            val pick = remember(deviceProfile) {
                runCatching { pickRecommendedModel(deviceProfile) }.getOrDefault(
                    Recommendation(
                        recommended = ALL_QUICK_INSTALLS[2], // 3B default
                        alternatives = ALL_QUICK_INSTALLS.filterIndexed { i, _ -> i != 2 },
                        warning = null,
                    ),
                )
            }
            val specsKnown = deviceProfile.totalRamBytes > 0L

            if (specsKnown) {
                Text(
                    text = "Your phone — ${formatGb(deviceProfile.totalRamBytes)} RAM, ${formatGb(deviceProfile.freeStorageBytes)} free storage",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
            }
            Text(
                text = "The MVE Assistant — your default on-device AI. Mobile-friendly, snappy, decent at code + reasoning, doesn't reflexively refuse. Pick this if you don't know which to pick.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            // Default MVE Assistant — opinionated single button. Dolphin3 tuning
            // emphasizes helpfulness over refusals; 3B fits ~95% of modern
            // phones at Q4_K_M; works on the bundled llama.cpp build.
            val defaultFits = pick.alternatives.any { it.repoId == MVE_DEFAULT_MODEL.repoId } ||
                pick.recommended.repoId == MVE_DEFAULT_MODEL.repoId ||
                deviceProfile.totalRamBytes == 0L ||
                MVE_DEFAULT_MODEL.approxRamBytes <= (deviceProfile.totalRamBytes / 1.6).toLong()

            Button(
                onClick = { repoInput = MVE_DEFAULT_MODEL.repoId },
                modifier = Modifier.fillMaxWidth().handCursor(),
            ) {
                Text(
                    text = "Install MVE Assistant\n${MVE_DEFAULT_MODEL.label} • ${MVE_DEFAULT_MODEL.sizeLabel}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (!defaultFits) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "⚠️ Your device may be tight on RAM for the default. Consider a smaller pick below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(12.dp))

            // Power-user fallback: hardware-aware size buckets, slightly
            // de-emphasized. Most users should just tap the MVE Assistant
            // above; this row is for "I want something specific."
            if (specsKnown) {
                Text(
                    text = "Or pick a different size — your phone: ${formatGb(deviceProfile.totalRamBytes)} RAM, ${formatGb(deviceProfile.freeStorageBytes)} free",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = "Or pick a different size:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(4.dp))

            // Hardware-aware primary recommendation as one of the alternatives.
            // Less prominent than the MVE default but called out if it differs.
            if (pick.recommended.repoId != MVE_DEFAULT_MODEL.repoId) {
                OutlinedButton(
                    onClick = { repoInput = pick.recommended.repoId },
                    modifier = Modifier.fillMaxWidth().handCursor(),
                ) {
                    Text(
                        text = "Best for your hardware: ${pick.recommended.label} • ${pick.recommended.sizeLabel}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (pick.warning != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = pick.warning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                pick.alternatives.forEach { alt ->
                    OutlinedButton(
                        onClick = { repoInput = alt.repoId },
                        modifier = Modifier.weight(1f).handCursor(),
                    ) {
                        Text(
                            text = "${alt.label}\n${alt.sizeLabel}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
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
                Spacer(Modifier.height(2.dp))
                // Distinguishes this card's Run/Stop control from the Services
                // tab's Show-in-chat toggle. Run = the llama.cpp engine for
                // THIS model is loaded and serving; Show-in-chat = whether the
                // service entry is visible in the chat AI picker. Both need to
                // be on to chat with this model.
                Text(
                    text = "Run loads this model into the llama.cpp engine. Use in chat registers it as a service so it shows up in the chat AI picker. Separate from the LiteRT on-device option in Services.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
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
                                        if (r.ok) message = "Running. Tap \"Use in chat\" below." else errorResult = r
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
                        message = "Connected. Open Services and toggle \"Show in chat\" on the new entry to use it."
                        scope.launch {
                            runCatching { dataRepository.validateConnection(Service.OpenAICompatible, instance.instanceId) }
                        }
                    },
                    modifier = Modifier.handCursor(),
                ) { Text("Use in chat") }
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

// ────────────────────────────────────────────────────────────────────────
// Hardware-aware model recommendation
//
// Instead of asking the user to guess between Tiny / Recommended / Big, scan
// the device's total RAM and free storage and surface the model that's most
// likely to actually run well. Phone CPUs vary wildly; RAM is the ceiling
// that gates which parameter count is even loadable.
//
// Heuristic uses RAM only — storage is just a sanity check against models
// that wouldn't fit at all. The "fits in RAM" rule of thumb for Q4_K_M
// GGUF: model file size + ~30% headroom for KV cache & context window.
// ────────────────────────────────────────────────────────────────────────

private data class DeviceProfile(
    val totalRamBytes: Long,
    val freeStorageBytes: Long,
)

private data class ModelOption(
    val repoId: String,
    val label: String,
    val sizeLabel: String,
    val approxDownloadBytes: Long,
    val approxRamBytes: Long,
    val tagline: String,
)

private data class Recommendation(
    val recommended: ModelOption,
    val alternatives: List<ModelOption>,
    val warning: String?,
)

/**
 * The opinionated MVE default. New users see this as the primary "just give
 * me a working AI" button; the sized alternatives below are for power users
 * who want to override.
 *
 * Selection criteria:
 *   - 3B parameter count: fits ~95% of modern phones at Q4_K_M (~2 GB),
 *     usable speed on phone CPU, decent reasoning + code quality.
 *   - Dolphin tuning: emphasizes helpfulness over reflexive refusals
 *     (the user's explicit ask — "not a super 'no I can't do that' type").
 *   - Llama 3.2 base: strong general capability, well-supported in
 *     llama.cpp, no known load-time issues on aarch64-musl builds.
 *
 * Update with care — this is the model new users will encounter as
 * "MVE's AI" without any other context. It needs to actually work.
 *
 * TODO(future): consider a Coder-specialized variant (Qwen2.5-Coder-3B-
 * abliterated) once we verify it loads cleanly. Coder models have better
 * code quality but more aggressive refusal training to defeat.
 */
private val MVE_DEFAULT_MODEL = ModelOption(
    repoId = "cognitivecomputations/Dolphin3.0-Llama3.2-3B-GGUF",
    label = "Dolphin3 Llama3.2 3B",
    sizeLabel = "~2 GB",
    approxDownloadBytes = 2L * 1024 * 1024 * 1024,
    approxRamBytes = 3L * 1024 * 1024 * 1024,
    tagline = "Default MVE Assistant — permissive, code + reasoning",
)

private val ALL_QUICK_INSTALLS = listOf(
    ModelOption(
        repoId = "bartowski/Qwen2.5-0.5B-Instruct-GGUF",
        label = "Qwen2.5 0.5B",
        sizeLabel = "~400 MB",
        approxDownloadBytes = 400L * 1024 * 1024,
        approxRamBytes = 600L * 1024 * 1024,
        tagline = "Smallest, fastest, low quality",
    ),
    ModelOption(
        repoId = "bartowski/Qwen2.5-1.5B-Instruct-GGUF",
        label = "Qwen2.5 1.5B",
        sizeLabel = "~1 GB",
        approxDownloadBytes = 1L * 1024 * 1024 * 1024,
        approxRamBytes = 1500L * 1024 * 1024,
        tagline = "Compact but usable",
    ),
    ModelOption(
        repoId = "bartowski/Qwen2.5-3B-Instruct-GGUF",
        label = "Qwen2.5 3B",
        sizeLabel = "~2 GB",
        approxDownloadBytes = 2L * 1024 * 1024 * 1024,
        approxRamBytes = 3L * 1024 * 1024 * 1024,
        tagline = "Sweet spot for most phones",
    ),
    ModelOption(
        repoId = "bartowski/Qwen2.5-7B-Instruct-GGUF",
        label = "Qwen2.5 7B",
        sizeLabel = "~4.5 GB",
        approxDownloadBytes = 4500L * 1024 * 1024,
        approxRamBytes = 6L * 1024 * 1024 * 1024,
        tagline = "High-end phones only",
    ),
)

private fun readDeviceProfile(context: Context): DeviceProfile {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val totalRam = if (activityManager != null) {
        val info = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(info)
        info.totalMem
    } else {
        Runtime.getRuntime().maxMemory()
    }
    val filesDir = context.filesDir
    val freeStorage = runCatching {
        val statFs = StatFs(filesDir.absolutePath)
        statFs.availableBytes
    }.getOrDefault(0L)
    return DeviceProfile(totalRamBytes = totalRam, freeStorageBytes = freeStorage)
}

private fun pickRecommendedModel(profile: DeviceProfile): Recommendation {
    // Conservative rule: a model is "comfortable" if total RAM is at least
    // 1.6x the model's expected runtime footprint (allows the OS, MVE, and
    // the proot sandbox to coexist without thrashing).
    val ramHeadroom = (profile.totalRamBytes / 1.6).toLong()
    val storageHeadroom = profile.freeStorageBytes - (500L * 1024 * 1024) // keep 500 MB for OS overhead

    val viable = ALL_QUICK_INSTALLS.filter {
        it.approxRamBytes <= ramHeadroom && it.approxDownloadBytes <= storageHeadroom
    }

    return when {
        viable.isEmpty() -> {
            // Nothing fits — recommend the smallest and warn explicitly.
            val smallest = ALL_QUICK_INSTALLS.first()
            Recommendation(
                recommended = smallest,
                alternatives = ALL_QUICK_INSTALLS.drop(1).take(2),
                warning = "Your device may be tight on resources for any local model. Cloud services in Settings → Services may be a better fit.",
            )
        }
        else -> {
            // Pick the LARGEST viable option (better quality), but keep the
            // others as alternatives for users who prioritize speed/storage.
            val pick = viable.last()
            val others = ALL_QUICK_INSTALLS.filter { it.repoId != pick.repoId }.take(3)
            Recommendation(
                recommended = pick,
                alternatives = others,
                warning = null,
            )
        }
    }
}

private fun formatGb(bytes: Long): String {
    if (bytes <= 0) return "?"
    val gb = bytes / (1024.0 * 1024.0 * 1024.0)
    return if (gb >= 10) "${gb.toLong()} GB" else "${(gb * 10).toLong() / 10.0} GB"
}
