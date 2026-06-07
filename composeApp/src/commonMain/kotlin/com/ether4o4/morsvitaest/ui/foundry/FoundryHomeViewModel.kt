package com.ether4o4.morsvitaest.ui.foundry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ether4o4.morsvitaest.data.Conversation
import com.ether4o4.morsvitaest.data.DataRepository
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
    val feed: ImmutableList<FoundryFeedItem> = persistentListOf(),
    val isRefreshing: Boolean = false,
)

/**
 * Backs the Foundry home newsfeed. The feed is the Heartbeat engine's output: each assistant
 * message in the dedicated heartbeat conversation is one "what's new" card, newest first.
 * Pull-to-refresh fires an on-demand heartbeat run via [TaskScheduler.triggerHeartbeatNow].
 */
class FoundryHomeViewModel(
    private val repository: DataRepository,
    private val taskScheduler: TaskScheduler,
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
    }

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
        val lines = content.trim().lineSequence().filter { it.isNotBlank() }.toList()
        val title = lines.firstOrNull()
            ?.trimStart('#', '*', '-', ' ')
            ?.take(120)
            ?.ifBlank { "Heartbeat update" }
            ?: "Heartbeat update"
        val summary = lines.drop(1).joinToString(" ").ifBlank { content.trim() }.take(240)
        return FoundryFeedItem(title = title, source = "Heartbeat", summary = summary)
    }
}
