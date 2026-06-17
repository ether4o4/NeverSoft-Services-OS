package com.ether4o4.morsvitaest.inference

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data class LocalModel(
    val id: String,
    val displayName: String,
    val fileName: String,
    val sizeBytes: Long,
    val downloadUrl: String,
    val gpuMemoryMb: Int,
    val defaultContextTokens: Int,
    val maxContextTokens: Int,
    val kvPerTokenBytes: Int,
    val isRecommended: Boolean = false,
)

enum class DevicePerformance {
    GOOD,
    OK,
    POOR,
}

fun estimateGpuMemoryMb(model: LocalModel, contextTokens: Int): Int {
    val modelFileMb = (model.sizeBytes / (1024 * 1024)).toInt()
    val extraTokens = contextTokens - model.defaultContextTokens
    val extraMemoryMb = (extraTokens.toLong() * model.kvPerTokenBytes) / (1024 * 1024)
    return modelFileMb + model.gpuMemoryMb + extraMemoryMb.toInt()
}

fun calculateDevicePerformance(totalMemoryBytes: Long, estimatedGpuMemoryMb: Int): DevicePerformance {
    val gpuMemoryBytes = estimatedGpuMemoryMb.toLong() * 1024 * 1024
    val ratio = totalMemoryBytes.toDouble() / gpuMemoryBytes
    return when {
        ratio >= 2.5 -> DevicePerformance.GOOD
        ratio >= 1.85 -> DevicePerformance.OK
        else -> DevicePerformance.POOR
    }
}

data class DownloadedModel(
    val id: String,
    val displayName: String,
    val filePath: String,
    val sizeBytes: Long,
)

enum class EngineState {
    UNINITIALIZED,
    INITIALIZING,
    READY,
    ERROR,
}

data class InferenceMessage(
    val role: String,
    val content: String,
)

/**
 * A tool definition handed to the on-device inference engine.
 *
 * @param name the tool's identifier as the model will see it
 * @param descriptionJsonString a complete OpenAPI/OpenAI-style JSON object describing the
 *        tool, e.g. `{"name":"get_time","description":"...","parameters":{"type":"object",...}}`
 * @param execute receives the JSON arguments object as a string and returns the
 *        JSON-encoded result string
 */
data class LocalTool(
    val name: String,
    val descriptionJsonString: String,
    val execute: suspend (jsonArgs: String) -> String,
)

class InsufficientMemoryException : Exception()
class InferenceTimeoutException : Exception()
class NoModelDownloadedException : Exception()

enum class DownloadError {
    NOT_ENOUGH_DISK_SPACE,
    NETWORK_ERROR,
    DOWNLOAD_INCOMPLETE,
}

/**
 * What the guided "set up on-device AI" flow should recommend for a device with
 * [totalMemoryBytes] of RAM. [recommendedModelId] null means the device is too weak for
 * any local model and should use the hosted Free (cloud) model instead.
 */
data class OnDeviceRecommendation(
    val recommendedModelId: String?,
    val gemmaPerformance: DevicePerformance,
    val qwenPerformance: DevicePerformance,
)

/**
 * Picks the most capable model the device can run acceptably: Gemma E2B (proficient tool
 * calling) where it's GOOD/OK, otherwise the tiny Qwen for basic chat, otherwise null
 * ("use cloud"). Tool calling needs a real model, so Gemma is preferred wherever it fits.
 */
fun recommendOnDeviceModel(totalMemoryBytes: Long): OnDeviceRecommendation {
    val gemma = MODEL_CATALOG.first { it.id == "gemma-4-e2b-it" }
    val qwen = MODEL_CATALOG.first { it.id == "qwen3-0.6b" }
    val gemmaPerf = calculateDevicePerformance(totalMemoryBytes, estimateGpuMemoryMb(gemma, gemma.defaultContextTokens))
    val qwenPerf = calculateDevicePerformance(totalMemoryBytes, estimateGpuMemoryMb(qwen, qwen.defaultContextTokens))
    val recommended = when {
        gemmaPerf != DevicePerformance.POOR -> gemma.id
        qwenPerf != DevicePerformance.POOR -> qwen.id
        else -> null
    }
    return OnDeviceRecommendation(recommended, gemmaPerf, qwenPerf)
}

interface LocalInferenceEngine {
    val engineState: StateFlow<EngineState>
    val downloadingModelId: StateFlow<String?>
    val downloadProgress: StateFlow<Float?>
    val downloadError: StateFlow<DownloadError?>

    val currentModelId: String?

    suspend fun initialize(model: DownloadedModel, contextTokens: Int = 0)
    suspend fun release()

    /**
     * Fire-and-forget release, run on the engine's own coroutine scope. Called from
     * non-suspend contexts (e.g. Settings UI when the user picks a different model) so
     * the GPU driver has time to reclaim memory before the next inference.
     */
    fun releaseInBackground()

    suspend fun chat(
        messages: List<InferenceMessage>,
        systemPrompt: String?,
        tools: List<LocalTool> = emptyList(),
    ): String

    fun getDownloadedModels(): List<DownloadedModel>
    fun getAvailableModels(): List<LocalModel>
    fun getFreeSpaceBytes(): Long
    fun startDownload(model: LocalModel)
    fun cancelDownload()
    suspend fun deleteModel(modelId: String)
}
