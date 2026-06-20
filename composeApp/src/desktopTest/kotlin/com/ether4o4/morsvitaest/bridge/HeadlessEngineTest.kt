package com.ether4o4.morsvitaest.bridge

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Proves the MVE kernel boots with no Compose UI — the load-bearing assumption
 * behind running MVE as the backend for the NeverSoft OS shell.
 *
 * If the engine's Koin graph could not stand up without the ViewModel/Compose
 * bindings, [HeadlessEngine.start] would throw while resolving the kernel, and
 * these tests would fail. They run on the desktop target, where the sandbox and
 * local-inference factories are no-op stubs, so no Android runtime is needed.
 */
class HeadlessEngineTest {

    @AfterTest
    fun tearDown() {
        // Always release the singleton container, even if an assertion failed.
        if (HeadlessEngine.isRunning) HeadlessEngine.stop()
    }

    @Test
    fun engineBootsHeadlessAndExposesTheKernel() {
        val engine = HeadlessEngine.start()

        // The two kernel handles the shell drives must resolve from the graph.
        assertNotNull(engine.data, "DataRepository should resolve from engineModule")
        assertNotNull(engine.sandbox, "SandboxController should resolve from engineModule")

        // Read-only surface should be callable without a configured account.
        assertNotNull(engine.services(), "services() should return a (possibly empty) list")
        assertNotNull(engine.sandboxStatus.value, "sandbox status should be observable")
        assertNotNull(engine.chatHistory.value, "chat history should be observable")
    }

    @Test
    fun startIsIdempotentlyGuardedAndStopReleases() {
        assertFalse(HeadlessEngine.isRunning, "engine should start stopped")

        HeadlessEngine.start()
        assertTrue(HeadlessEngine.isRunning, "engine should report running after start")

        HeadlessEngine.stop()
        assertFalse(HeadlessEngine.isRunning, "engine should report stopped after stop")
    }
}
