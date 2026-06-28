package com.ether4o4.morsvitaest.data

import com.ether4o4.morsvitaest.defaultUiScale
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class ImportSection {
    SERVICES,
    SOUL,
    MEMORY,
    SCHEDULING,
    HEARTBEAT,
    EMAIL,
    SMS,
    SPLINTERLANDS,
    TOOLS,
    MCP,
    CONVERSATIONS,
}

enum class ThemeMode {
    System,
    Light,
    Dark,
    OledBlack,
}

/**
 * Stricter than [detectImportSections]: only includes sections that contain actual user data,
 * skipping ones that exist purely because of default feature-toggle flags (e.g. `sms_enabled = false`,
 * `splinterlands_enabled = false`, `mcp_servers = []`). Used to drive the Export preview dialog.
 */
fun detectExportableSections(json: JsonObject): Map<ImportSection, String?> {
    val sections = mutableMapOf<ImportSection, String?>()

    val configured = json["configured_services"]?.jsonArray
    if (configured != null && configured.isNotEmpty()) {
        sections[ImportSection.SERVICES] = "${configured.size}"
    }

    if (json["soul_text"] != null) {
        sections[ImportSection.SOUL] = null
    }

    val memories = json["agent_memories"]?.jsonArray
    if (memories != null && memories.isNotEmpty()) {
        sections[ImportSection.MEMORY] = "${memories.size}"
    }

    val tasks = json["scheduled_tasks"]?.jsonArray
    if (tasks != null && tasks.isNotEmpty()) {
        sections[ImportSection.SCHEDULING] = "${tasks.size}"
    }

    val heartbeatHasPrompt = json["heartbeat_prompt"] != null
    val heartbeatHasConfig = json["heartbeat_config"] != null
    val heartbeatHasLog = json["heartbeat_log"]?.jsonArray?.isNotEmpty() == true
    if (heartbeatHasPrompt || heartbeatHasConfig || heartbeatHasLog) {
        sections[ImportSection.HEARTBEAT] = null
    }

    val emails = json["email_accounts"]?.jsonArray
    if (emails != null && emails.isNotEmpty()) {
        sections[ImportSection.EMAIL] = "${emails.size}"
    }

    val smsEnabled = json["sms_enabled"]?.jsonPrimitive?.content?.toBoolean() == true
    val smsSendEnabled = json["sms_send_enabled"]?.jsonPrimitive?.content?.toBoolean() == true
    if (smsEnabled || smsSendEnabled) {
        sections[ImportSection.SMS] = null
    }

    if (json["splinterlands_account"] != null) {
        sections[ImportSection.SPLINTERLANDS] = null
    }

    val toolOverrides = json["tool_overrides"]?.jsonObject
    if (toolOverrides != null && toolOverrides.isNotEmpty()) {
        val enabled = toolOverrides.count { (_, v) ->
            try {
                v.jsonPrimitive.content.toBoolean()
            } catch (_: Exception) {
                false
            }
        }
        sections[ImportSection.TOOLS] = "$enabled"
    }

    val mcp = json["mcp_servers"]?.jsonArray
    if (mcp != null && mcp.isNotEmpty()) {
        sections[ImportSection.MCP] = "${mcp.size}"
    }

    val conversations = json["conversations"]?.jsonArray
    if (conversations != null && conversations.isNotEmpty()) {
        sections[ImportSection.CONVERSATIONS] = "${conversations.size}"
    }

    return sections
}

fun detectImportSections(json: JsonObject): Map<ImportSection, String?> {
    val sections = mutableMapOf<ImportSection, String?>()
    if (json["configured_services"] != null || json["current_service_id"] != null || json["free_fallback_enabled"] != null || json["instance_settings"] != null) {
        val count = json["configured_services"]?.jsonArray?.size
        sections[ImportSection.SERVICES] = count?.let { "$it" }
    }
    if (json["soul_text"] != null) {
        sections[ImportSection.SOUL] = null
    }
    if (json["memory_enabled"] != null || json["agent_memories"] != null) {
        val count = json["agent_memories"]?.jsonArray?.size
        sections[ImportSection.MEMORY] = count?.let { "$it" }
    }
    if (json["scheduling_enabled"] != null || json["scheduled_tasks"] != null) {
        val count = json["scheduled_tasks"]?.jsonArray?.size
        sections[ImportSection.SCHEDULING] = count?.let { "$it" }
    }
    if (json["heartbeat_config"] != null || json["heartbeat_prompt"] != null || json["heartbeat_log"] != null) {
        sections[ImportSection.HEARTBEAT] = null
    }
    if (json["email_enabled"] != null || json["email_accounts"] != null) {
        val count = json["email_accounts"]?.jsonArray?.size
        sections[ImportSection.EMAIL] = count?.let { "$it" }
    }
    if (json["sms_enabled"] != null || json["sms_poll_interval"] != null || json["sms_send_enabled"] != null) {
        sections[ImportSection.SMS] = null
    }
    if (json["splinterlands_enabled"] != null || json["splinterlands_account"] != null) {
        sections[ImportSection.SPLINTERLANDS] = null
    }
    if (json["tool_overrides"] != null) {
        val enabled = json["tool_overrides"]?.jsonObject?.count { (_, v) ->
            try {
                v.jsonPrimitive.content.toBoolean()
            } catch (_: Exception) {
                false
            }
        }
        sections[ImportSection.TOOLS] = enabled?.let { "$it" }
    }
    if (json["mcp_servers"] != null) {
        val count = json["mcp_servers"]?.jsonArray?.size
        sections[ImportSection.MCP] = count?.let { "$it" }
    }
    if (json["conversations"] != null) {
        val count = try {
            json["conversations"]?.jsonArray?.size
        } catch (_: Exception) {
            null
        }
        sections[ImportSection.CONVERSATIONS] = count?.let { "$it" }
    }
    return sections
}

data class ServiceInstance(
    val instanceId: String,
    val serviceId: String,
)

class AppSettings(internal val settings: Settings) {

    // App open tracking
    fun trackAppOpen(): Int {
        val currentCount = settings.getInt(KEY_APP_OPENS, 0)
        val newCount = currentCount + 1
        settings.putInt(KEY_APP_OPENS, newCount)
        return newCount
    }

    // First-run welcome tour. Shown once until completed/skipped; the help
    // bubble can replay it on demand (which doesn't reset this flag).
    fun hasSeenWelcome(): Boolean = settings.getBoolean(KEY_WELCOME_SEEN, false)

    fun setWelcomeSeen(seen: Boolean = true) {
        settings.putBoolean(KEY_WELCOME_SEEN, seen)
    }

    // First-run setup wizard (home launcher / restricted settings / permissions).
    fun hasSeenSetup(): Boolean = settings.getBoolean(KEY_SETUP_SEEN, false)

    fun setSetupSeen(seen: Boolean = true) {
        settings.putBoolean(KEY_SETUP_SEEN, seen)
    }

    // Guided settings tour (setup recap + appearance + system/AI settings).
    // Shown once after setup; re-launchable from the Start menu any time.
    fun hasSeenGuide(): Boolean = settings.getBoolean(KEY_GUIDE_SEEN, false)

    fun setGuideSeen(seen: Boolean = true) {
        settings.putBoolean(KEY_GUIDE_SEEN, seen)
    }

    // One-time "set up on-device AI" onboarding prompt (download a local model).
    fun hasOfferedOnDeviceAi(): Boolean = settings.getBoolean(KEY_OFFERED_ON_DEVICE_AI, false)

    fun markOnDeviceAiOffered(offered: Boolean = true) {
        settings.putBoolean(KEY_OFFERED_ON_DEVICE_AI, offered)
    }

    // Tool enable/disable settings
    fun isToolEnabled(toolId: String, defaultEnabled: Boolean = true): Boolean = settings.getBoolean("$KEY_TOOL_PREFIX$toolId", defaultEnabled)

    fun setToolEnabled(toolId: String, enabled: Boolean) {
        settings.putBoolean("$KEY_TOOL_PREFIX$toolId", enabled)
    }

    fun getConversationsJson(): String? = settings.getStringOrNull(KEY_CONVERSATIONS)

    fun setConversationsJson(json: String) {
        settings.putString(KEY_CONVERSATIONS, json)
    }

    fun getCurrentConversationId(): String? = settings.getStringOrNull(KEY_CURRENT_CONVERSATION_ID)

    fun setCurrentConversationId(id: String?) {
        if (id == null) {
            settings.remove(KEY_CURRENT_CONVERSATION_ID)
        } else {
            settings.putString(KEY_CURRENT_CONVERSATION_ID, id)
        }
    }

    fun getCurrentInteractiveMode(): Boolean = settings.getBoolean(KEY_CURRENT_INTERACTIVE_MODE, false)

    fun setCurrentInteractiveMode(enabled: Boolean) {
        settings.putBoolean(KEY_CURRENT_INTERACTIVE_MODE, enabled)
    }

    fun isCurrentConversationMigrated(): Boolean = settings.getBoolean(KEY_CURRENT_CONVERSATION_MIGRATED, false)

    fun markCurrentConversationMigrated() {
        settings.putBoolean(KEY_CURRENT_CONVERSATION_MIGRATED, true)
    }

    fun getEncryptionKey(): ByteArray? {
        val encoded = settings.getStringOrNull(KEY_ENCRYPTION_KEY) ?: return null
        return try {
            @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
            kotlin.io.encoding.Base64.decode(encoded)
        } catch (_: Exception) {
            null
        }
    }

    // Free fallback
    fun isFreeFallbackEnabled(): Boolean = settings.getBoolean(KEY_FREE_FALLBACK_ENABLED, true)

    fun setFreeFallbackEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_FREE_FALLBACK_ENABLED, enabled)
    }

    fun getFreeMode(): FreeMode {
        val stored = settings.getStringOrNull(KEY_FREE_MODE) ?: return FreeMode.FAST
        return FreeMode.entries.find { it.name == stored } ?: FreeMode.FAST
    }

    fun setFreeMode(mode: FreeMode) {
        settings.putString(KEY_FREE_MODE, mode.name)
    }

    fun isFreeServicePrimary(): Boolean = settings.getBoolean(KEY_FREE_SERVICE_PRIMARY, false)

    fun setFreeServicePrimary(primary: Boolean) {
        settings.putBoolean(KEY_FREE_SERVICE_PRIMARY, primary)
    }

    // Soul (system prompt)
    fun getSoulText(): String = settings.getString(KEY_SOUL, "")

    fun setSoulText(text: String) {
        settings.putString(KEY_SOUL, text)
    }

    // Memory
    fun isMemoryEnabled(): Boolean = settings.getBoolean(KEY_MEMORY_ENABLED, true)

    fun setMemoryEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_MEMORY_ENABLED, enabled)
    }

    fun getMemoryInstructions(): String = settings.getString(KEY_MEMORY_INSTRUCTIONS, DEFAULT_MEMORY_INSTRUCTIONS)

    // Agent memories
    fun getMemoriesJson(): String = settings.getString(KEY_AGENT_MEMORIES, "[]")

    fun setMemoriesJson(json: String) {
        settings.putString(KEY_AGENT_MEMORIES, json)
    }

    // Scheduling
    fun isSchedulingEnabled(): Boolean = settings.getBoolean(KEY_SCHEDULING_ENABLED, true)

    fun setSchedulingEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_SCHEDULING_ENABLED, enabled)
    }

    // On-device chat engine persistence: true (default) keeps the model warm between uses;
    // false frees it from memory soon after the chat box is idle/closed.
    fun isChatEnginePersistent(): Boolean = settings.getBoolean(KEY_CHAT_ENGINE_PERSISTENT, true)

    fun setChatEnginePersistent(enabled: Boolean) {
        settings.putBoolean(KEY_CHAT_ENGINE_PERSISTENT, enabled)
    }

    // Dynamic UI
    fun isDynamicUiEnabled(): Boolean = settings.getBoolean(KEY_DYNAMIC_UI_ENABLED, true)

    fun setDynamicUiEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_DYNAMIC_UI_ENABLED, enabled)
    }

    private val _themeModeFlow = MutableStateFlow(loadInitialThemeMode())
    val themeModeFlow: StateFlow<ThemeMode> = _themeModeFlow

    fun getThemeMode(): ThemeMode = _themeModeFlow.value

    fun setThemeMode(mode: ThemeMode) {
        settings.putString(KEY_THEME_MODE, mode.name)
        _themeModeFlow.value = mode
    }

    private fun loadInitialThemeMode(): ThemeMode {
        val raw = settings.getString(KEY_THEME_MODE, "")
        if (raw.isNotEmpty()) {
            return try {
                ThemeMode.valueOf(raw)
            } catch (_: IllegalArgumentException) {
                ThemeMode.System
            }
        }
        // No saved choice yet: default to Dark. Migrate the legacy boolean OLED
        // toggle when it was on: true → OledBlack, otherwise → Dark.
        return if (settings.getBoolean(KEY_OLED_MODE_ENABLED, false)) ThemeMode.OledBlack else ThemeMode.Dark
    }

    // Daemon mode
    fun isDaemonEnabled(): Boolean = settings.getBoolean(KEY_DAEMON_ENABLED, false)

    fun setDaemonEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_DAEMON_ENABLED, enabled)
    }

    // Linux Sandbox
    fun isSandboxEnabled(): Boolean = settings.getBoolean(KEY_SANDBOX_ENABLED, true)

    fun setSandboxEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_SANDBOX_ENABLED, enabled)
    }

    // Launcher (NeverSoft OS shell)
    fun getLauncherWallpaper(): String = settings.getString(KEY_LAUNCHER_WALLPAPER, "aurora")

    fun setLauncherWallpaper(value: String) {
        settings.putString(KEY_LAUNCHER_WALLPAPER, value)
    }

    fun isLauncherLabelsShown(): Boolean = settings.getBoolean(KEY_LAUNCHER_LABELS, true)

    fun setLauncherLabelsShown(shown: Boolean) {
        settings.putBoolean(KEY_LAUNCHER_LABELS, shown)
    }

    // Full-screen launcher: hide the system navigation bar (gesture pill) on MVE's
    // own window so the taskbar sits flush at the very bottom — like a desktop OS.
    // Observed as a flow so the Activity can apply immersive mode the moment it changes.
    private val _fullscreenLauncherFlow = MutableStateFlow(settings.getBoolean(KEY_FULLSCREEN_LAUNCHER, false))
    val fullscreenLauncherFlow: StateFlow<Boolean> = _fullscreenLauncherFlow

    fun isFullscreenLauncherEnabled(): Boolean = _fullscreenLauncherFlow.value

    fun setFullscreenLauncherEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_FULLSCREEN_LAUNCHER, enabled)
        _fullscreenLauncherFlow.value = enabled
    }

    // Windowed apps (experimental): launch apps in a freeform window sized to stop
    // just above the taskbar, instead of fullscreen — the desktop-OS model. Requires
    // the device to have freeform windowing enabled (Samsung: Good Lock/MultiStar or
    // the "Force activities to be resizable" developer option).
    fun isWindowedAppsEnabled(): Boolean = settings.getBoolean(KEY_WINDOWED_APPS, false)

    fun setWindowedAppsEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_WINDOWED_APPS, enabled)
    }

    // Sticky-note widget text.
    fun getLauncherNote(): String = settings.getString(KEY_LAUNCHER_NOTE, "")

    fun setLauncherNote(text: String) {
        settings.putString(KEY_LAUNCHER_NOTE, text)
    }

    // The app package the taskbar's file-explorer button launches.
    fun getDefaultFileExplorer(): String = settings.getString(KEY_DEFAULT_FILE_EXPLORER, "")

    fun setDefaultFileExplorer(pkg: String) {
        settings.putString(KEY_DEFAULT_FILE_EXPLORER, pkg)
    }

    // Bumped whenever a launcher appearance setting (theme / orb / dock pins) changes, so
    // long-lived launcher surfaces (the Foundry home, the Widgets board) can observe it
    // and re-tint live.
    private val _launcherAppearanceFlow = MutableStateFlow(0)
    val launcherAppearanceFlow: StateFlow<Int> = _launcherAppearanceFlow

    private fun bumpLauncherAppearance() {
        _launcherAppearanceFlow.value++
    }

    // Home-screen app widgets the user added to the launcher Widgets page, stored as the
    // list of AppWidget host ids (Android only; empty elsewhere).
    fun getHostedWidgetIds(): List<Int> =
        settings.getString(KEY_HOSTED_WIDGETS, "").split(',').mapNotNull { it.trim().toIntOrNull() }

    fun setHostedWidgetIds(ids: List<Int>) {
        settings.putString(KEY_HOSTED_WIDGETS, ids.joinToString(","))
    }

    // Start orb + app pins. Dock pins and Start-menu pins are independent
    // lists of launcher app ids, fully user-curated.
    fun getLauncherOrbStyle(): String = settings.getString(KEY_LAUNCHER_ORB_STYLE, "orb")

    fun setLauncherOrbStyle(style: String) {
        settings.putString(KEY_LAUNCHER_ORB_STYLE, style)
        bumpLauncherAppearance()
    }

    // Theme that tints the taskbar, Start menu, and widgets window.
    fun getLauncherTheme(): String = settings.getString(KEY_LAUNCHER_THEME, "glass")

    fun setLauncherTheme(id: String) {
        settings.putString(KEY_LAUNCHER_THEME, id)
        bumpLauncherAppearance()
    }

    // Custom photos for the wallpaper and the Start orb (absolute file paths).
    // Empty = use the built-in gradient / orb style.
    fun getLauncherWallpaperImage(): String = settings.getString(KEY_LAUNCHER_WALLPAPER_IMAGE, "")

    fun setLauncherWallpaperImage(path: String) {
        if (path.isBlank()) {
            settings.remove(KEY_LAUNCHER_WALLPAPER_IMAGE)
        } else {
            settings.putString(KEY_LAUNCHER_WALLPAPER_IMAGE, path)
        }
    }

    fun getLauncherOrbImage(): String = settings.getString(KEY_LAUNCHER_ORB_IMAGE, "")

    fun setLauncherOrbImage(path: String) {
        if (path.isBlank()) {
            settings.remove(KEY_LAUNCHER_ORB_IMAGE)
        } else {
            settings.putString(KEY_LAUNCHER_ORB_IMAGE, path)
        }
        bumpLauncherAppearance()
    }

    fun getLauncherDockPins(default: List<String>): List<String> {
        val raw = settings.getString(KEY_LAUNCHER_DOCK_PINS, "")
        return if (raw.isBlank()) default else raw.split(",").filter { it.isNotBlank() }
    }

    fun setLauncherDockPins(ids: List<String>) {
        settings.putString(KEY_LAUNCHER_DOCK_PINS, ids.joinToString(","))
        bumpLauncherAppearance()
    }

    // News sources for the Foundry home news box: RSS/Atom feed URLs. Stored
    // newline-separated (URLs can contain commas). Blank = fall back to defaults.
    fun getNewsFeedUrls(default: List<String>): List<String> {
        val raw = settings.getString(KEY_NEWS_FEED_URLS, "")
        return if (raw.isBlank()) default else raw.split("\n").map { it.trim() }.filter { it.isNotBlank() }
    }

    fun setNewsFeedUrls(urls: List<String>) {
        settings.putString(KEY_NEWS_FEED_URLS, urls.map { it.trim() }.filter { it.isNotBlank() }.joinToString("\n"))
    }

    fun getLauncherStartPins(default: List<String>): List<String> {
        val raw = settings.getString(KEY_LAUNCHER_START_PINS, "")
        return if (raw.isBlank()) default else raw.split(",").filter { it.isNotBlank() }
    }

    fun setLauncherStartPins(ids: List<String>) {
        settings.putString(KEY_LAUNCHER_START_PINS, ids.joinToString(","))
    }

    // Persisted sizes (as fractions of the screen) for resizable launcher
    // surfaces, so a resize sticks across opens.
    fun getStartMenuSize(defaultW: Float, defaultH: Float): Pair<Float, Float> = settings.getFloat(KEY_START_MENU_W, defaultW) to settings.getFloat(KEY_START_MENU_H, defaultH)

    fun setStartMenuSize(w: Float, h: Float) {
        settings.putFloat(KEY_START_MENU_W, w)
        settings.putFloat(KEY_START_MENU_H, h)
    }

    fun getWidgetPanelSize(defaultW: Float, defaultH: Float): Pair<Float, Float> = settings.getFloat(KEY_WIDGET_PANEL_W, defaultW) to settings.getFloat(KEY_WIDGET_PANEL_H, defaultH)

    fun setWidgetPanelSize(w: Float, h: Float) {
        settings.putFloat(KEY_WIDGET_PANEL_W, w)
        settings.putFloat(KEY_WIDGET_PANEL_H, h)
    }

    // Per-icon launch links: a URL, a sandbox file path, or an app package name
    // assigned to a desktop icon. Empty = use the icon's built-in action.
    fun getLauncherIconLink(iconId: String): String = settings.getString("$KEY_LAUNCHER_ICON_LINK_PREFIX$iconId", "")

    fun setLauncherIconLink(iconId: String, target: String) {
        if (target.isBlank()) {
            settings.remove("$KEY_LAUNCHER_ICON_LINK_PREFIX$iconId")
        } else {
            settings.putString("$KEY_LAUNCHER_ICON_LINK_PREFIX$iconId", target)
        }
    }

    // Start-menu quick-launch slots: the app package the user chose for a fixed
    // slot (photos / music / files / settings). Empty = not yet chosen.
    fun getQuickLaunchApp(slot: String): String = settings.getString("$KEY_LAUNCHER_QUICK_LAUNCH_PREFIX$slot", "")

    fun setQuickLaunchApp(slot: String, pkg: String) {
        if (pkg.isBlank()) {
            settings.remove("$KEY_LAUNCHER_QUICK_LAUNCH_PREFIX$slot")
        } else {
            settings.putString("$KEY_LAUNCHER_QUICK_LAUNCH_PREFIX$slot", pkg)
        }
    }

    // Per-app Start-menu category override. Empty = auto-sort by the OS category;
    // a category id pins it there; "none" keeps it out of every named box.
    fun getAppCategoryOverride(pkg: String): String = settings.getString("$KEY_LAUNCHER_APP_CATEGORY_PREFIX$pkg", "")

    fun setAppCategoryOverride(pkg: String, category: String) {
        if (category.isBlank()) {
            settings.remove("$KEY_LAUNCHER_APP_CATEGORY_PREFIX$pkg")
        } else {
            settings.putString("$KEY_LAUNCHER_APP_CATEGORY_PREFIX$pkg", category)
        }
    }

    // Desktop items (user-created icons + folders), stored as a JSON array.
    fun getDesktopItemsJson(): String = settings.getString(KEY_LAUNCHER_DESKTOP_ITEMS, "[]")

    fun setDesktopItemsJson(json: String) {
        settings.putString(KEY_LAUNCHER_DESKTOP_ITEMS, json)
    }

    fun getScheduledTasksJson(): String = settings.getString(KEY_SCHEDULED_TASKS, "[]")

    fun setScheduledTasksJson(json: String) {
        settings.putString(KEY_SCHEDULED_TASKS, json)
    }

    // Heartbeat config
    fun getHeartbeatConfigJson(): String = settings.getString(KEY_HEARTBEAT_CONFIG, "")

    fun setHeartbeatConfigJson(json: String) {
        settings.putString(KEY_HEARTBEAT_CONFIG, json)
    }

    // Heartbeat log
    fun getHeartbeatLogJson(): String = settings.getString(KEY_HEARTBEAT_LOG, "")

    fun setHeartbeatLogJson(json: String) {
        settings.putString(KEY_HEARTBEAT_LOG, json)
    }

    // Heartbeat prompt
    fun getHeartbeatPrompt(): String = settings.getString(KEY_HEARTBEAT_PROMPT, "")

    fun setHeartbeatPrompt(text: String) {
        settings.putString(KEY_HEARTBEAT_PROMPT, text)
    }

    // Budget governor config (daily token cap, auto-pause, manual kill switch)
    fun getBudgetConfigJson(): String = settings.getString(KEY_BUDGET_CONFIG, "")

    fun setBudgetConfigJson(json: String) {
        settings.putString(KEY_BUDGET_CONFIG, json)
    }

    // Budget usage accounting (current local day's token counters)
    fun getBudgetUsageJson(): String = settings.getString(KEY_BUDGET_USAGE, "")

    fun setBudgetUsageJson(json: String) {
        settings.putString(KEY_BUDGET_USAGE, json)
    }

    // MCP Servers
    fun getMcpServersJson(): String = settings.getString(KEY_MCP_SERVERS, "")

    fun setMcpServersJson(json: String) {
        settings.putString(KEY_MCP_SERVERS, json)
    }

    // UI Scale
    private val _uiScaleFlow = MutableStateFlow(settings.getFloat(KEY_UI_SCALE, defaultUiScale))
    val uiScaleFlow: StateFlow<Float> = _uiScaleFlow

    fun getUiScale(): Float = _uiScaleFlow.value

    fun setUiScale(scale: Float) {
        settings.putFloat(KEY_UI_SCALE, scale)
        _uiScaleFlow.value = scale
    }

    // Email
    fun isEmailEnabled(): Boolean = settings.getBoolean(KEY_EMAIL_ENABLED, true)

    fun setEmailEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_EMAIL_ENABLED, enabled)
    }

    fun getEmailAccountsJson(): String = settings.getString(KEY_EMAIL_ACCOUNTS, "")

    fun setEmailAccountsJson(json: String) {
        settings.putString(KEY_EMAIL_ACCOUNTS, json)
    }

    fun getEmailPassword(accountId: String): String = settings.getString("${KEY_EMAIL_PASSWORD_PREFIX}$accountId", "")

    fun setEmailPassword(accountId: String, password: String) {
        settings.putString("${KEY_EMAIL_PASSWORD_PREFIX}$accountId", password)
    }

    fun removeEmailPassword(accountId: String) {
        settings.remove("${KEY_EMAIL_PASSWORD_PREFIX}$accountId")
    }

    fun getEmailSyncStateJson(accountId: String): String = settings.getString("${KEY_EMAIL_SYNC_PREFIX}$accountId", "")

    fun setEmailSyncStateJson(accountId: String, json: String) {
        settings.putString("${KEY_EMAIL_SYNC_PREFIX}$accountId", json)
    }

    fun getEmailPollIntervalMinutes(): Int = settings.getInt(KEY_EMAIL_POLL_INTERVAL, 15)

    fun setEmailPollIntervalMinutes(minutes: Int) {
        settings.putInt(KEY_EMAIL_POLL_INTERVAL, minutes)
    }

    fun getEmailPendingJson(): String = settings.getString(KEY_EMAIL_PENDING, "")

    fun setEmailPendingJson(json: String) {
        settings.putString(KEY_EMAIL_PENDING, json)
    }

    // Outreach guardrails — dedup set of already-contacted addresses + a per-local-day
    // send counter, so autonomous cold outreach can't double-contact or exceed the cap.
    fun getOutreachContactedJson(): String = settings.getString(KEY_OUTREACH_CONTACTED, "")

    fun setOutreachContactedJson(json: String) {
        settings.putString(KEY_OUTREACH_CONTACTED, json)
    }

    fun getOutreachDailyJson(): String = settings.getString(KEY_OUTREACH_DAILY, "")

    fun setOutreachDailyJson(json: String) {
        settings.putString(KEY_OUTREACH_DAILY, json)
    }

    fun getOutreachDailyCap(): Int = settings.getInt(KEY_OUTREACH_DAILY_CAP, 5)

    fun setOutreachDailyCap(cap: Int) {
        settings.putInt(KEY_OUTREACH_DAILY_CAP, cap)
    }

    // SMS (FOSS-only, Android-only — settings layer is platform-agnostic, feature gate
    // is enforced by the READ_SMS permission being declared only in foss/AndroidManifest.xml)
    fun isSmsEnabled(): Boolean = settings.getBoolean(KEY_SMS_ENABLED, false)

    fun setSmsEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_SMS_ENABLED, enabled)
    }

    fun getSmsPollIntervalMinutes(): Int = settings.getInt(KEY_SMS_POLL_INTERVAL, 15)

    fun setSmsPollIntervalMinutes(minutes: Int) {
        settings.putInt(KEY_SMS_POLL_INTERVAL, minutes)
    }

    fun getSmsPendingJson(): String = settings.getString(KEY_SMS_PENDING, "")

    fun setSmsPendingJson(json: String) {
        settings.putString(KEY_SMS_PENDING, json)
    }

    fun getSmsSyncStateJson(): String = settings.getString(KEY_SMS_SYNC_STATE, "")

    fun setSmsSyncStateJson(json: String) {
        settings.putString(KEY_SMS_SYNC_STATE, json)
    }

    fun isSmsSendEnabled(): Boolean = settings.getBoolean(KEY_SMS_SEND_ENABLED, false)

    fun setSmsSendEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_SMS_SEND_ENABLED, enabled)
    }

    fun getSmsDraftsJson(): String = settings.getString(KEY_SMS_DRAFTS, "")

    fun setSmsDraftsJson(json: String) {
        settings.putString(KEY_SMS_DRAFTS, json)
    }

    // Notifications (FOSS-only, Android-only — settings layer is platform-agnostic, feature
    // gate is enforced by the listener service being declared only in foss/AndroidManifest.xml)
    fun isNotificationsEnabled(): Boolean = settings.getBoolean(KEY_NOTIFICATIONS_ENABLED, false)

    fun setNotificationsEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled)
    }

    fun getNotificationsPendingJson(): String = settings.getString(KEY_NOTIFICATIONS_PENDING, "")

    fun setNotificationsPendingJson(json: String) {
        settings.putString(KEY_NOTIFICATIONS_PENDING, json)
    }

    fun getNotificationsStoreJson(): String = settings.getString(KEY_NOTIFICATIONS_STORE, "")

    fun setNotificationsStoreJson(json: String) {
        settings.putString(KEY_NOTIFICATIONS_STORE, json)
    }

    fun getNotificationsSyncStateJson(): String = settings.getString(KEY_NOTIFICATIONS_SYNC_STATE, "")

    fun setNotificationsSyncStateJson(json: String) {
        settings.putString(KEY_NOTIFICATIONS_SYNC_STATE, json)
    }

    // Local model context size
    fun getModelContextTokens(modelId: String): Int = settings.getInt("$KEY_MODEL_CONTEXT_PREFIX$modelId", 0)

    fun setModelContextTokens(modelId: String, contextTokens: Int) {
        settings.putInt("$KEY_MODEL_CONTEXT_PREFIX$modelId", contextTokens)
    }

    // Splinterlands
    fun isSplinterlandsEnabled(): Boolean = settings.getBoolean(KEY_SPLINTERLANDS_ENABLED, false)

    fun setSplinterlandsEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_SPLINTERLANDS_ENABLED, enabled)
    }

    fun getSplinterlandsAccountJson(): String = settings.getString(KEY_SPLINTERLANDS_ACCOUNT, "")

    fun setSplinterlandsAccountJson(json: String) {
        settings.putString(KEY_SPLINTERLANDS_ACCOUNT, json)
    }

    fun getSplinterlandsPostingKey(): String = settings.getString(KEY_SPLINTERLANDS_POSTING_KEY, "")

    fun getSplinterlandsPostingKey(accountId: String): String = settings.getString("${KEY_SPLINTERLANDS_POSTING_KEY}_$accountId", "")
        .ifEmpty { getSplinterlandsPostingKey() } // fallback to legacy key

    fun setSplinterlandsPostingKey(accountId: String, key: String) {
        settings.putString("${KEY_SPLINTERLANDS_POSTING_KEY}_$accountId", key)
    }

    fun getSplinterlandsInstanceId(): String = settings.getString(KEY_SPLINTERLANDS_INSTANCE_ID, "")

    fun setSplinterlandsInstanceId(instanceId: String) {
        settings.putString(KEY_SPLINTERLANDS_INSTANCE_ID, instanceId)
    }

    fun getSplinterlandsInstanceIdsJson(): String = settings.getString(KEY_SPLINTERLANDS_INSTANCE_IDS, "")

    fun setSplinterlandsInstanceIdsJson(json: String) {
        settings.putString(KEY_SPLINTERLANDS_INSTANCE_IDS, json)
    }

    fun getSplinterlandsBattleLogJson(): String = settings.getString(KEY_SPLINTERLANDS_BATTLE_LOG, "")

    fun setSplinterlandsBattleLogJson(json: String) {
        settings.putString(KEY_SPLINTERLANDS_BATTLE_LOG, json)
    }

    companion object {
        const val KEY_CURRENT_SERVICE_ID = "current_service_id"
        const val KEY_APP_OPENS = "app_opens"
        const val KEY_WELCOME_SEEN = "welcome_seen"
        const val KEY_SETUP_SEEN = "setup_wizard_seen"
        const val KEY_GUIDE_SEEN = "settings_guide_seen"
        const val KEY_OFFERED_ON_DEVICE_AI = "offered_on_device_ai"

        const val KEY_CONVERSATIONS = "conversations_json"
        const val KEY_CURRENT_CONVERSATION_ID = "current_conversation_id"
        const val KEY_CURRENT_INTERACTIVE_MODE = "current_interactive_mode"
        const val KEY_CURRENT_CONVERSATION_MIGRATED = "current_conversation_migrated"
        const val KEY_ENCRYPTION_KEY = "encryption_key"
        const val KEY_MIGRATION_COMPLETE = "migration_complete_v1"
        const val KEY_TOOL_PREFIX = "tool_enabled_"
        const val KEY_SOUL = "soul_text"
        const val KEY_MEMORY_ENABLED = "memory_enabled"
        const val KEY_MEMORY_INSTRUCTIONS = "memory_instructions"
        const val KEY_AGENT_MEMORIES = "agent_memories"
        const val KEY_SCHEDULED_TASKS = "scheduled_tasks"
        const val KEY_SCHEDULING_ENABLED = "scheduling_enabled"
        const val KEY_DYNAMIC_UI_ENABLED = "dynamic_ui_enabled"
        const val KEY_CHAT_ENGINE_PERSISTENT = "chat_engine_persistent"
        const val KEY_OLED_MODE_ENABLED = "oled_mode_enabled"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_DAEMON_ENABLED = "daemon_enabled"
        const val KEY_HEARTBEAT_CONFIG = "heartbeat_config"
        const val KEY_HEARTBEAT_PROMPT = "heartbeat_prompt"
        const val KEY_HEARTBEAT_LOG = "heartbeat_log"

        const val KEY_BUDGET_CONFIG = "budget_config"
        const val KEY_BUDGET_USAGE = "budget_usage"

        const val KEY_EMAIL_ENABLED = "email_enabled"
        const val KEY_EMAIL_ACCOUNTS = "email_accounts"
        const val KEY_EMAIL_PASSWORD_PREFIX = "email_password_"
        const val KEY_EMAIL_SYNC_PREFIX = "email_sync_"
        const val KEY_EMAIL_POLL_INTERVAL = "email_poll_interval"
        const val KEY_EMAIL_PENDING = "email_pending"

        const val KEY_OUTREACH_CONTACTED = "outreach_contacted"
        const val KEY_OUTREACH_DAILY = "outreach_daily"
        const val KEY_OUTREACH_DAILY_CAP = "outreach_daily_cap"

        const val KEY_SMS_ENABLED = "sms_enabled"
        const val KEY_SMS_POLL_INTERVAL = "sms_poll_interval"
        const val KEY_SMS_PENDING = "sms_pending"
        const val KEY_SMS_SYNC_STATE = "sms_sync_state"
        const val KEY_SMS_SEND_ENABLED = "sms_send_enabled"
        const val KEY_SMS_DRAFTS = "sms_drafts"

        const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        const val KEY_NOTIFICATIONS_PENDING = "notifications_pending"
        const val KEY_NOTIFICATIONS_STORE = "notifications_store"
        const val KEY_NOTIFICATIONS_SYNC_STATE = "notifications_sync_state"
        const val KEY_CONFIGURED_SERVICES = "configured_services"
        const val KEY_PROJECTS = "projects_v1"
        const val KEY_ACTIVE_PROJECT_ID = "active_project_id"
        const val KEY_FREE_FALLBACK_ENABLED = "free_fallback_enabled"
        const val KEY_FREE_MODE = "free_mode"
        const val KEY_FREE_SERVICE_PRIMARY = "free_service_primary"
        const val KEY_SERVICES_MIGRATION_COMPLETE = "services_migration_complete_v1"
        const val KEY_UI_SCALE = "ui_scale"
        const val KEY_MCP_SERVERS = "mcp_servers"
        const val KEY_INSTANCE_MIGRATION_COMPLETE = "instance_migration_complete_v1"
        const val KEY_BASE_URL_V1_MIGRATION_COMPLETE = "base_url_v1_migration_complete"

        const val KEY_SPLINTERLANDS_ENABLED = "splinterlands_enabled"
        const val KEY_SPLINTERLANDS_ACCOUNT = "splinterlands_account"
        const val KEY_SPLINTERLANDS_POSTING_KEY = "splinterlands_posting_key"
        const val KEY_SPLINTERLANDS_BATTLE_LOG = "splinterlands_battle_log"
        const val KEY_SPLINTERLANDS_INSTANCE_ID = "splinterlands_instance_id"
        const val KEY_SPLINTERLANDS_INSTANCE_IDS = "splinterlands_instance_ids"

        const val KEY_MODEL_CONTEXT_PREFIX = "model_context_"

        const val KEY_SANDBOX_ENABLED = "sandbox_enabled"

        const val KEY_LAUNCHER_WALLPAPER = "launcher_wallpaper"
        const val KEY_LAUNCHER_LABELS = "launcher_labels"
        const val KEY_FULLSCREEN_LAUNCHER = "launcher_fullscreen_immersive"
        const val KEY_WINDOWED_APPS = "launcher_windowed_apps"
        const val KEY_LAUNCHER_NOTE = "launcher_note"
        const val KEY_DEFAULT_FILE_EXPLORER = "default_file_explorer"
        const val KEY_LAUNCHER_ICON_LINK_PREFIX = "launcher_icon_link_"
        const val KEY_LAUNCHER_QUICK_LAUNCH_PREFIX = "launcher_quick_launch_"
        const val KEY_LAUNCHER_APP_CATEGORY_PREFIX = "launcher_app_category_"
        const val KEY_LAUNCHER_DESKTOP_ITEMS = "launcher_desktop_items"
        const val KEY_LAUNCHER_ORB_STYLE = "launcher_orb_style"
        const val KEY_LAUNCHER_THEME = "launcher_theme"
        const val KEY_LAUNCHER_WALLPAPER_IMAGE = "launcher_wallpaper_image"
        const val KEY_LAUNCHER_ORB_IMAGE = "launcher_orb_image"
        const val KEY_LAUNCHER_DOCK_PINS = "launcher_dock_pins"
        const val KEY_LAUNCHER_START_PINS = "launcher_start_pins"
        const val KEY_NEWS_FEED_URLS = "news_feed_urls"
        const val KEY_START_MENU_W = "launcher_start_menu_w"
        const val KEY_START_MENU_H = "launcher_start_menu_h"
        const val KEY_WIDGET_PANEL_W = "launcher_widget_panel_w"
        const val KEY_WIDGET_PANEL_H = "launcher_widget_panel_h"
        const val KEY_HOSTED_WIDGETS = "launcher_hosted_widgets"

        // Basic memory guidance shared by every chat variant. The advanced `## Structured
        // Learning` block lives in `ChatSystemPromptBuilder.DEFAULT_STRUCTURED_LEARNING_SECTION`
        // and is composed in only for the remote variant.
        const val DEFAULT_MEMORY_INSTRUCTIONS =
            "You have persistent memory across conversations. " +
                "All your stored memories are listed in the system prompt grouped by category.\n\n" +
                "When you learn important information about the user (name, preferences, projects, goals, etc.), " +
                "proactively use the memory_store tool to save it.\n" +
                "Write each memory tersely — the key facts and keywords only, no filler or full sentences — so it stays quick to scan.\n" +
                "Use the memory_forget tool to remove outdated or incorrect memories.\n" +
                "Do not store trivial or transient information."
    }
}
