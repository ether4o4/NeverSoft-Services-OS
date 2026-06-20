package com.ether4o4.morsvitaest.data

import com.ether4o4.morsvitaest.httpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * One news story pulled from an RSS/Atom feed. [imageUrl] is the article's own
 * hero/thumbnail image (from the feed's media/enclosure/inline-img markup) — not
 * the website's favicon — so the news box shows real per-story pictures.
 */
data class NewsArticle(
    val title: String,
    val source: String,
    val summary: String,
    val link: String?,
    val imageUrl: String?,
)

/**
 * Fetches the Foundry home news box. Each source is an RSS or Atom feed URL; the
 * box is a manual-refresh aggregation of their newest stories with real article
 * thumbnails. Fetching and XML parsing happen off the main thread; a failed feed
 * is skipped rather than failing the whole refresh.
 */
class NewsRepository {

    private val client = httpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
        }
    }

    /** Pull and merge every [feedUrls] source, newest-ish first, capped at [limit]. */
    suspend fun fetch(feedUrls: List<String>, limit: Int = 30): List<NewsArticle> = withContext(Dispatchers.Default) {
        val sources = feedUrls.ifEmpty { DEFAULT_FEEDS }
        val perFeed = coroutineScope {
            sources.map { url -> async { runCatching { fetchOne(url) }.getOrDefault(emptyList()) } }.awaitAll()
        }
        // Round-robin the feeds so one chatty source doesn't crowd out the rest,
        // then de-dupe by link/title and cap the total.
        val seen = HashSet<String>()
        val merged = ArrayList<NewsArticle>()
        var index = 0
        while (merged.size < limit) {
            var addedThisPass = false
            for (feed in perFeed) {
                if (index < feed.size) {
                    val article = feed[index]
                    val key = (article.link ?: article.title).trim().lowercase()
                    if (key.isNotEmpty() && seen.add(key)) merged.add(article)
                    addedThisPass = true
                }
            }
            if (!addedThisPass) break
            index++
        }
        merged.take(limit)
    }

    private suspend fun fetchOne(url: String): List<NewsArticle> {
        val xml = client.get(url) {
            header("User-Agent", USER_AGENT)
            header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, */*")
        }.bodyAsText()
        return parseFeed(xml)
    }

    companion object {
        /** Image-rich tech/AI feeds used until the user adds their own sources. */
        val DEFAULT_FEEDS = listOf(
            "https://www.theverge.com/rss/index.xml",
            "https://feeds.arstechnica.com/arstechnica/index",
            "https://www.wired.com/feed/rss",
        )

        private const val USER_AGENT = "Mozilla/5.0 (compatible; MorsVitaEst/1.0)"
    }
}

// ───────────────────────── XML parsing (regex, no DOM in commonMain) ─────────

private val ITEM_BLOCK = Regex("<(item|entry)\\b[\\s\\S]*?</\\1>", RegexOption.IGNORE_CASE)
private val TITLE_TAG = Regex("<title\\b[^>]*>([\\s\\S]*?)</title>", RegexOption.IGNORE_CASE)
private val RSS_LINK_TAG = Regex("<link\\b[^>]*>([\\s\\S]*?)</link>", RegexOption.IGNORE_CASE)
private val ATOM_LINK_HREF = Regex("<link\\b[^>]*\\bhref=\"([^\"]+)\"[^>]*/?>", RegexOption.IGNORE_CASE)
private val DESCRIPTION_TAG = Regex("<(description|summary|content[^>]*)>([\\s\\S]*?)</(?:description|summary|content)>", RegexOption.IGNORE_CASE)
private val CHANNEL_TITLE = Regex("<(?:channel|feed)\\b[\\s\\S]*?<title\\b[^>]*>([\\s\\S]*?)</title>", RegexOption.IGNORE_CASE)
private val CDATA = Regex("<!\\[CDATA\\[([\\s\\S]*?)]]>")
private val HTML_TAG = Regex("<[^>]+>")
private val WHITESPACE = Regex("\\s+")

// Image extraction, in priority order.
private val MEDIA_CONTENT = Regex("<media:content\\b[^>]*\\burl=\"([^\"]+)\"", RegexOption.IGNORE_CASE)
private val MEDIA_THUMBNAIL = Regex("<media:thumbnail\\b[^>]*\\burl=\"([^\"]+)\"", RegexOption.IGNORE_CASE)
private val ENCLOSURE = Regex("<enclosure\\b[^>]*>", RegexOption.IGNORE_CASE)
private val ENCLOSURE_URL = Regex("\\burl=\"([^\"]+)\"", RegexOption.IGNORE_CASE)
private val IMG_SRC = Regex("<img\\b[^>]*\\bsrc=\"([^\"]+)\"", RegexOption.IGNORE_CASE)
private val IMAGE_EXT = Regex("\\.(?:png|jpe?g|webp|gif|avif)(?:\\?|$)", RegexOption.IGNORE_CASE)

internal fun parseFeed(xml: String): List<NewsArticle> {
    val source = CHANNEL_TITLE.find(xml)?.groupValues?.getOrNull(1)?.let { cleanText(it) }?.takeIf { it.isNotBlank() }
        ?: "News"
    return ITEM_BLOCK.findAll(xml).map { match ->
        val block = match.value
        val title = TITLE_TAG.find(block)?.groupValues?.getOrNull(1)?.let { cleanText(it) }.orEmpty()
        val rawDescription = DESCRIPTION_TAG.find(block)?.groupValues?.lastOrNull().orEmpty()
        val summary = cleanText(rawDescription).take(240)
        val link = extractLink(block)
        val imageUrl = extractImage(block, rawDescription)
        NewsArticle(title = title, source = source, summary = summary, link = link, imageUrl = imageUrl)
    }.filter { it.title.isNotBlank() }.take(20).toList()
}

private fun extractLink(block: String): String? {
    val rss = RSS_LINK_TAG.find(block)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.startsWith("http") }
    if (rss != null) return decodeUrl(rss)
    return ATOM_LINK_HREF.find(block)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.startsWith("http") }?.let(::decodeUrl)
}

private fun extractImage(block: String, descriptionHtml: String): String? {
    MEDIA_CONTENT.find(block)?.groupValues?.getOrNull(1)?.let { if (it.startsWith("http")) return decodeUrl(it) }
    MEDIA_THUMBNAIL.find(block)?.groupValues?.getOrNull(1)?.let { if (it.startsWith("http")) return decodeUrl(it) }
    ENCLOSURE.find(block)?.value?.let { tag ->
        if (tag.contains("image", ignoreCase = true) || IMAGE_EXT.containsMatchIn(tag)) {
            ENCLOSURE_URL.find(tag)?.groupValues?.getOrNull(1)?.let { if (it.startsWith("http")) return decodeUrl(it) }
        }
    }
    // Inline <img> inside the (possibly CDATA-wrapped) description / content HTML.
    val html = CDATA.replace(descriptionHtml) { it.groupValues[1] }
    IMG_SRC.find(html)?.groupValues?.getOrNull(1)?.let { if (it.startsWith("http")) return decodeUrl(it) }
    return null
}

/** XML attributes escape `&` as `&amp;`; undo that (and a couple of friends) so
 *  query strings in image/article URLs aren't broken. */
private fun decodeUrl(url: String): String = url
    .replace("&amp;", "&")
    .replace("&#38;", "&")
    .replace("&apos;", "'")

/** Unwrap CDATA, drop HTML tags, decode the handful of common entities, collapse whitespace. */
private fun cleanText(raw: String): String {
    val unwrapped = CDATA.replace(raw) { it.groupValues[1] }
    val stripped = HTML_TAG.replace(unwrapped, " ")
    val decoded = stripped
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&nbsp;", " ")
        .replace("&hellip;", "…")
        .replace("&mdash;", "—")
        .replace("&ndash;", "–")
    return WHITESPACE.replace(decoded, " ").trim()
}
