package com.ether4o4.morsvitaest.ui.launcher

import com.ether4o4.morsvitaest.InstalledApp
import com.ether4o4.morsvitaest.data.AppSettings

/**
 * The "All apps" buckets shown in the redesigned Start menu. [Connect],
 * [Discover], [Utilities], [Create] and [Media] are the five real categories an
 * installed app can be sorted into; everything also appears under the catch-all
 * "All" box, which is not a value here because it never owns an app exclusively.
 */
internal enum class AppCategory(val id: String, val label: String) {
    Connect("connect", "Connect"),
    Discover("discover", "Discover"),
    Utilities("utilities", "Utilities"),
    Create("create", "Create"),
    Media("media", "Media"),
    ;

    companion object {
        fun fromId(id: String): AppCategory? = entries.firstOrNull { it.id == id }
    }
}

/**
 * Sentinel override stored when the user deliberately moves an app out of every
 * named box — it then shows only under "All".
 */
internal const val UNCATEGORIZED_ID = "none"

/**
 * Maps Android's `ApplicationInfo.category` (API 26+) to one of our buckets.
 * The integers mirror the platform constants so commonMain stays free of the
 * Android SDK. `CATEGORY_UNDEFINED` (-1) and anything we don't place return null,
 * so the app lives only under "All" until the user sorts it by hand.
 */
internal fun autoCategoryFor(androidCategory: Int): AppCategory? = when (androidCategory) {
    4, 5 -> AppCategory.Connect // SOCIAL, NEWS
    6 -> AppCategory.Discover // MAPS
    7, 8 -> AppCategory.Utilities // PRODUCTIVITY, ACCESSIBILITY
    3 -> AppCategory.Create // IMAGE
    0, 1, 2 -> AppCategory.Media // GAME, AUDIO, VIDEO
    else -> null
}

/**
 * The bucket an app currently belongs to: a user override wins, then the
 * auto-sort from the OS category, otherwise null (only under "All").
 */
internal fun effectiveCategory(app: InstalledApp, settings: AppSettings): AppCategory? =
    when (val override = settings.getAppCategoryOverride(app.packageName)) {
        "" -> autoCategoryFor(app.category)
        UNCATEGORIZED_ID -> null
        else -> AppCategory.fromId(override)
    }
