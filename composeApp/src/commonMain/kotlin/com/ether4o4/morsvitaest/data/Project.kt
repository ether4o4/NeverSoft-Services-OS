package com.ether4o4.morsvitaest.data

import androidx.compose.runtime.Immutable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * A Project is an app-owned context container that follows the user across
 * any AI provider. The AI never "remembers" the project — the app composes
 * the project's [instructions] (and, in later phases, its files + memories)
 * into the system prompt on every send. Switch from OpenRouter to a local
 * Qwen model and the same project context shows up — the AI is just the
 * messenger; the app is the source of truth.
 *
 * Phase 1 fields. Phase 2 will add `attachedFiles: List<String>` (paths
 * under app filesDir/projects/<id>/). Phase 3 will add `memoryScope` so
 * memories can be filtered per-project.
 */
@OptIn(ExperimentalUuidApi::class)
@Immutable
data class Project(
    val id: String = Uuid.random().toString(),
    val name: String,
    val instructions: String = "",
    val createdAt: Long = 0L,
) {
    companion object {
        /** Stable id for the "no project active / default container" state. */
        const val NONE_ID: String = ""
    }
}
