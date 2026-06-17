package com.ether4o4.morsvitaest.inference

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.OpenApiTool
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.tool
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.time.Duration.Companion.milliseconds

class LiteRTInferenceEngine : LocalInferenceEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadJob: Job? = null
    private var idleReleaseJob: Job? = null

    private var engine: Engine? = null
    private var conversation: com.google.ai.edge.litertlm.Conversation? = null
    override var currentModelId: String? = null
        private set
    private var currentContextTokens: Int = 0

    private val _engineState = MutableStateFlow(EngineState.UNINITIALIZED)
    override val engineState: StateFlow<EngineState> = _engineState

    private val _downloadingModelId = MutableStateFlow<String?>(null)
    override val downloadingModelId: StateFlow<String?> = _downloadingModelId

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    override val downloadProgress: StateFlow<Float?> = _downloadProgress

    private val _downloadError = MutableStateFlow<DownloadError?>(null)
    override val downloadError: StateFlow<DownloadError?> = _downloadError

    override suspend fun initialize(model: DownloadedModel, contextTokens: Int) {
        withContext(Dispatchers.IO) {
            idleReleaseJob?.cancel()
            if (currentModelId == model.id && currentContextTokens == contextTokens && _engineState.value == EngineState.READY) return@withContext
            _engineState.value = EngineState.INITIALIZING
            try {
                val modelFile = File(model.filePath)
                if (!modelFile.exists() || modelFile.length() < 1_000_000) {
                    throw IllegalStateException("Model file missing or too small: ${model.filePath}")
                }

                // Release any currently-loaded engine before measuring available memory,
                // otherwise its GPU/CPU working set counts against the headroom check and
                // switching between models spuriously fails (e.g. Qwen -> Gemma 4).
                val hadExistingEngine = engine != null
                release()
                _engineState.value = EngineState.INITIALIZING

                if (hadExistingEngine) {
                    // engine.close() returns before the OpenCL driver actually reclaims the
                    // previous model's GPU buffers, so loading a second model on top would
                    // briefly hold both resident and trip Android's LMK. Give the driver a
                    // beat to drain before allocating ~GB of new GPU buffers.
                    System.gc()
                    delay(GPU_DRAIN_DELAY_MS.milliseconds)
                }

                val availMem = getAvailableMemoryBytes()
                if (availMem < MIN_MEMORY_HEADROOM_BYTES) {
                    throw InsufficientMemoryException()
                }

                fun initWithBackend(backend: Backend, maxTokens: Int?): Engine {
                    val config = EngineConfig(
                        modelPath = model.filePath,
                        backend = backend,
                        cacheDir = getModelCacheDirectory(),
                        maxNumTokens = maxTokens,
                    )
                    val e = Engine(config)
                    e.initialize()
                    return e
                }

                val requestedTokens = if (contextTokens > 0) contextTokens else null
                println("LiteRT: initializing model=${model.id} maxNumTokens=$requestedTokens")

                val newEngine = try {
                    try {
                        initWithBackend(Backend.GPU(), requestedTokens)
                    } catch (e: Exception) {
                        initWithBackend(Backend.CPU(), requestedTokens)
                    }
                } catch (e: Exception) {
                    // Context size not supported — retry with model default
                    println("LiteRT: init failed with maxNumTokens=$requestedTokens, falling back to default: ${e.message}")
                    if (requestedTokens != null) {
                        try {
                            initWithBackend(Backend.GPU(), null)
                        } catch (e2: Exception) {
                            initWithBackend(Backend.CPU(), null)
                        }
                    } else {
                        throw e
                    }
                }

                engine = newEngine
                conversation = newEngine.createConversation()
                currentModelId = model.id
                currentContextTokens = contextTokens
                _engineState.value = EngineState.READY
            } catch (e: Exception) {
                _engineState.value = EngineState.ERROR
                throw e
            }
        }
    }

    override suspend fun release() {
        withContext(Dispatchers.IO) {
            // Null before close so a concurrent release() sees null and skips —
            // Conversation.close() / Engine.close() throw IllegalStateException on double-close.
            val convToClose = conversation
            val engineToClose = engine
            conversation = null
            engine = null
            currentModelId = null
            _engineState.value = EngineState.UNINITIALIZED
            runCatching { convToClose?.close() }
            runCatching { engineToClose?.close() }
        }
    }

    override fun releaseInBackground() {
        idleReleaseJob?.cancel()
        idleReleaseJob = scope.launch { release() }
    }

    override suspend fun chat(
        messages: List<InferenceMessage>,
        systemPrompt: String?,
        tools: List<LocalTool>,
    ): String = withContext(Dispatchers.IO) {
        idleReleaseJob?.cancel()
        try {
            val currentEngine = engine ?: throw IllegalStateException("Engine not initialized")

            val lastUserIndex = messages.indexOfLast { it.role == "user" }
            if (lastUserIndex < 0) throw IllegalStateException("No user message found")

            val sanitizedSystemPrompt = sanitizeForLiteRt(systemPrompt)
            val initialMessages = messages.subList(0, lastUserIndex).map { msg ->
                val sanitized = sanitizeForLiteRt(msg.content) ?: ""
                when (msg.role) {
                    "user" -> Message.user(sanitized)
                    else -> Message.model(sanitized)
                }
            }

            val toolProviders = tools.map { tool(LocalToolOpenApiAdapter(it)) }
            val config = ConversationConfig(
                systemInstruction = sanitizedSystemPrompt?.let { Contents.of(it) },
                initialMessages = initialMessages,
                tools = toolProviders,
                // Decoding tuned per the lowest_refusal LiteRT profile: direct + confident
                // without going incoherent (see profiles/lowest_refusal/).
                samplerConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 0.72),
                // automaticToolCalling = true drives the parser; only enable when we
                // actually have tools, otherwise plain-text responses get parsed as FCs.
                automaticToolCalling = toolProviders.isNotEmpty(),
            )
            val prev = conversation
            conversation = null
            runCatching { prev?.close() }
            val conv = currentEngine.createConversation(config)
            conversation = conv

            val lastMessage = sanitizeForLiteRt(messages[lastUserIndex].content) ?: ""
            val response = try {
                withTimeout(INFERENCE_TIMEOUT_MS.milliseconds) {
                    conv.sendMessage(lastMessage)
                }
            } catch (e: TimeoutCancellationException) {
                throw InferenceTimeoutException()
            }
            stripThinkBlocks(response.toString())
        } finally {
            scheduleIdleRelease()
        }
    }

    /**
     * Adapter that exposes a MorsVitaEst [LocalTool] (suspend execute) to litert-lm's [OpenApiTool]
     * (synchronous execute). The bridge uses [runBlocking] because the engine calls
     * [execute] on its own worker thread (we're already inside `Dispatchers.IO` from
     * [chat]) and waits for the result before continuing the tool loop.
     */
    private class LocalToolOpenApiAdapter(private val localTool: LocalTool) : OpenApiTool {
        override fun getToolDescriptionJsonString(): String = localTool.descriptionJsonString
        override fun execute(paramsJsonString: String): String = runBlocking { localTool.execute(paramsJsonString) }
    }

    private fun scheduleIdleRelease() {
        idleReleaseJob?.cancel()
        idleReleaseJob = scope.launch {
            delay(IDLE_RELEASE_MS.milliseconds)
            release()
        }
    }

    companion object {
        private const val IDLE_RELEASE_MS = 5L * 60 * 1000 // 5 minutes
        private const val INFERENCE_TIMEOUT_MS = 120_000L // 2 minutes
        private const val MIN_MEMORY_HEADROOM_BYTES = 512L * 1024 * 1024 // 512 MB
        private const val DOWNLOAD_SPACE_BUFFER_BYTES = 500L * 1024 * 1024 // 500 MB
        private const val GPU_DRAIN_DELAY_MS = 750L
        private const val MAX_DOWNLOAD_ATTEMPTS = 4

        // Exponential backoff between resume attempts: 1s, 2s, 4s, 8s … capped at 15s.
        private fun downloadBackoffMs(attempt: Int): Long = (1000L shl (attempt - 1).coerceIn(0, 5)).coerceAtMost(15_000L)
    }

    override fun getDownloadedModels(): List<DownloadedModel> {
        val modelsDir = File(getModelStorageDirectory())
        if (!modelsDir.exists()) return emptyList()
        return getAvailableModels().mapNotNull { model ->
            val modelFile = File(File(modelsDir, model.id), model.fileName)
            if (modelFile.exists()) {
                DownloadedModel(
                    id = model.id,
                    displayName = model.displayName,
                    filePath = modelFile.absolutePath,
                    sizeBytes = modelFile.length(),
                )
            } else {
                null
            }
        }
    }

    private val customModelsJson = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private fun customModelsFile() = File(getModelStorageDirectory(), "custom_models.json")

    private fun loadCustomModels(): List<LocalModel> = runCatching {
        val file = customModelsFile()
        if (!file.exists()) return emptyList()
        customModelsJson.decodeFromString(ListSerializer(LocalModel.serializer()), file.readText())
    }.getOrDefault(emptyList())

    private fun saveCustomModels(models: List<LocalModel>) {
        runCatching {
            val file = customModelsFile()
            file.parentFile?.mkdirs()
            file.writeText(customModelsJson.encodeToString(ListSerializer(LocalModel.serializer()), models))
        }
    }

    /** Persist a user-supplied (non-catalog) model so it survives restarts and appears in the model lists. */
    private fun persistCustomModel(model: LocalModel) {
        if (MODEL_CATALOG.any { it.id == model.id }) return
        val current = loadCustomModels()
        if (current.any { it.id == model.id }) return
        saveCustomModels(current + model)
    }

    // Catalog models plus any user-installed (e.g. installed-from-URL) custom models.
    override fun getAvailableModels(): List<LocalModel> = MODEL_CATALOG + loadCustomModels()

    override fun getFreeSpaceBytes(): Long = getAvailableDiskSpaceBytes(getModelStorageDirectory())

    override fun startDownload(model: LocalModel) {
        cancelDownload()
        // A model that isn't in the built-in catalog (e.g. installed from a URL) is recorded
        // so it survives restarts and shows up in getAvailableModels()/getDownloadedModels().
        persistCustomModel(model)
        downloadJob = scope.launch {
            _downloadingModelId.value = model.id
            _downloadProgress.value = 0f
            _downloadError.value = null
            var tempFile: File? = null
            var notificationStarted = false

            try {
                val modelsDir = getModelStorageDirectory()
                val modelDir = File(modelsDir, model.id)
                modelDir.mkdirs()
                val targetFile = File(modelDir, model.fileName)
                tempFile = File(modelDir, "${model.fileName}.tmp")
                var lastNotifiedPercent = -1

                // Resume-aware, retry-with-backoff download. The partial file is kept across
                // failures so an interrupted multi-GB pull continues from where it stopped
                // instead of restarting at byte 0. Only an explicit cancel (or a successful
                // finalize) discards it.
                val expectedTotal = model.sizeBytes
                var success = false
                var attempt = 0

                while (!success && attempt < MAX_DOWNLOAD_ATTEMPTS) {
                    attempt++
                    ensureActive()

                    val existingBytes = if (tempFile.exists()) tempFile.length() else 0L
                    val remaining = (expectedTotal - existingBytes).coerceAtLeast(0L)
                    if (getFreeSpaceBytes() < remaining + DOWNLOAD_SPACE_BUFFER_BYTES) {
                        _downloadError.value = DownloadError.NOT_ENOUGH_DISK_SPACE
                        return@launch
                    }

                    try {
                        @Suppress("DEPRECATION")
                        val connection = URL(model.downloadUrl).openConnection() as HttpURLConnection
                        connection.instanceFollowRedirects = true
                        connection.connectTimeout = 30_000
                        connection.readTimeout = 60_000
                        if (existingBytes > 0) {
                            connection.setRequestProperty("Range", "bytes=$existingBytes-")
                        }
                        connection.connect()

                        val responseCode = connection.responseCode
                        // 416 Range Not Satisfiable: our partial is already at/over the full
                        // size. Accept it if it checks out, otherwise drop it and refetch.
                        if (responseCode == 416) {
                            connection.disconnect()
                            if (existingBytes >= expectedTotal * 0.95) {
                                success = true
                            } else {
                                tempFile.delete()
                            }
                            continue
                        }
                        if (responseCode !in 200..299) {
                            connection.disconnect()
                            throw IOException("Download failed: HTTP $responseCode")
                        }

                        // 206 => server honored the range, append to the partial. 200 => server
                        // ignored the range (or it's a fresh start), so truncate and restart.
                        val resuming = responseCode == 206
                        if (!resuming && tempFile.exists()) tempFile.delete()

                        // Only start the foreground service once we have a live connection.
                        // Starting it earlier risks ForegroundServiceDidNotStartInTimeException
                        // if connect() fails fast (e.g. offline) before the service can run.
                        if (!notificationStarted) {
                            startDownloadNotificationService()
                            notificationStarted = true
                        }

                        val total = (
                            if (resuming) {
                                existingBytes + connection.contentLengthLong.coerceAtLeast(0L)
                            } else {
                                connection.contentLengthLong.takeIf { it > 0 } ?: expectedTotal
                            }
                            ).coerceAtLeast(1L)

                        val buffer = ByteArray(65536)
                        var totalBytesRead = if (resuming) existingBytes else 0L

                        connection.inputStream.use { input ->
                            FileOutputStream(tempFile, resuming).use { output ->
                                while (true) {
                                    ensureActive()
                                    val bytesRead = input.read(buffer)
                                    if (bytesRead <= 0) break
                                    output.write(buffer, 0, bytesRead)
                                    totalBytesRead += bytesRead
                                    val percent = (totalBytesRead * 100 / total).toInt().coerceIn(1, 100)
                                    if (percent != lastNotifiedPercent) {
                                        lastNotifiedPercent = percent
                                        _downloadProgress.value = percent / 100f
                                        updateDownloadNotificationProgress(percent)
                                    }
                                }
                            }
                        }
                        connection.disconnect()

                        if (tempFile.length() >= total * 0.95) {
                            success = true
                        } else if (attempt < MAX_DOWNLOAD_ATTEMPTS) {
                            // Stream ended early (server/proxy closed mid-transfer). Keep the
                            // partial and resume after a short backoff.
                            delay(downloadBackoffMs(attempt))
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: IOException) {
                        // Transient network failure — keep the partial for resume. Surface the
                        // error only once every attempt is exhausted.
                        if (attempt >= MAX_DOWNLOAD_ATTEMPTS) throw e
                        delay(downloadBackoffMs(attempt))
                    }
                }

                if (!success) {
                    throw IOException("Download did not complete after $MAX_DOWNLOAD_ATTEMPTS attempts")
                }

                if (!tempFile.renameTo(targetFile)) {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                }
            } catch (e: CancellationException) {
                // Explicit cancel discards the partial so it doesn't linger or occupy space.
                if (tempFile?.exists() == true) tempFile.delete()
                throw e
            } catch (e: Throwable) {
                // Keep tempFile (if any) so the next startDownload() resumes from where we
                // stopped rather than re-pulling gigabytes.
                _downloadError.value = DownloadError.NETWORK_ERROR
            } finally {
                _downloadingModelId.value = null
                _downloadProgress.value = null
                if (notificationStarted) stopDownloadNotificationService()
            }
        }
    }

    override fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
    }

    override suspend fun deleteModel(modelId: String) {
        withContext(Dispatchers.IO) {
            // Wait for any in-flight idle release so its native teardown doesn't race with deleteRecursively().
            idleReleaseJob?.cancelAndJoin()
            idleReleaseJob = null
            if (currentModelId == modelId) {
                release()
            }
            val modelDir = File(getModelStorageDirectory(), modelId)
            modelDir.deleteRecursively()
            // Drop it from the custom registry too (no-op for catalog models).
            saveCustomModels(loadCustomModels().filterNot { it.id == modelId })
        }
    }
}
