package com.ether4o4.morsvitaest.ui.settings.diagnostics

/**
 * Pure data + pure builder for the local-LLM readiness panel rendered in
 * the Local LLM tab. The Android UI gathers the live inputs
 * (sandbox status, engine status, downloaded models, /health probe, service
 * registration) and calls [DiagnosticsBuilder.build]; the builder returns
 * the ordered list of rows to render. Keeping the build logic pure lets us
 * unit-test every state combination without spinning up the proot sandbox
 * or hitting localhost.
 *
 * The builder short-circuits: if the sandbox is down, downstream checks
 * (engine, models, server, endpoint, service) are not emitted because they
 * would all read as red and crowd the panel for no signal. Same shape when
 * the sandbox is up but the engine isn't built: nothing past `engine` is
 * meaningful yet.
 */

enum class DiagnosticStatus { OK, WARN, ERROR, CHECKING }

/**
 * Identifies a one-tap repair action the UI can wire to. Keeping the
 * builder pure means we pass enum tokens here, not lambdas — the UI maps
 * tokens to actual side-effects (provision, restart, scroll-to). The
 * navigation-hint variants (ENABLE_SANDBOX, DOWNLOAD_MODEL, etc.) have no
 * automated fix; tapping just re-runs the checks so the panel refreshes
 * after the user follows the in-row hint.
 */
enum class DiagnosticRepair {
    BUILD_ENGINE,
    RESTART_SERVER,
    ENABLE_SANDBOX,
    DOWNLOAD_MODEL,
    REGISTER_SERVICE,
    ENABLE_SERVICE,
}

data class DiagnosticCheck(
    val key: String,
    val label: String,
    val status: DiagnosticStatus,
    val detail: String,
    val repair: DiagnosticRepair? = null,
    val repairLabel: String? = null,
)

/**
 * Snapshot of every observable bit of the local-LLM stack at the moment
 * the diagnostic runs. Nullable fields mean "not applicable / not probed
 * yet"; the builder handles the absence semantically (e.g. a null
 * [endpointReachable] while [serverRunning] = true renders as CHECKING).
 */
data class DiagnosticInput(
    val appVersion: String,
    val platformName: String,
    val sandboxReady: Boolean,
    val engineProvisioned: Boolean,
    val serverRunning: Boolean,
    val serverModel: String?,
    val serverPort: String?,
    val downloadedModelCount: Int,
    val endpointReachable: Boolean?,
    val matchingServiceInstanceId: String?,
    val matchingServiceEnabled: Boolean,
)

object DiagnosticsBuilder {
    fun build(input: DiagnosticInput): List<DiagnosticCheck> {
        val out = mutableListOf<DiagnosticCheck>()

        // 1. App build — always green, pure info. First row so a user
        // reporting an issue can read the version off the diagnostic panel
        // without leaving the screen.
        out += DiagnosticCheck(
            key = "version",
            label = "App build",
            status = DiagnosticStatus.OK,
            detail = "MVE ${input.appVersion} on ${input.platformName}",
        )

        // 2. Sandbox runtime. Everything below depends on this; if it's
        // down, short-circuit so we don't show six red rows that all mean
        // the same thing.
        out += if (input.sandboxReady) {
            DiagnosticCheck(
                key = "sandbox",
                label = "Linux sandbox",
                status = DiagnosticStatus.OK,
                detail = "Running",
            )
        } else {
            DiagnosticCheck(
                key = "sandbox",
                label = "Linux sandbox",
                status = DiagnosticStatus.ERROR,
                detail = "Not running — enable it in the Sandbox card above",
                repair = DiagnosticRepair.ENABLE_SANDBOX,
                repairLabel = "How",
            )
        }
        if (!input.sandboxReady) return out

        // 3. Engine binary (llama-server, built or downloaded into the
        // sandbox). Same short-circuit logic: no point checking server /
        // endpoint / service if the binary isn't there.
        out += if (input.engineProvisioned) {
            DiagnosticCheck(
                key = "engine",
                label = "llama-server engine",
                status = DiagnosticStatus.OK,
                detail = "Built and runnable",
            )
        } else {
            DiagnosticCheck(
                key = "engine",
                label = "llama-server engine",
                status = DiagnosticStatus.ERROR,
                detail = "Not built yet — first build pulls a prebuilt binary in seconds, falls back to source compile if that fails (10–30 min on a phone)",
                repair = DiagnosticRepair.BUILD_ENGINE,
                repairLabel = "Build now",
            )
        }
        if (!input.engineProvisioned) return out

        // 4. Downloaded GGUF models. Yellow if none — the user can still
        // download one from the card above, so we don't gate further
        // checks; they just won't be very interesting until a model is on
        // disk.
        out += if (input.downloadedModelCount > 0) {
            DiagnosticCheck(
                key = "models",
                label = "Downloaded models",
                status = DiagnosticStatus.OK,
                detail = "${input.downloadedModelCount} GGUF file(s) on disk",
            )
        } else {
            DiagnosticCheck(
                key = "models",
                label = "Downloaded models",
                status = DiagnosticStatus.WARN,
                detail = "No GGUF models yet — pick one from the card above",
                repair = DiagnosticRepair.DOWNLOAD_MODEL,
                repairLabel = "How",
            )
        }

        // 5. Inference server. We only emit a server row at all if either
        // the server is actually running OR the user has a model
        // downloaded (in which case the absence of a running server is
        // actionable: tap Run on the model). Skipping the row when there's
        // no model and no server avoids "everything is fine but nothing is
        // running" confusion in the early-setup state.
        if (input.serverRunning) {
            val portFragment = if (!input.serverPort.isNullOrBlank()) " on port ${input.serverPort}" else ""
            out += DiagnosticCheck(
                key = "server",
                label = "Inference server",
                status = DiagnosticStatus.OK,
                detail = "Serving ${input.serverModel ?: "model"}$portFragment",
            )

            // 6. Endpoint reachability — only checked when the server
            // claims to be up. If /health responds, the chat path works
            // end-to-end. If it doesn't, the server PID is alive but the
            // socket is broken (port in use, crash mid-init, etc.) and
            // the user should restart.
            out += when (input.endpointReachable) {
                true -> DiagnosticCheck(
                    key = "endpoint",
                    label = "Endpoint reachable",
                    status = DiagnosticStatus.OK,
                    detail = "/health responds at 127.0.0.1:${input.serverPort.orEmpty()}",
                )
                false -> DiagnosticCheck(
                    key = "endpoint",
                    label = "Endpoint reachable",
                    status = DiagnosticStatus.ERROR,
                    detail = "Server process is alive but /health did not respond. Restarting usually fixes this.",
                    repair = DiagnosticRepair.RESTART_SERVER,
                    repairLabel = "Restart",
                )
                null -> DiagnosticCheck(
                    key = "endpoint",
                    label = "Endpoint reachable",
                    status = DiagnosticStatus.CHECKING,
                    detail = "Probing /health…",
                )
            }
        } else if (input.downloadedModelCount > 0) {
            out += DiagnosticCheck(
                key = "server",
                label = "Inference server",
                status = DiagnosticStatus.WARN,
                detail = "Not serving — tap Run on a downloaded model in the card above",
            )
        }

        // 7. Chat-picker registration. The chat UI only sees services
        // that (a) exist in the configured-services list and (b) have
        // "Show in chat" turned on. Both gates need to be green for the
        // user to actually pick the local engine in chat.
        out += when {
            input.matchingServiceInstanceId == null -> DiagnosticCheck(
                key = "chat-service",
                label = "Chat picker registration",
                status = DiagnosticStatus.WARN,
                detail = "No service points at the local engine yet — tap \"Use in chat\" once a model is running",
                repair = DiagnosticRepair.REGISTER_SERVICE,
                repairLabel = "How",
            )
            !input.matchingServiceEnabled -> DiagnosticCheck(
                key = "chat-service",
                label = "Chat picker registration",
                status = DiagnosticStatus.WARN,
                detail = "Local service exists but is hidden — flip \"Show in chat\" on it in Services",
                repair = DiagnosticRepair.ENABLE_SERVICE,
                repairLabel = "How",
            )
            else -> DiagnosticCheck(
                key = "chat-service",
                label = "Chat picker registration",
                status = DiagnosticStatus.OK,
                detail = "Local model is visible in the chat AI picker",
            )
        }

        return out
    }
}
