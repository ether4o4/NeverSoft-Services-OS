package com.ether4o4.morsvitaest

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.ether4o4.morsvitaest.data.TaskScheduler
import com.ether4o4.morsvitaest.sandbox.GgufServerManager
import com.ether4o4.morsvitaest.sandbox.sandboxModule
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MorsVitaEstApplication : Application() {

    private val taskScheduler: TaskScheduler by inject()

    // Eagerly construct the manager so its sandbox-ready watcher fires and the
    // `morsllm` script lands in /usr/local/bin without any UI interaction —
    // makes it usable directly from the in-app Terminal.
    private val ggufServerManager: GgufServerManager by inject()

    override fun onCreate() {
        super.onCreate()
        installCrashHandler()
        startKoin {
            androidContext(this@MorsVitaEstApplication)
            modules(appModule, sandboxModule)
        }
        // Force construction so the manager's sandbox-ready watcher arms.
        ggufServerManager

        // Track app foreground state so the scheduler only pushes a heartbeat notification
        // when the in-app banner isn't visible. ViewModel lifecycle is the wrong signal —
        // it survives backgrounding and only clears on Activity destruction.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                taskScheduler.appInForeground = true
            }
            override fun onStop(owner: LifecycleOwner) {
                taskScheduler.appInForeground = false
            }
        })
    }

    /**
     * Persist the stack trace of any uncaught (Kotlin/Java) exception to
     * `last_crash.txt` so MainActivity can show it on the next launch — turning a
     * silent force-close into a report the user can read and share without a PC or
     * adb. Native crashes (SIGSEGV/SIGABRT) bypass this handler and aren't captured.
     */
    private fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val report = buildString {
                    append("MorsVitaEst crashed at ")
                    append(java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date()))
                    append("\nThread: ").append(thread.name).append('\n').append('\n')
                    append(throwable.stackTraceToString())
                }
                java.io.File(filesDir, "last_crash.txt").writeText(report)
            }
            previous?.uncaughtException(thread, throwable)
        }
    }
}
