package com.ether4o4.morsvitaest

actual fun createDaemonController(): DaemonController = NoOpDaemonController()

class NoOpDaemonController : DaemonController {
    override fun start() { /* No-op on iOS */ }
    override fun stop() { /* No-op on iOS */ }
}
