package com.ether4o4.morsvitaest

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.ether4o4.morsvitaest.data.AppSettings
import com.ether4o4.morsvitaest.data.DataRepository
import com.ether4o4.morsvitaest.data.ThemeMode
import com.ether4o4.morsvitaest.ui.DarkColorScheme
import com.ether4o4.morsvitaest.ui.LightColorScheme
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import nl.marc_apps.tts.TextToSpeechEngine
import nl.marc_apps.tts.rememberTextToSpeechOrNull
import org.koin.android.ext.android.get

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        FileKit.init(this)
        handleDeepLinkIntent(intent)

        val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val appSettings: AppSettings = get()
        setContent {
            val themeMode by appSettings.themeModeFlow.collectAsStateWithLifecycle()
            val systemInDark = isSystemInDarkTheme()
            val isDarkTheme = when (themeMode) {
                ThemeMode.System -> systemInDark
                ThemeMode.Light -> false
                ThemeMode.Dark, ThemeMode.OledBlack -> true
            }
            LaunchedEffect(isDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT,
                        )
                    },
                    navigationBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT,
                        )
                    },
                )
            }
            // Full-screen launcher: hide the system nav bar (gesture pill) so the
            // taskbar is the true bottom edge. Keyed on isDarkTheme too, so it re-applies
            // after enableEdgeToEdge runs (which can re-show the bar).
            val fullscreenLauncher by appSettings.fullscreenLauncherFlow.collectAsStateWithLifecycle()
            LaunchedEffect(fullscreenLauncher, isDarkTheme) {
                applyImmersive(fullscreenLauncher)
            }
            val context = LocalContext.current
            val lightScheme: ColorScheme = if (dynamicColor) dynamicLightColorScheme(context) else LightColorScheme
            val darkScheme: ColorScheme = if (dynamicColor) dynamicDarkColorScheme(context) else DarkColorScheme
            val navController = rememberNavController()
            // Defer TTS initialization until after the first frame
            var ttsReady by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { ttsReady = true }
            val textToSpeech = if (ttsReady) {
                rememberTextToSpeechOrNull(TextToSpeechEngine.Google)
            } else {
                null
            }
            App(
                navController = navController,
                lightColorScheme = lightScheme,
                darkColorScheme = darkScheme,
                textToSpeech = textToSpeech,
                isKoinStarted = true,
                onAppOpens = { appOpens ->
                    if (appOpens % 5 == 0) {
                        requestReview(this@MainActivity)
                    }
                },
            )
        }
    }

    override fun onStart() {
        super.onStart()
        // Re-assert the daemon every time the activity is brought to the foreground.
        // `onCreate`-only is not enough: aggressive OEM battery managers (MIUI,
        // EMUI/Huawei) sometimes kill the foreground service while the activity
        // is still alive in the background — without this, the user has to fully
        // close and reopen the app for scheduling to resume. `startForegroundService`
        // is idempotent when the service is already up.
        autoStartDaemon()
        // Re-assert immersive mode (some OEMs reset it when returning to the app).
        applyImmersive(get<AppSettings>().isFullscreenLauncherEnabled())
    }

    /**
     * Hide or show the system navigation bar (gesture pill) for MorsVitaEst's own
     * window. When hidden the taskbar sits flush at the very bottom — the desktop
     * look; the bar reappears transiently on an edge swipe. This only affects MVE's
     * window (an app can't hide the nav bar over other apps without root).
     */
    private fun applyImmersive(enabled: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (enabled) {
            controller.hide(WindowInsetsCompat.Type.navigationBars())
        } else {
            controller.show(WindowInsetsCompat.Type.navigationBars())
        }
    }

    private fun autoStartDaemon() {
        val daemonController: DaemonController = get()
        if (daemonController is AndroidDaemonController && daemonController.shouldAutoStart()) {
            daemonController.start()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_HEARTBEAT, false) == true) {
            val dataRepository: DataRepository = get()
            dataRepository.requestOpenHeartbeat()
            // Drop the extra so a configuration change (screen rotation) doesn't re-trigger
            // the deep-link after ChatViewModel has already consumed it.
            intent.removeExtra(EXTRA_OPEN_HEARTBEAT)
        }
        if (intent?.getBooleanExtra(EXTRA_OPEN_START_MENU, false) == true) {
            val dataRepository: DataRepository = get()
            dataRepository.requestOpenStartMenu()
            intent.removeExtra(EXTRA_OPEN_START_MENU)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(navController = rememberNavController())
}
