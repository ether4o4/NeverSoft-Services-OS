package com.ether4o4.morsvitaest.ui.foundry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ether4o4.morsvitaest.data.AppSettings
import com.ether4o4.morsvitaest.data.Conversation
import com.ether4o4.morsvitaest.data.DataRepository
import com.ether4o4.morsvitaest.data.NewsArticle
import com.ether4o4.morsvitaest.data.NewsRepository
import com.ether4o4.morsvitaest.data.TaskScheduler
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FoundryHomeUiState(
    // Heartbeat box (bottom): the assistant's "what's new" updates.
    val feed: ImmutableList<FoundryFeedItem> = persistentListOf(),
    val isRefreshing: Boolean = false,
    // News box (top): real stories with article thumbnails from RSS/Atom sources.
    val news: ImmutableList<FoundryFeedItem> = persistentListOf(),
    val isNewsRefreshing: Boolean = false,
    val newsSources: ImmutableList<String> = persistentListOf(),
)

/**
 * Backs the Foundry home. Two independent boxes:
 *  - News: a manual-refresh aggregation of the user's RSS/Atom sources, each story
 *    carrying its own article thumbnail. Refresh is on-demand (no auto-polling).
 *  - Heartbeat: each assistant message in the dedicated heartbeat conversation is a
 *    "what's new" card, newest first; pull/tap refresh runs a heartbeat on demand.
 */
class FoundryHomeViewModel(
    private val repository: DataRepository,
    private val taskScheduler: TaskScheduler,
    private val newsRepository: NewsRepository,
    private val settings: AppSettings,
) : ViewModel() {

    private val _state = MutableStateFlow(FoundryHomeUiState())
    val state: StateFlow<FoundryHomeUiState> = _state.asStateFlow()

    init {
        repository.loadConversations()
        viewModelScope.launch {
            repository.savedConversations.collect { conversations ->
                _state.update { it.copy(feed = conversations.toFeed()) }
            }
        }
        _state.update { it.copy(newsSources = currentSources().toImmutableList()) }
        // One fetch on open so the box isn't empty; further refreshes are manual.
        refreshNews()
    }

    // region News box

    fun refreshNews() {
        if (_state.value.isNewsRefreshing) return
        viewModelScope.launch {
            _state.update { it.copy(isNewsRefreshing = true) }
            val articles = try {
                newsRepository.fetch(currentSources())
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                emptyList()
            }
            _state.update { state ->
                state.copy(
                    isNewsRefreshing = false,
                    // Keep whatever we already have if a refresh comes back empty (offline).
                    news = if (articles.isEmpty() && state.news.isNotEmpty()) {
                        state.news
                    } else {
                        articles.map { it.toFeedItem() }.toImmutableList()
                    },
                )
            }
        }
    }

    fun addNewsSource(url: String) {
        val cleaned = url.trim()
        if (cleaned.isBlank() || !cleaned.startsWith("http")) return
        val updated = (currentSources() + cleaned).distinct()
        settings.setNewsFeedUrls(updated)
        _state.update { it.copy(newsSources = updated.toImmutableList()) }
        refreshNews()
    }

    fun removeNewsSource(url: String) {
        val updated = currentSources().filterNot { it == url }
        settings.setNewsFeedUrls(updated)
        _state.update { it.copy(newsSources = updated.toImmutableList()) }
        refreshNews()
    }

    private fun currentSources(): List<String> = settings.getNewsFeedUrls(NewsRepository.DEFAULT_FEEDS)

    private fun NewsArticle.toFeedItem(): FoundryFeedItem = FoundryFeedItem(
        title = title,
        source = source,
        summary = summary,
        thumbnailUrl = imageUrl,
        link = link,
    )

    // endregion

    // region Heartbeat box

    fun refresh() {
        if (_state.value.isRefreshing) return
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            try {
                taskScheduler.triggerHeartbeatNow()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // A failed/disabled heartbeat shouldn't break the home; just reload what we have.
            }
            repository.loadConversations()
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private fun List<Conversation>.toFeed(): ImmutableList<FoundryFeedItem> {
        val heartbeat = firstOrNull { it.type == Conversation.TYPE_HEARTBEAT } ?: return persistentListOf()
        return heartbeat.messages
            .filter { it.role == "assistant" && it.content.isNotBlank() }
            .asReversed()
            .map { it.toFeedItem() }
            .toImmutableList()
    }

    private fun Conversation.Message.toFeedItem(): FoundryFeedItem {
        val thumbnailUrl = extractFeedThumbnailUrl(content)
        // Strip image/link markup so the title and summary read as plain text.
        val cleaned = stripFeedMarkdownMedia(content).trim()
        val lines = cleaned.lineSequence().filter { it.isNotBlank() }.toList()
        val title = lines.firstOrNull()
            ?.trimStart('#', '*', '-', ' ')
            ?.take(120)
            ?.ifBlank { "Heartbeat update" }
            ?: "Heartbeat update"
        val summary = lines.drop(1).joinToString(" ").ifBlank { cleaned }.take(240)
        return FoundryFeedItem(title = title, source = "Heartbeat", summary = summary, thumbnailUrl = thumbnailUrl)
    }

    // endregion
}

// ![alt](https://host/pic.png) — grab the URL inside a markdown image.
private val MARKDOWN_IMAGE = Regex("""!\[[^\]]*]\((https?://[^)\s]+)\)""")

// A bare image link, e.g. https://host/a/b.jpg?x=1 — common in RSS/heartbeat text.
private val IMAGE_URL = Regex(
    """https?://[^\s)"']+\.(?:png|jpe?g|webp|gif|avif|svg|bmp)(?:\?[^\s)"']*)?""",
    RegexOption.IGNORE_CASE,
)

// Markdown image (drop entirely) and markdown link (keep the visible label).
private val STRIP_IMAGE = Regex("""!\[[^\]]*]\([^)]*\)""")
private val LINK_LABEL = Regex("""\[([^\]]+)]\([^)]*\)""")

/**
 * First usable thumbnail URL in a feed entry: a markdown image's URL if present,
 * otherwise the first bare link that ends in a known image extension. Returns null
 * when the text carries no image, so the row falls back to its source initial.
 */
internal fun extractFeedThumbnailUrl(content: String): String? {
    MARKDOWN_IMAGE.find(content)?.groupValues?.getOrNull(1)?.let { return it }
    return IMAGE_URL.find(content)?.value
}

/** Drop markdown images and unwrap markdown links to their label, so feed titles
 *  and summaries read as plain prose instead of raw markup. */
internal fun stripFeedMarkdownMedia(content: String): String = content.replace(STRIP_IMAGE, "").replace(LINK_LABEL, "$1")
