package com.ether4o4.morsvitaest.bridge

import com.ether4o4.morsvitaest.SandboxController
import com.ether4o4.morsvitaest.data.DataRepository
import com.ether4o4.morsvitaest.engineModule
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.koinApplication

/**
 * Boots the MVE kernel with no Compose UI — the entry point that turns MVE into
 * a backend for the NeverSoft OS shell (or a desktop test, or a daemon).
 *
 * It loads only [engineModule], so none of the ViewModel/Compose bindings are
 * required. The resulting [MveEngine] hands the host a fully wired engine:
 * chat, providers, the Linux sandbox, MCP, tasks, memory — the same singletons
 * the Compose app uses, minus the screens.
 *
 * The container is created with [koinApplication] (an isolated Koin scope) so
 * starting the headless engine never collides with a global `startKoin` the
 * platform may also run. A platform that owns extra bindings — Android's
 * `sandboxModule`, which supplies the real proot sandbox — passes them via
 * [extraModules].
 */
object HeadlessEngine {

    private var app: KoinApplication? = null

    /** True once [start] has booted the engine and before [stop] tears it down. */
    val isRunning: Boolean get() = app != null

    fun start(extraModules: List<Module> = emptyList()): MveEngine {
        check(app == null) { "HeadlessEngine is already started" }
        val koinApp = koinApplication { modules(listOf(engineModule) + extraModules) }
        app = koinApp
        val koin = koinApp.koin
        return MveEngine(
            data = koin.get<DataRepository>(),
            sandbox = koin.get<SandboxController>(),
        )
    }

    fun stop() {
        app?.close()
        app = null
    }
}
