package com.ether4o4.morsvitaest.sandbox

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val sandboxModule = module {
    single<LinuxSandboxManager> { LinuxSandboxManager(androidContext(), get()) }
    single<GgufServerManager> { GgufServerManager(androidContext(), get()) }
}
