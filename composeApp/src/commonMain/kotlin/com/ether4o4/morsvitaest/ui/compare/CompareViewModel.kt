package com.ether4o4.morsvitaest.ui.compare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ether4o4.morsvitaest.data.DataRepository
import com.ether4o4.morsvitaest.data.ServiceEntry
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CompareUiState(
    val entries: ImmutableList<ServiceEntry> = persistentListOf(),
    val paneAInstanceId: String? = null,
    val paneBInstanceId: String? = null,
    val seed: String = "",
    val messages: ImmutableList<CompareMessage> = persistentListOf(),
    val merge: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    val canSend: Boolean get() = paneAInstanceId != null && paneBInstanceId != null && !isLoading

    fun labelFor(instanceId: String?): String {
        val entry = entries.firstOrNull { it.instanceId == instanceId } ?: return "Model"
        return "${entry.serviceName} · ${entry.modelId}"
    }
}

/**
 * Backs the Compare screen: holds the two selected model panes and runs a [CompareOrchestrator]
 * rotation against the real inference layer via [DataRepository.askSilentlyWithInstance]. Each
 * model call is wrapped so one pane failing surfaces as that pane's message instead of killing
 * the whole rotation.
 */
class CompareViewModel(
    private val repository: DataRepository,
) : ViewModel() {

    private val orchestrator = CompareOrchestrator()
    private val _state = MutableStateFlow(CompareUiState())
    val state: StateFlow<CompareUiState> = _state.asStateFlow()

    private var rotationJob: Job? = null

    init {
        val entries = repository.getServiceEntries().toImmutableList()
        _state.update {
            it.copy(
                entries = entries,
                paneAInstanceId = entries.getOrNull(0)?.instanceId,
                paneBInstanceId = (entries.getOrNull(1) ?: entries.getOrNull(0))?.instanceId,
            )
        }
    }

    fun selectPane(pane: ComparePane, instanceId: String) {
        _state.update {
            if (pane == ComparePane.A) it.copy(paneAInstanceId = instanceId) else it.copy(paneBInstanceId = instanceId)
        }
    }

    fun setMerge(enabled: Boolean) {
        _state.update { it.copy(merge = enabled) }
    }

    fun clear() {
        rotationJob?.cancel()
        _state.update { it.copy(seed = "", messages = persistentListOf(), error = null, isLoading = false) }
    }

    fun send(seed: String) {
        val current = _state.value
        val aId = current.paneAInstanceId
        val bId = current.paneBInstanceId
        if (seed.isBlank() || aId == null || bId == null || current.isLoading) return

        rotationJob?.cancel()
        rotationJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, seed = seed, messages = persistentListOf()) }
            try {
                orchestrator.runRotation(
                    seed = seed,
                    labelA = current.labelFor(aId),
                    labelB = current.labelFor(bId),
                    merge = current.merge,
                    ask = { pane, prompt ->
                        val instanceId = if (pane == ComparePane.A) aId else bId
                        try {
                            repository.askSilentlyWithInstance(instanceId, prompt)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            "⚠️ ${e.message ?: "request failed"}"
                        }
                    },
                    onMessage = { message ->
                        _state.update { it.copy(messages = (it.messages + message).toImmutableList()) }
                    },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Compare failed") }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}
