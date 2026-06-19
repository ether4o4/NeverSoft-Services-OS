package com.ether4o4.morsvitaest

import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import com.ether4o4.morsvitaest.network.tools.Tool
import com.ether4o4.morsvitaest.network.tools.ToolInfo
import com.russhwolf.settings.Settings
import io.github.vinceglb.filekit.PlatformFile
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import kotlin.coroutines.CoroutineContext

expect fun httpClient(config: HttpClientConfig<*>.() -> Unit = {}): HttpClient

expect fun createSecureSettings(): Settings

expect fun createLegacySettings(): Settings?

expect fun getBackgroundDispatcher(): CoroutineContext

expect fun onDragAndDropEventDropped(event: DragAndDropEvent): PlatformFile?

expect val BackIcon: ImageVector

sealed class Platform(val displayName: String) {
    sealed class Mobile(displayName: String) : Platform(displayName) {
        data object Android : Mobile("Android")
        data object Ios : Mobile("iOS")
    }

    sealed class Desktop(displayName: String) : Platform(displayName) {
        data object Mac : Desktop("macOS")
        data object Windows : Desktop("Windows")
        data object Linux : Desktop("Linux")
    }

    data object Web : Platform("Web")
}

expect val currentPlatform: Platform

expect val defaultUiScale: Float

expect fun getAppFilesDirectory(): String

expect fun getAvailableTools(): List<Tool>

/**
 * Returns all raw tool definitions available on this platform.
 * The returned tools have no isEnabled state set - that's handled by RemoteDataRepository.
 * Unlike getAvailableTools(), this returns all tools regardless of enabled state.
 */
expect fun getPlatformToolDefinitions(): List<ToolInfo>

expect val isEmailSupported: Boolean

/**
 * True only on the FOSS Android build. Gated on `READ_SMS` being declared in the
 * merged manifest — the Play Store flavor doesn't declare it, so this returns
 * false there, and the SMS feature is invisible in that build.
 */
expect val isSmsSupported: Boolean

/**
 * True only on the FOSS Android build. Gated on `MorsVitaEstNotificationListenerService`
 * being declared in the merged manifest — the Play Store flavor doesn't declare
 * it, so this returns false there, and the notification-reading feature is
 * invisible in that build.
 */
expect val isNotificationsSupported: Boolean

expect val isSplinterlandsSupported: Boolean

expect suspend fun compressImageBytes(bytes: ByteArray, mimeType: String): ByteArray

expect fun openUrl(url: String): Boolean

/** Launch an installed app by its platform identifier (Android package name). */
expect fun launchApp(appId: String): Boolean

/** A specific system settings screen the first-run setup wizard can open. */
enum class SystemSetting {
    /** Default home-app / launcher chooser. */
    HomeLauncher,

    /** This app's "App info" page — where "Allow restricted settings" + the
     *  Permissions list live. */
    AppDetails,

    /** This app's notification settings. */
    AppNotifications,

    /** The system keyboards / input-method list, to enable & pick the MVE keyboard. */
    InputMethods,
}

/** Opens the given system settings screen. Returns false if unavailable. */
expect fun openSystemSetting(setting: SystemSetting): Boolean

/** A built-in device app the taskbar's glass icons can launch. */
enum class SystemApp {
    Phone,
    Messages,
    Camera,
}

/** Opens the device's default app for [app] (dialer / SMS / camera). Returns false if unavailable. */
expect fun openSystemApp(app: SystemApp): Boolean

/**
 * Persistent taskbar — a system overlay bar that stays on screen over every other
 * app. Only meaningful on Android (needs the "Display over other apps" overlay
 * permission + a foreground service); other platforms report unsupported.
 */
expect fun persistentTaskbarSupported(): Boolean

/** Whether the "Display over other apps" overlay permission has been granted. */
expect fun persistentTaskbarHasPermission(): Boolean

/** Opens the system screen where the user grants the overlay permission. */
expect fun requestPersistentTaskbarPermission()

/**
 * Start or stop the persistent overlay taskbar service to match [enabled]. A no-op
 * if unsupported or (when enabling) the overlay permission isn't granted yet.
 */
expect fun applyPersistentTaskbar(enabled: Boolean)

/**
 * Persist a picked image's [bytes] into app storage under [name] and return its
 * absolute path (or null on failure / unsupported platform). Used for the
 * customizable launcher wallpaper and Start orb.
 */
expect fun saveLauncherImage(name: String, bytes: ByteArray): String?

/**
 * A launchable app installed on the device. [category] mirrors Android's
 * `ApplicationInfo.category` (API 26+; -1 = undefined) so the Start menu can
 * auto-sort apps into its "All apps" boxes without the Android SDK in common code.
 */
data class InstalledApp(
    val label: String,
    val packageName: String,
    val icon: ImageBitmap?,
    val category: Int = -1,
)

/** Every launchable app installed on the device, sorted by label. */
expect suspend fun getInstalledApps(): List<InstalledApp>

/** Live system stats for the widgets panel. */
data class SystemStats(
    val cpu: String,
    val cores: Int,
    val gpu: String,
    val ramUsedMb: Long,
    val ramTotalMb: Long,
    val storageFreeGb: Double,
    val storageTotalGb: Double,
)

expect suspend fun getSystemStats(): SystemStats

@androidx.compose.runtime.Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)

expect fun decodeToImageBitmap(bytes: ByteArray): ImageBitmap?

expect suspend fun saveFileToDevice(bytes: ByteArray, baseName: String, extension: String)

/**
 * Fires a background push notification for a heartbeat that produced a non-trivial
 * response. Android additionally wires a tap-to-open-heartbeat deep link via its
 * PendingIntent; iOS/desktop just surface the message in the OS notification center
 * without deep-linking back to the conversation. No-op on web.
 */
expect fun sendHeartbeatNotification(title: String, body: String)
