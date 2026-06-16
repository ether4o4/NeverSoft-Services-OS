package com.ether4o4.morsvitaest.ui.launcher

import com.ether4o4.morsvitaest.data.AppSettings
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * A user-created desktop item — a folder or a launchable shortcut. The whole set
 * is persisted as a JSON list in [AppSettings].
 *
 * - [target]: what a shortcut launches — a URL, a sandbox path, or an app package.
 * - [imagePath]: a saved photo to use as the icon (wins over everything).
 * - [iconId]: a built-in icon id (used when there's no photo).
 * - with neither set, an app-package shortcut shows that installed app's own icon.
 * - [parent]: the containing folder's id, or "" for the desktop root.
 */
@Serializable
internal data class DesktopItem(
    val id: String,
    val isFolder: Boolean = false,
    val label: String = "",
    val target: String = "",
    val imagePath: String = "",
    val iconId: String = "",
    val parent: String = "",
)

private val desktopItemsJson = Json { ignoreUnknownKeys = true }

internal fun AppSettings.loadDesktopItems(): List<DesktopItem> = try {
    desktopItemsJson.decodeFromString(getDesktopItemsJson())
} catch (_: Exception) {
    emptyList()
}

internal fun AppSettings.saveDesktopItems(items: List<DesktopItem>) {
    setDesktopItemsJson(desktopItemsJson.encodeToString(items))
}

/** Appends a launchable shortcut to the desktop (or a folder) and persists it. */
internal fun AppSettings.addDesktopShortcut(target: String, label: String, parent: String = "") {
    val item = DesktopItem(
        id = "item_${Random.nextLong()}",
        isFolder = false,
        label = label,
        target = target,
        parent = parent,
    )
    saveDesktopItems(loadDesktopItems() + item)
}
