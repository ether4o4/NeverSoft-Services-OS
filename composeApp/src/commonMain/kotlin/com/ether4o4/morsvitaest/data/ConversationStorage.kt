package com.ether4o4.morsvitaest.data

import com.ether4o4.morsvitaest.TerminalLine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

expect fun readLegacyConversationFile(): ByteArray?
expect fun deleteLegacyConversationFile()

/**
 * Sidecar blob store for large attachment binaries (images, PDFs). Pulling them out
 * of the conversation JSON keeps that JSON tiny, so it no longer re-serializes
 * multi-megabyte base64 on every save — the allocation churn that caused
 * GC-pause stutters once image attachments existed.
 */
expect fun conversationBlobExists(ref: String): Boolean
expect fun writeConversationBlob(ref: String, bytes: ByteArray)
expect fun readConversationBlob(ref: String): ByteArray?

/** Delete every stored blob whose ref isn't in [referenced] (orphan cleanup). */
expect fun sweepConversationBlobs(referenced: Set<String>)

private const val MAX_SHELL_TRANSCRIPT_CHARS = 10_000

// Attachments whose base64 payload exceeds this are moved to a sidecar blob file;
// smaller ones (short text files) stay inline. ~8 KB base64 ≈ 6 KB of raw bytes.
private const val INLINE_ATTACHMENT_LIMIT = 8_192

class ConversationStorage(private val appSettings: AppSettings) {
    private val mutableConversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = mutableConversations.asStateFlow()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun loadConversations() {
        val data = appSettings.getConversationsJson()
        if (data != null) {
            mutableConversations.value = deserialize(data)
        } else {
            migrateLegacy()
        }
        // Drop blob files no longer referenced by any conversation (e.g. from deleted chats).
        val referenced = mutableConversations.value
            .asSequence()
            .flatMap { it.messages.asSequence() }
            .flatMap { it.attachments.asSequence() }
            .mapNotNull { it.blobRef }
            .toSet()
        runCatching { sweepConversationBlobs(referenced) }
    }

    fun saveConversation(conversation: Conversation) {
        mutableConversations.update { current ->
            val index = current.indexOfFirst { it.id == conversation.id }
            if (index >= 0) {
                val existing = current[index]
                // Chat-layer save paths rebuild Conversation without the shell
                // transcript; preserve the stored tail rather than wiping it.
                val merged = if (conversation.shellTranscript.isEmpty() && existing.shellTranscript.isNotEmpty()) {
                    conversation.copy(shellTranscript = existing.shellTranscript)
                } else {
                    conversation
                }
                current.toMutableList().apply { set(index, merged) }
            } else {
                current + conversation
            }
        }
        persist()
    }

    /**
     * Persist a shell transcript snapshot for [conversationId]. No-op when the
     * conversation isn't in the saved set yet (chat hasn't been saved). The
     * stored transcript is trimmed from the head until the joined text is
     * within the per-conversation char budget — older lines drop first so the
     * tail (most recent activity) survives a restart.
     */
    fun updateShellTranscript(conversationId: String, lines: List<TerminalLine>) {
        val trimmed = trimToCharLimit(lines, MAX_SHELL_TRANSCRIPT_CHARS)
        var changed = false
        mutableConversations.update { current ->
            val idx = current.indexOfFirst { it.id == conversationId }
            if (idx < 0) {
                current
            } else if (current[idx].shellTranscript == trimmed) {
                current
            } else {
                changed = true
                current.toMutableList().apply { set(idx, current[idx].copy(shellTranscript = trimmed)) }
            }
        }
        if (changed) persist()
    }

    private fun trimToCharLimit(lines: List<TerminalLine>, limit: Int): List<TerminalLine> {
        if (lines.isEmpty()) return lines
        val totalChars = lines.sumOf { it.text.length }
        if (totalChars <= limit) return lines
        val tail = ArrayDeque<TerminalLine>()
        var running = 0
        for (i in lines.indices.reversed()) {
            val line = lines[i]
            if (running + line.text.length <= limit) {
                tail.addFirst(line)
                running += line.text.length
            } else {
                break
            }
        }
        if (tail.isNotEmpty()) return tail.toList()
        // Single most-recent line is itself larger than the budget — keep its
        // tail so the user still sees something on next launch.
        val last = lines.last()
        return listOf(last.withText(last.text.takeLast(limit)))
    }

    fun deleteConversation(id: String) {
        mutableConversations.update { current ->
            current.filter { it.id != id }
        }
        persist()
    }

    private fun persist() {
        // Externalize large attachment payloads to blob files first, so the JSON written
        // to settings stays small. mutableConversations keeps the full in-memory copy
        // (unchanged from before); only the serialized form is stripped.
        val externalized = mutableConversations.value.map { externalize(it) }
        val data = json.encodeToString(ConversationsData(conversations = externalized))
        appSettings.setConversationsJson(data)
    }

    /**
     * Returns a copy of [conversation] in which every large attachment has its base64
     * `data` written to a sidecar blob (keyed by `messageId_index`) and cleared inline.
     * The blob is written at most once per attachment — if it already exists on disk, the
     * base64 is not decoded again, so steady-state saves do no heavy work.
     */
    @OptIn(ExperimentalEncodingApi::class)
    private fun externalize(conversation: Conversation): Conversation {
        val needsWork = conversation.messages.any { m ->
            m.attachments.any { (it.blobRef == null && it.data.length > INLINE_ATTACHMENT_LIMIT) || (it.blobRef != null && it.data.isNotEmpty()) }
        }
        if (!needsWork) return conversation
        return conversation.copy(
            messages = conversation.messages.map { m ->
                if (m.attachments.isEmpty()) return@map m
                m.copy(
                    attachments = m.attachments.mapIndexed { i, a ->
                        when {
                            // Already externalized — make sure no inline copy leaks into the JSON.
                            a.blobRef != null -> if (a.data.isEmpty()) a else a.copy(data = "")
                            a.data.length > INLINE_ATTACHMENT_LIMIT -> {
                                val ref = "${m.id}_$i"
                                if (!conversationBlobExists(ref)) {
                                    runCatching { writeConversationBlob(ref, Base64.decode(a.data)) }
                                }
                                a.copy(data = "", blobRef = ref)
                            }
                            else -> a
                        }
                    },
                )
            },
        )
    }

    private fun deserialize(data: String): List<Conversation> = try {
        json.decodeFromString<ConversationsData>(data).conversations
    } catch (_: Exception) {
        emptyList()
    }

    private fun migrateLegacy() {
        val legacyData = readLegacyConversationFile() ?: return
        val key = appSettings.getEncryptionKey() ?: return
        val decrypted = ByteArray(legacyData.size)
        for (i in legacyData.indices) {
            decrypted[i] = (legacyData[i].toInt() xor key[i % key.size].toInt()).toByte()
        }
        val conversations = deserialize(decrypted.decodeToString())
        mutableConversations.value = conversations
        persist()
        deleteLegacyConversationFile()
    }
}
