package com.ether4o4.morsvitaest.sandbox

import android.content.Context
import com.ether4o4.morsvitaest.SandboxController
import com.ether4o4.morsvitaest.SandboxSessions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Typed orchestration over the `morsllm` shell runtime. Installs the script into
 * the sandbox the first time it's needed, then drives provision/pull/serve/stop
 * through the SYSTEM shell session so long-running operations don't block any
 * chat's own shell. JSON-emitting subcommands are run with stderr suppressed so
 * the returned string is a parseable JSON object.
 */
class GgufServerManager(
    private val context: Context,
    private val sandbox: SandboxController,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val installLock = Mutex()

    @Volatile
    private var scriptInstalled = false

    val openAiBaseUrl: String = "http://127.0.0.1:8080/v1"

    /**
     * State of the current long-running engine/model operation. Owned by the
     * manager (an app-scoped singleton) rather than the screen, so a build or
     * download keeps running — and stays observable — after the user navigates
     * away and comes back. [Running] carries a label for the busy UI; [Done] and
     * [Failed] are terminal until the UI acknowledges them.
     */
    sealed interface EngineOp {
        data object Idle : EngineOp
        data class Running(val label: String) : EngineOp
        data class Done(val message: String) : EngineOp
        data class Failed(val result: GenericResult) : EngineOp
    }

    private val _op = MutableStateFlow<EngineOp>(EngineOp.Idle)
    val op: StateFlow<EngineOp> = _op.asStateFlow()

    /**
     * Launch a long op on the manager's own scope. No-op if one is already in
     * flight (prevents a second build/download from a double tap or a re-entered
     * screen). The op runs to completion regardless of UI lifecycle.
     */
    private fun launchOp(label: String, block: suspend () -> EngineOp) {
        if (_op.value is EngineOp.Running) return
        _op.value = EngineOp.Running(label)
        scope.launch {
            _op.value = try {
                block()
            } catch (e: CancellationException) {
                _op.value = EngineOp.Idle
                throw e
            } catch (e: Exception) {
                EngineOp.Failed(GenericResult(ok = false, error = e.message ?: "operation_failed"))
            }
        }
    }

    fun startProvision() = launchOp("Building engine… one-time, may take 10–30 min") {
        val r = provision()
        if (r.ok) EngineOp.Done("Engine ready") else EngineOp.Failed(r)
    }

    fun startPull(repoOrUrl: String, quant: String? = null) = launchOp("Downloading model…") {
        val r = pull(repoOrUrl, quant)
        if (r.ok) EngineOp.Done("Downloaded ${r.file ?: "model"}") else EngineOp.Failed(r)
    }

    fun startServe(modelFilename: String) = launchOp("Starting $modelFilename…") {
        val r = serve(modelFilename)
        if (r.ok) EngineOp.Done("Running. Tap \"Add as service\" below.") else EngineOp.Failed(r)
    }

    fun startStop() = launchOp("Stopping…") {
        val r = stop()
        if (r.ok) EngineOp.Done("Stopped") else EngineOp.Failed(r)
    }

    /** Clear a terminal [EngineOp.Done]/[EngineOp.Failed] back to idle once the UI
     *  has shown it, so it doesn't re-appear on the next visit to the screen. */
    fun acknowledgeOp() {
        if (_op.value is EngineOp.Done || _op.value is EngineOp.Failed) {
            _op.value = EngineOp.Idle
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Serializable
    data class Status(
        val ok: Boolean = true,
        val provisioned: Boolean = false,
        val running: Boolean = false,
        val pid: String = "",
        val port: String = "",
        val model: String = "",
        @SerialName("base_url") val baseUrl: String = "",
    )

    @Serializable
    data class QuantFile(
        val name: String,
        val size: Long = 0,
        val quant: String? = null,
    )

    @Serializable
    data class ListQuantsResult(
        val ok: Boolean = false,
        val repo: String? = null,
        val files: List<QuantFile> = emptyList(),
        val error: String? = null,
    )

    @Serializable
    data class ModelFile(
        val name: String,
        val path: String,
        val size: Long = 0,
    )

    @Serializable
    data class ListModelsResult(
        val ok: Boolean = false,
        val models: List<ModelFile> = emptyList(),
        val error: String? = null,
    )

    @Serializable
    data class GenericResult(
        val ok: Boolean = false,
        val error: String? = null,
        val detail: String? = null,
        @SerialName("log_path") val logPath: String? = null,
        val hint: String? = null,
        @SerialName("base_url") val baseUrl: String? = null,
        val pid: Long? = null,
        val port: Int? = null,
        val model: String? = null,
        val file: String? = null,
        val path: String? = null,
        val size: Long? = null,
        @SerialName("already_built") val alreadyBuilt: Boolean? = null,
    )

    private suspend fun ensureScriptInstalled(): Boolean {
        if (scriptInstalled) return true
        installLock.withLock {
            if (scriptInstalled) return true
            // Sandbox isn't always fully responsive the instant its status flag
            // flips to ready — writeTextFile + executeCommand can intermittently
            // come back garbled or refuse on the first try after a cold mount.
            // Retry up to 3 times with a brief backoff so the first script call
            // after app launch doesn't race the install and report
            // "not built yet" until the user manually re-taps Set up engine.
            for (attempt in 1..3) {
                if (installScriptAsset("sandbox/morsllm.sh", SCRIPT_PATH)) {
                    // Best-effort install of the manual-recovery helper. Failure
                    // here doesn't block provisioning — it's only an escape
                    // hatch the user can invoke from the terminal.
                    installScriptAsset("sandbox/morsllm-setup.sh", SETUP_SCRIPT_PATH)
                    scriptInstalled = true
                    return true
                }
                kotlinx.coroutines.delay(1500L * attempt)
            }
            return false
        }
    }

    private suspend fun installScriptAsset(asset: String, path: String): Boolean {
        val raw = readAssetText(asset) ?: return false
        // Strip CR so a CRLF-mangled asset can't turn the shebang / `set`
        // lines into "command not found" or syntax errors under bash.
        val script = raw.replace("\r\n", "\n").replace("\r", "\n")
        // Clear anything stale at the target first (file, dir, or symlink).
        sandbox.executeCommand(
            command = "rm -rf $path 2>/dev/null; mkdir -p \$(dirname $path)",
            sessionId = SandboxSessions.SYSTEM,
        )
        val written = sandbox.writeTextFile(path, script)
        if (!written) return false
        sandbox.executeCommand(
            command = "chmod 755 $path",
            sessionId = SandboxSessions.SYSTEM,
        )
        val verify = sandbox.executeCommand(
            command = "test -x $path && echo INSTALL_OK",
            sessionId = SandboxSessions.SYSTEM,
        )
        return verify.contains("INSTALL_OK")
    }

    private fun readAssetText(path: String): String? = runCatching {
        context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }.getOrNull()

    private suspend fun runQuick(subcommand: String): String {
        if (!ensureScriptInstalled()) return SCRIPT_INSTALL_FAILED_JSON
        return sandbox.executeCommand(
            command = "$SCRIPT_PATH $subcommand 2>/dev/null",
            sessionId = SandboxSessions.SYSTEM,
        ).trim()
    }

    private suspend fun runStreaming(subcommand: String): String {
        if (!ensureScriptInstalled()) return SCRIPT_INSTALL_FAILED_JSON
        val stdoutBuf = StringBuilder()
        val handle = sandbox.executeCommandStreaming(
            command = "$SCRIPT_PATH $subcommand",
            onStdout = { synchronized(stdoutBuf) { stdoutBuf.append(it) } },
            onStderr = { /* progress; ignored — morsllm emits the result JSON on stdout */ },
            sessionId = SandboxSessions.SYSTEM,
        )
        handle.awaitExit()
        val captured = synchronized(stdoutBuf) { stdoutBuf.toString() }
        return captured.lines()
            .lastOrNull { it.trim().startsWith("{") }
            ?.trim()
            ?: ""
    }

    suspend fun status(): Status = decodeOr(runQuick("status"), Status())

    suspend fun listModels(): ListModelsResult = decodeOr(runQuick("list-models"), ListModelsResult(ok = false, error = "decode_failed"))

    suspend fun listQuants(repo: String): ListQuantsResult = decodeOr(
        runQuick("list-quants ${shellQuote(repo)}"),
        ListQuantsResult(ok = false, error = "decode_failed"),
    )

    suspend fun provision(): GenericResult = decodeOr(runStreaming("provision"), GenericResult(ok = false, error = "provision_unparseable"))

    suspend fun pull(repoOrUrl: String, quant: String? = null): GenericResult {
        val args = if (quant.isNullOrBlank()) {
            shellQuote(repoOrUrl)
        } else {
            "${shellQuote(repoOrUrl)} ${shellQuote(quant)}"
        }
        return decodeOr(
            runStreaming("pull $args"),
            GenericResult(ok = false, error = "pull_unparseable"),
        )
    }

    suspend fun serve(modelFilename: String, port: Int = 8080): GenericResult {
        val portArg = if (port == DEFAULT_PORT) "" else " --port $port"
        return decodeOr(
            runStreaming("serve ${shellQuote(modelFilename)}$portArg"),
            GenericResult(ok = false, error = "serve_unparseable"),
        )
    }

    suspend fun stop(): GenericResult = decodeOr(runQuick("stop"), GenericResult(ok = false, error = "stop_unparseable"))

    /** Read the tail of a log file from inside the sandbox; capped so we don't
     * push huge text into a Compose dialog. Returns empty string if missing. */
    suspend fun readLogTail(path: String, maxBytes: Int = 8000): String = sandbox.executeCommand(
        command = "tail -c $maxBytes ${shellQuote(path)} 2>/dev/null",
        sessionId = SandboxSessions.SYSTEM,
    )

    private inline fun <reified T> decodeOr(raw: String, fallback: T): T = runCatching {
        if (raw.isBlank()) fallback else json.decodeFromString<T>(raw)
    }.getOrDefault(fallback)

    private fun shellQuote(s: String): String = "'" + s.replace("'", "'\\''") + "'"

    init {
        // Pre-install the script once the sandbox reaches Ready so the user can
        // also invoke `morsllm` directly from the in-app Terminal without going
        // through this manager. Pure file write + chmod — building llama-server
        // is gated behind explicit provision().
        scope.launch {
            sandbox.status.filter { it.ready }.first()
            ensureScriptInstalled()
        }
    }

    companion object {
        const val DEFAULT_PORT = 8080
        private const val SCRIPT_PATH = "/usr/local/bin/morsllm"
        private const val SETUP_SCRIPT_PATH = "/usr/local/bin/morsllm-setup"
        private const val SCRIPT_INSTALL_FAILED_JSON = """{"ok":false,"error":"script_install_failed"}"""
    }
}
