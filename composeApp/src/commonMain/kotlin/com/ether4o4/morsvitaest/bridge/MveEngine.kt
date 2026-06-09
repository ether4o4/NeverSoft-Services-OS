package com.ether4o4.morsvitaest.bridge

import com.ether4o4.morsvitaest.SandboxController
import com.ether4o4.morsvitaest.SandboxFileEntry
import com.ether4o4.morsvitaest.SandboxStatus
import com.ether4o4.morsvitaest.data.DataRepository
import com.ether4o4.morsvitaest.data.ServiceInstance
import com.ether4o4.morsvitaest.ui.chat.History
import kotlinx.coroutines.flow.StateFlow

/**
 * The MVE kernel as seen by a non-Compose host (the NeverSoft OS shell).
 *
 * MVE's full engine lives behind [DataRepository] and [SandboxController]; this
 * facade exposes the curated slice a desktop shell actually drives — chat,
 * provider/API-key config, the core toggles, and the Linux sandbox (terminal +
 * files) — as plain suspend/`StateFlow` calls a native bridge can marshal. It
 * adds no behavior of its own beyond two convenience helpers built on the
 * sandbox; everything else delegates straight to the reused engine.
 *
 * Obtain one via [HeadlessEngine.start]. Construction is internal so the only
 * supported entry point stays the boot path that wires the Koin graph.
 */
class MveEngine internal constructor(
    val data: DataRepository,
    val sandbox: SandboxController,
) {
    // ---- Chat ----------------------------------------------------------------

    /** The live transcript of the active conversation. */
    val chatHistory: StateFlow<List<History>> get() = data.chatHistory

    /** Send a user turn through the full MVE pipeline (provider, tools, memory). */
    suspend fun sendMessage(text: String) = data.ask(text, emptyList())

    fun startNewChat() = data.startNewChat()

    fun clearChat() = data.clearHistory()

    // ---- Providers / API keys ------------------------------------------------

    fun services(): List<ServiceInstance> = data.getConfiguredServiceInstances()

    fun apiKey(instanceId: String): String = data.getInstanceApiKey(instanceId)

    fun setApiKey(instanceId: String, apiKey: String) = data.updateInstanceApiKey(instanceId, apiKey)

    fun isServiceEnabled(instanceId: String): Boolean = data.isInstanceEnabled(instanceId)

    fun setServiceEnabled(instanceId: String, enabled: Boolean) = data.setInstanceEnabled(instanceId, enabled)

    // ---- Core toggles --------------------------------------------------------

    fun isSandboxEnabled(): Boolean = data.isSandboxEnabled()
    fun setSandboxEnabled(enabled: Boolean) = data.setSandboxEnabled(enabled)

    fun isDaemonEnabled(): Boolean = data.isDaemonEnabled()
    fun setDaemonEnabled(enabled: Boolean) = data.setDaemonEnabled(enabled)

    // ---- Sandbox: terminal + files ------------------------------------------

    val sandboxStatus: StateFlow<SandboxStatus> get() = sandbox.status

    fun setupSandbox() = sandbox.setup()

    /** Run a shell command in the persistent sandbox shell; returns combined output. */
    suspend fun run(command: String): String = sandbox.executeCommand(command)

    suspend fun listDir(path: String): List<SandboxFileEntry> = sandbox.listDirectory(path)

    suspend fun readFile(path: String): String? = sandbox.readTextFile(path)

    suspend fun writeFile(path: String, content: String): Boolean = sandbox.writeTextFile(path, content)

    // ---- Use-case helpers ----------------------------------------------------

    /**
     * Recursively collect the paths of files under [root] whose name contains
     * [keyword] (case-insensitive) — the "find every file about X" helper.
     */
    suspend fun searchFilenames(root: String, keyword: String): List<String> {
        val out = run("find ${shellQuote(root)} -type f -iname ${shellQuote("*$keyword*")} 2>/dev/null")
        return out.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
    }

    /**
     * Validation-gated write — the "play button that won't let you save with
     * errors" behavior. Stages [content] to a temp file, runs [validateCommand]
     * against it (the literal `{file}` is substituted with the staged path), and
     * only promotes the content to [path] when the validator exits 0. The live
     * file is never touched on failure, so a broken edit can't take down a
     * working component (e.g. the taskbar).
     */
    suspend fun writeIfValid(
        path: String,
        content: String,
        validateCommand: String,
    ): ValidationResult {
        val stage = "/tmp/.mve_stage_${path.substringAfterLast('/')}"
        if (!sandbox.writeTextFile(stage, content)) {
            return ValidationResult(saved = false, output = "failed to stage file for validation")
        }
        val cmd = validateCommand.replace("{file}", shellQuote(stage))
        val output = run("$cmd; echo \"$EXIT_MARKER$?\"")
        val ok = output.trimEnd().endsWith("${EXIT_MARKER}0")
        if (ok) sandbox.writeTextFile(path, content)
        return ValidationResult(saved = ok, output = output.substringBeforeLast(EXIT_MARKER).trim())
    }

    private companion object {
        const val EXIT_MARKER = "__MVE_EXIT__"
    }
}

/** Outcome of [MveEngine.writeIfValid]: whether the file was promoted, plus validator output. */
data class ValidationResult(val saved: Boolean, val output: String)

/** Wrap [value] in single quotes for safe interpolation into a shell command. */
private fun shellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
