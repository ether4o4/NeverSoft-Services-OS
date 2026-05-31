package com.ether4o4.morsvitaest.sandbox

import android.content.Context
import com.ether4o4.morsvitaest.SandboxController
import com.ether4o4.morsvitaest.SandboxSessions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
            val raw = readAssetText("sandbox/morsllm.sh") ?: return false
            // Strip CR so a CRLF-mangled asset can't turn the shebang / `set`
            // lines into "command not found" or syntax errors under bash.
            val script = raw.replace("\r\n", "\n").replace("\r", "\n")
            // Clear anything stale at the target first. If a prior run or a user
            // experiment left a *directory* (or a non-file) at this path,
            // writeTextFile refuses to overwrite it and the shell then reports
            // "morsllm: Is a directory" on every invocation.
            sandbox.executeCommand(
                command = "rm -rf $SCRIPT_PATH 2>/dev/null; mkdir -p \$(dirname $SCRIPT_PATH)",
                sessionId = SandboxSessions.SYSTEM,
            )
            val written = sandbox.writeTextFile(SCRIPT_PATH, script)
            if (!written) return false
            // writeTextFile doesn't set the executable bit.
            sandbox.executeCommand(
                command = "chmod 755 $SCRIPT_PATH",
                sessionId = SandboxSessions.SYSTEM,
            )
            // Confirm it's an executable regular file before trusting it.
            val verify = sandbox.executeCommand(
                command = "test -x $SCRIPT_PATH && echo MORSLLM_OK",
                sessionId = SandboxSessions.SYSTEM,
            )
            if (!verify.contains("MORSLLM_OK")) return false
            scriptInstalled = true
            return true
        }
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
    suspend fun readLogTail(path: String, maxBytes: Int = 8000): String =
        sandbox.executeCommand(
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
        private const val SCRIPT_INSTALL_FAILED_JSON = """{"ok":false,"error":"script_install_failed"}"""
    }
}
