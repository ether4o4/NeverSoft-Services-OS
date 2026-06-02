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
    val documents: List<ProjectDocument> = emptyList(),
) {
    companion object {
        /** Stable id for the "no project active / default container" state. */
        const val NONE_ID: String = ""
    }
}

/**
 * A document attached to a project. Markdown-flavored text — stored inline in
 * the project JSON for v1 (simpler cross-platform than per-file storage; can
 * migrate to filesDir-on-disk in a future phase if file sizes get unwieldy).
 *
 * Each document's content gets included in the active project's system-prompt
 * preamble, so the AI sees the project's full reference library on every send
 * regardless of which provider is being called.
 */
@OptIn(ExperimentalUuidApi::class)
@Immutable
data class ProjectDocument(
    val id: String = Uuid.random().toString(),
    val name: String,
    val content: String = "",
)
