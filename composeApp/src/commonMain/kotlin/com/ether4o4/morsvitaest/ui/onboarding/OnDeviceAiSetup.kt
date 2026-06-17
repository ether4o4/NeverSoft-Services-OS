package com.ether4o4.morsvitaest.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ether4o4.morsvitaest.data.AppSettings
import com.ether4o4.morsvitaest.data.DataRepository
import com.ether4o4.morsvitaest.data.Service
import com.ether4o4.morsvitaest.inference.DevicePerformance
import com.ether4o4.morsvitaest.inference.DownloadError
import com.ether4o4.morsvitaest.inference.LocalModel
import com.ether4o4.morsvitaest.inference.recommendOnDeviceModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.compose.koinInject

private val Accent = Color(0xFF00D4FF)

private fun formatSize(bytes: Long): String {
    val gb = bytes.toDouble() / 1_000_000_000.0
    return if (gb >= 1.0) "${(gb * 10).toInt() / 10.0} GB" else "${bytes / 1_000_000} MB"
}

private fun perfLabel(p: DevicePerformance): String = when (p) {
    DevicePerformance.GOOD -> "Runs well on your device"
    DevicePerformance.OK -> "Should run on your device"
    DevicePerformance.POOR -> "May be slow or fail on your device"
}

/** Registers LiteRT with [modelId] selected and makes it the primary chat service. */
private fun enableOnDevice(repo: DataRepository, modelId: String) {
    val existing = repo.getConfiguredServiceInstances().firstOrNull { it.serviceId == Service.LiteRT.id }
    val instance = existing ?: repo.addConfiguredService(Service.LiteRT.id)
    repo.setInstanceEnabled(instance.instanceId, true)
    repo.updateInstanceSelectedModel(instance.instanceId, Service.LiteRT, modelId)
    val order = listOf(instance.instanceId) +
        repo.getConfiguredServiceInstances().map { it.instanceId }.filterNot { it == instance.instanceId }
    repo.reorderConfiguredServices(order)
    repo.setFreeServicePrimary(false)
}

/**
 * Guided, capability-aware "set up on-device AI" prompt. Detects the device's RAM and
 * recommends the right path — the full Gemma model (proficient tool calling) on capable
 * phones, the tiny Qwen on mid devices, or the hosted Free (cloud) model on weak ones —
 * downloads the chosen model with a progress bar, then enables it as the primary chat
 * service so on-device chat works with zero terminal.
 */
@Composable
fun OnDeviceAiSetup(onClose: () -> Unit) {
    val repo = koinInject<DataRepository>()
    val settings = koinInject<AppSettings>()

    val totalMem = remember { repo.getTotalDeviceMemoryBytes() }
    val freeSpace = remember { repo.getLocalFreeSpaceBytes() }
    val rec = remember { recommendOnDeviceModel(totalMem) }
    val catalog = remember { repo.getLocalAvailableModels() }
    val gemma = remember { catalog.firstOrNull { it.id == "gemma-4-e2b-it" } }
    val qwen = remember { catalog.firstOrNull { it.id == "qwen3-0.6b" } }

    val downloadingId by remember { repo.getLocalDownloadingModelId() ?: MutableStateFlow<String?>(null) }.collectAsState()
    val progress by remember { repo.getLocalDownloadProgress() ?: MutableStateFlow<Float?>(null) }.collectAsState()
    val downloadError by remember { repo.getLocalDownloadError() ?: MutableStateFlow<DownloadError?>(null) }.collectAsState()

    var startedModel by remember { mutableStateOf<LocalModel?>(null) }
    var sawActive by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf<DownloadError?>(null) }

    LaunchedEffect(downloadingId, downloadError) {
        val started = startedModel ?: return@LaunchedEffect
        if (downloadError != null) {
            failed = downloadError
            return@LaunchedEffect
        }
        if (downloadingId == started.id) sawActive = true
        if (sawActive && downloadingId == null && !done) {
            if (repo.getLocalDownloadedModels().any { it.id == started.id }) {
                done = true
            } else {
                failed = DownloadError.DOWNLOAD_INCOMPLETE
            }
        }
    }

    fun choose(model: LocalModel) {
        failed = null
        // Configure LiteRT now (not on completion) so that even if the user closes this
        // screen mid-download, the model auto-becomes the primary chat service the moment
        // its file finishes downloading — see ChatViewModel's hasUsableOnDeviceModel.
        enableOnDevice(repo, model.id)
        settings.markOnDeviceAiOffered()
        if (repo.getLocalDownloadedModels().any { it.id == model.id }) {
            done = true
            return
        }
        startedModel = model
        sawActive = false
        repo.startLocalModelDownload(model)
    }

    fun useCloud() {
        repo.setFreeServicePrimary(true)
        settings.markOnDeviceAiOffered()
        onClose()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xF2090B10)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .heightIn(max = 720.dp)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when {
                done -> {
                    Text("✓ On-device AI ready", color = Accent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Your assistant now runs on this phone — offline, private, with tool calling. " +
                            "You can change it any time in Settings → Services.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(22.dp))
                    Button(onClick = onClose) { Text("Start chatting") }
                }

                startedModel != null && failed == null -> {
                    Text("Downloading ${startedModel?.displayName}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    val p = progress ?: 0f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color.White.copy(alpha = 0.12f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(p.coerceIn(0f, 1f))
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(Accent),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("${(p.coerceIn(0f, 1f) * 100).toInt()}%", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "You can close this — it keeps downloading in the background and turns on automatically.",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            repo.cancelLocalModelDownload()
                            startedModel = null
                            sawActive = false
                        }) { Text("Cancel") }
                        TextButton(onClick = onClose) { Text("Hide") }
                    }
                }

                else -> {
                    Text("Set up on-device AI", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Run the assistant directly on your phone — works offline, and nothing leaves the device. " +
                            "No terminal needed.",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
                    val deviceNote = when (rec.recommendedModelId) {
                        "gemma-4-e2b-it" -> "Your device can run the full model with proper tool calling."
                        "qwen3-0.6b" -> "Your device is best suited to the lighter model (basic tools)."
                        else -> "Your device is best suited to cloud AI — a local model would be too slow here."
                    }
                    Text(deviceNote, color = Accent.copy(alpha = 0.9f), fontSize = 13.sp, textAlign = TextAlign.Center)
                    if (failed != null) {
                        Spacer(Modifier.height(8.dp))
                        val msg = when (failed) {
                            DownloadError.NOT_ENOUGH_DISK_SPACE -> "Not enough storage for that model."
                            DownloadError.NETWORK_ERROR -> "Download failed (network). Try again on stable wifi."
                            else -> "Download didn't complete. Try again."
                        }
                        Text(msg, color = Color(0xFFE2557A), fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(18.dp))

                    gemma?.let {
                        ModelCard(
                            model = it,
                            tagline = "Best tool calling · ${perfLabel(rec.gemmaPerformance)}",
                            recommended = rec.recommendedModelId == it.id,
                            enoughSpace = it.sizeBytes < freeSpace,
                            onPick = { choose(it) },
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    qwen?.let {
                        ModelCard(
                            model = it,
                            tagline = "Fast & small · basic tools · ${perfLabel(rec.qwenPerformance)}",
                            recommended = rec.recommendedModelId == it.id,
                            enoughSpace = it.sizeBytes < freeSpace,
                            onPick = { choose(it) },
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    TextButton(onClick = { useCloud() }) {
                        Text("Use cloud AI instead (no download)", color = Color.White.copy(alpha = 0.85f))
                    }
                    TextButton(onClick = {
                        settings.markOnDeviceAiOffered()
                        onClose()
                    }) { Text("Maybe later", color = Color.White.copy(alpha = 0.5f)) }
                }
            }
        }
    }
}

@Composable
private fun ModelCard(
    model: LocalModel,
    tagline: String,
    recommended: Boolean,
    enoughSpace: Boolean,
    onPick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = if (recommended) 0.10f else 0.05f))
            .border(
                width = if (recommended) 1.5.dp else 1.dp,
                color = if (recommended) Accent else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(model.displayName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(formatSize(model.sizeBytes), color = Color.White.copy(alpha = 0.55f), fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            if (recommended) {
                Text(
                    "Recommended",
                    color = Color(0xFF090B10),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Accent).padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(tagline, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        if (enoughSpace) {
            Button(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
                Text(if (recommended) "Download & enable" else "Download this instead")
            }
        } else {
            Text("Not enough free storage for this model.", color = Color(0xFFE2557A), fontSize = 12.sp)
        }
    }
}
