package com.ether4o4.morsvitaest

interface DaemonController {
    fun start()
    fun stop()
}

expect fun createDaemonController(): DaemonController
