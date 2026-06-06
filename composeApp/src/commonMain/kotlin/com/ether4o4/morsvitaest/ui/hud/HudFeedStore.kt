package com.ether4o4.morsvitaest.ui.hud

import com.ether4o4.morsvitaest.data.AppSettings
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * One row in the HUD's curated feed at the top of the home screen. Users
 * paste URLs via the "+" button in the feed header; entries persist
 * across launches via [AppSettings.getHudFeedItems] /
 * [AppSettings.setHudFeedItems]. Thumbnails are procedural — we colour
 * a placeholder by [accentHex] derived from the URL host so each card
 * still reads distinct without us fetching og:image.
 */
@Serializable
data class HudFeedItem(
    val id: String,
    val url: String,
    val label: String = "",
    @SerialName("accent") val accentHex: Long = 0xFFE53935L,
    @SerialName("added_at") val addedAt: Long = 0L,
)

private val feedJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private const val HUD_FEED_KEY = "hud_feed_items_v1"

fun AppSettings.getHudFeedItems(): List<HudFeedItem> {
    val raw = settings.getString(HUD_FEED_KEY, "")
    if (raw.isBlank()) return defaultHudFeed
    return runCatching { feedJson.decodeFromString<List<HudFeedItem>>(raw) }
        .getOrDefault(defaultHudFeed)
}

fun AppSettings.setHudFeedItems(items: List<HudFeedItem>) {
    settings.putString(HUD_FEED_KEY, feedJson.encodeToString(items))
}

/**
 * Seeded list that ships on fresh installs so the feed isn't a blank
 * card on first open. Users can remove any of these from the share
 * sheet's "Remove" action.
 */
private val defaultHudFeed = listOf(
    HudFeedItem(
        id = "seed-mve-release",
        url = "https://github.com/ether4o4/MorsVitaEst/releases/tag/android-preview-latest",
        label = "MVE preview build",
        accentHex = 0xFFE53935L,
    ),
    HudFeedItem(
        id = "seed-dolphin",
        url = "https://huggingface.co/cognitivecomputations/Dolphin3.0-Llama3.2-3B-GGUF",
        label = "Dolphin 3.0 (Llama 3.2 3B)",
        accentHex = 0xFF7CB342L,
    ),
    HudFeedItem(
        id = "seed-llama-cpp",
        url = "https://github.com/ggerganov/llama.cpp",
        label = "llama.cpp",
        accentHex = 0xFFFFB300L,
    ),
    HudFeedItem(
        id = "seed-mcp",
        url = "https://modelcontextprotocol.io",
        label = "Model Context Protocol",
        accentHex = 0xFF42A5F5L,
    ),
    HudFeedItem(
        id = "seed-awesome-mcp",
        url = "https://github.com/punkpeye/awesome-mcp-servers",
        label = "Awesome MCP servers",
        accentHex = 0xFFAB47BCL,
    ),
)

/**
 * Pick a stable accent colour for a brand-new URL. We hash the host
 * and map into a small palette so adding the same domain twice gets the
 * same colour, but two different domains rarely collide.
 */
fun accentForUrl(url: String): Long {
    val host = runCatching {
        url.substringAfter("://").substringBefore("/").lowercase()
    }.getOrDefault(url.lowercase())
    val palette = listOf(
        0xFFE53935L, // red
        0xFF7CB342L, // green
        0xFFFFB300L, // amber
        0xFF42A5F5L, // blue
        0xFFAB47BCL, // purple
        0xFFFF7043L, // deep orange
        0xFF26A69AL, // teal
        0xFFEC407AL, // pink
    )
    val hash = host.fold(0) { acc, ch -> acc * 31 + ch.code }
    return palette[((hash % palette.size) + palette.size) % palette.size]
}

/**
 * Short label for a URL when the user didn't provide one — host minus
 * leading "www." so cards say "github.com" instead of the full path.
 */
fun shortLabelFor(url: String): String {
    val trimmed = url.trim()
    if (trimmed.isBlank()) return "Untitled"
    val host = trimmed
        .substringAfter("://", trimmed)
        .substringBefore("/")
        .removePrefix("www.")
    return host.ifBlank { trimmed }
}
