package com.ether4o4.morsvitaest.ui.settings.diagnostics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Locks the [DiagnosticsBuilder] state machine in place. The Android UI
 * just shuffles live data into a [DiagnosticInput] and renders the
 * resulting check list — every "what shows up when X is broken?" question
 * is decided here, in a pure test.
 */
class DiagnosticsBuilderTest {

    private fun baseInput() = DiagnosticInput(
        appVersion = "2.6.2",
        platformName = "Android",
        sandboxReady = false,
        engineProvisioned = false,
        serverRunning = false,
        serverModel = null,
        serverPort = null,
        downloadedModelCount = 0,
        endpointReachable = null,
        matchingServiceInstanceId = null,
        matchingServiceEnabled = false,
    )

    @Test
    fun `version row is always first and always OK`() {
        val checks = DiagnosticsBuilder.build(baseInput())
        val first = checks.first()
        assertEquals("version", first.key)
        assertEquals(DiagnosticStatus.OK, first.status)
        assertTrue(first.detail.contains("2.6.2"))
        assertTrue(first.detail.contains("Android"))
    }

    @Test
    fun `sandbox down short-circuits subsequent rows`() {
        val checks = DiagnosticsBuilder.build(baseInput())
        // version + sandbox only — no engine / models / server / endpoint /
        // chat-service rows, because they would all be red for the same
        // reason.
        assertEquals(2, checks.size)
        val sandbox = checks[1]
        assertEquals("sandbox", sandbox.key)
        assertEquals(DiagnosticStatus.ERROR, sandbox.status)
        assertEquals(DiagnosticRepair.ENABLE_SANDBOX, sandbox.repair)
    }

    @Test
    fun `sandbox up but no engine shows engine ERROR and stops`() {
        val checks = DiagnosticsBuilder.build(baseInput().copy(sandboxReady = true))
        assertEquals(3, checks.size)
        assertEquals("version", checks[0].key)
        assertEquals("sandbox", checks[1].key)
        assertEquals(DiagnosticStatus.OK, checks[1].status)
        val engine = checks[2]
        assertEquals("engine", engine.key)
        assertEquals(DiagnosticStatus.ERROR, engine.status)
        assertEquals(DiagnosticRepair.BUILD_ENGINE, engine.repair)
    }

    @Test
    fun `engine ready but no models shows model WARN, no server row`() {
        val input = baseInput().copy(sandboxReady = true, engineProvisioned = true)
        val checks = DiagnosticsBuilder.build(input)
        val keys = checks.map { it.key }
        assertEquals(listOf("version", "sandbox", "engine", "models", "chat-service"), keys)
        assertEquals(DiagnosticStatus.WARN, checks.first { it.key == "models" }.status)
        assertEquals(DiagnosticRepair.DOWNLOAD_MODEL, checks.first { it.key == "models" }.repair)
    }

    @Test
    fun `model on disk but server not running shows server WARN`() {
        val input = baseInput().copy(
            sandboxReady = true,
            engineProvisioned = true,
            downloadedModelCount = 2,
        )
        val checks = DiagnosticsBuilder.build(input)
        val server = checks.first { it.key == "server" }
        assertEquals(DiagnosticStatus.WARN, server.status)
        assertTrue(server.detail.contains("Run"))
        // No endpoint row when the server isn't running — no point probing.
        assertTrue(checks.none { it.key == "endpoint" })
    }

    @Test
    fun `server running but endpoint unreachable -- ERROR with RESTART_SERVER`() {
        val input = baseInput().copy(
            sandboxReady = true,
            engineProvisioned = true,
            serverRunning = true,
            serverModel = "Dolphin",
            serverPort = "8080",
            downloadedModelCount = 1,
            endpointReachable = false,
        )
        val checks = DiagnosticsBuilder.build(input)
        val endpoint = checks.first { it.key == "endpoint" }
        assertEquals(DiagnosticStatus.ERROR, endpoint.status)
        assertEquals(DiagnosticRepair.RESTART_SERVER, endpoint.repair)
    }

    @Test
    fun `endpoint probe pending renders CHECKING with no repair`() {
        val input = baseInput().copy(
            sandboxReady = true,
            engineProvisioned = true,
            serverRunning = true,
            serverModel = "Dolphin",
            serverPort = "8080",
            downloadedModelCount = 1,
            endpointReachable = null,
        )
        val checks = DiagnosticsBuilder.build(input)
        val endpoint = checks.first { it.key == "endpoint" }
        assertEquals(DiagnosticStatus.CHECKING, endpoint.status)
        assertNull(endpoint.repair)
    }

    @Test
    fun `fully ready chain -- every check is green`() {
        val input = baseInput().copy(
            sandboxReady = true,
            engineProvisioned = true,
            serverRunning = true,
            serverModel = "Dolphin",
            serverPort = "8080",
            downloadedModelCount = 1,
            endpointReachable = true,
            matchingServiceInstanceId = "abc",
            matchingServiceEnabled = true,
        )
        val checks = DiagnosticsBuilder.build(input)
        assertTrue(
            checks.all { it.status == DiagnosticStatus.OK },
            "expected all OK, got: ${checks.map { it.key to it.status }}",
        )
        // Sanity: every expected row is present.
        assertEquals(
            listOf("version", "sandbox", "engine", "models", "server", "endpoint", "chat-service"),
            checks.map { it.key },
        )
    }

    @Test
    fun `service registered but disabled -- WARN ENABLE_SERVICE`() {
        val input = baseInput().copy(
            sandboxReady = true,
            engineProvisioned = true,
            serverRunning = true,
            serverModel = "Dolphin",
            serverPort = "8080",
            downloadedModelCount = 1,
            endpointReachable = true,
            matchingServiceInstanceId = "abc",
            matchingServiceEnabled = false,
        )
        val checks = DiagnosticsBuilder.build(input)
        val chat = checks.first { it.key == "chat-service" }
        assertEquals(DiagnosticStatus.WARN, chat.status)
        assertEquals(DiagnosticRepair.ENABLE_SERVICE, chat.repair)
        assertTrue(chat.detail.contains("Show in chat"))
    }

    @Test
    fun `no matching service at all -- WARN REGISTER_SERVICE`() {
        val input = baseInput().copy(
            sandboxReady = true,
            engineProvisioned = true,
            serverRunning = true,
            serverModel = "Dolphin",
            serverPort = "8080",
            downloadedModelCount = 1,
            endpointReachable = true,
            matchingServiceInstanceId = null,
        )
        val checks = DiagnosticsBuilder.build(input)
        val chat = checks.first { it.key == "chat-service" }
        assertEquals(DiagnosticStatus.WARN, chat.status)
        assertEquals(DiagnosticRepair.REGISTER_SERVICE, chat.repair)
    }

    @Test
    fun `serverPort blank renders server row without port fragment`() {
        // Defensive: GgufServerManager.Status fields are non-null Strings
        // that default to "". Builder should tolerate "" and not emit
        // "on port " with a trailing space.
        val input = baseInput().copy(
            sandboxReady = true,
            engineProvisioned = true,
            serverRunning = true,
            serverModel = "Dolphin",
            serverPort = "",
            downloadedModelCount = 1,
            endpointReachable = true,
        )
        val checks = DiagnosticsBuilder.build(input)
        val server = checks.first { it.key == "server" }
        val detail = server.detail
        assertTrue(
            !detail.contains("on port"),
            "expected detail to skip the port fragment for blank port, got: $detail",
        )
    }

    @Test
    fun `repair tokens never leak into a OK row`() {
        // OK rows never need fixing — they should leave repair null so the
        // UI doesn't render an empty action button.
        val input = baseInput().copy(
            sandboxReady = true,
            engineProvisioned = true,
            serverRunning = true,
            serverModel = "Dolphin",
            serverPort = "8080",
            downloadedModelCount = 1,
            endpointReachable = true,
            matchingServiceInstanceId = "abc",
            matchingServiceEnabled = true,
        )
        val checks = DiagnosticsBuilder.build(input)
        checks.filter { it.status == DiagnosticStatus.OK }.forEach {
            assertNull(it.repair, "OK row ${it.key} should not carry a repair token")
            assertNull(it.repairLabel, "OK row ${it.key} should not carry a repair label")
        }
    }

    @Test
    fun `every row has a stable key for compose recomposition`() {
        // Keys are used as remember() identifiers in the row composable;
        // they need to be present and unique within a single build.
        val input = baseInput().copy(
            sandboxReady = true,
            engineProvisioned = true,
            serverRunning = true,
            serverModel = "Dolphin",
            serverPort = "8080",
            downloadedModelCount = 1,
            endpointReachable = true,
            matchingServiceInstanceId = "abc",
            matchingServiceEnabled = true,
        )
        val checks = DiagnosticsBuilder.build(input)
        val keys = checks.map { it.key }
        assertEquals(keys.distinct().size, keys.size, "duplicate diagnostic key(s) in $keys")
        keys.forEach { assertNotNull(it) }
    }
}
