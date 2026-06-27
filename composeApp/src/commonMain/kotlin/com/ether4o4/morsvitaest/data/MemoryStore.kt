package com.ether4o4.morsvitaest.data

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Serializable
enum class MemoryCategory {
    GENERAL,
    LEARNING,
    ERROR,
    PREFERENCE,
}

@Immutable
@Serializable
data class MemoryEntry(
    val key: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val category: MemoryCategory = MemoryCategory.GENERAL,
    val hitCount: Int = 1,
    val source: String? = null,
)

@OptIn(ExperimentalTime::class)
class MemoryStore(private val appSettings: AppSettings) {

    private val json = SharedJson
    private val mutex = Mutex()

    private fun loadMemories(): MutableList<MemoryEntry> {
        val raw = appSettings.getMemoriesJson()
        if (raw.isBlank()) return mutableListOf()
        return try {
            json.decodeFromString<List<MemoryEntry>>(raw).toMutableList()
        } catch (e: Exception) {
            println("MemoryStore: failed to load memories: ${e.message}")
            mutableListOf()
        }
    }

    private fun saveMemories(memories: List<MemoryEntry>) {
        appSettings.setMemoriesJson(json.encodeToString(memories))
    }

    suspend fun store(
        key: String,
        content: String,
        category: MemoryCategory = MemoryCategory.GENERAL,
        source: String? = null,
    ): MemoryEntry = mutex.withLock {
        // Compress the model's auto-stored memory so it's reference-dense — more useful
        // memories fit the on-device memory budget. User-edited memories (updateContent)
        // are left verbatim.
        val compressed = compressMemoryText(content)
        val memories = loadMemories()
        val now = Clock.System.now().toEpochMilliseconds()
        val existing = memories.indexOfFirst { it.key == key }
        val entry = if (existing >= 0) {
            val updated = memories[existing].copy(content = compressed, updatedAt = now, category = category, source = source ?: memories[existing].source)
            memories[existing] = updated
            updated
        } else {
            val newEntry = MemoryEntry(key = key, content = compressed, createdAt = now, updatedAt = now, category = category, source = source)
            memories.add(newEntry)
            newEntry
        }
        saveMemories(memories)
        entry
    }

    suspend fun updateContent(key: String, content: String): MemoryEntry? = mutex.withLock {
        val memories = loadMemories()
        val index = memories.indexOfFirst { it.key == key }
        if (index < 0) return@withLock null
        val now = Clock.System.now().toEpochMilliseconds()
        val updated = memories[index].copy(content = content, updatedAt = now)
        memories[index] = updated
        saveMemories(memories)
        updated
    }

    suspend fun reinforceMemory(key: String): MemoryEntry? = mutex.withLock {
        val memories = loadMemories()
        val index = memories.indexOfFirst { it.key == key }
        if (index < 0) return@withLock null
        val now = Clock.System.now().toEpochMilliseconds()
        val updated = memories[index].copy(hitCount = memories[index].hitCount + 1, updatedAt = now)
        memories[index] = updated
        saveMemories(memories)
        updated
    }

    fun getPromotionCandidates(minHits: Int = 5): List<MemoryEntry> = loadMemories().filter { it.hitCount >= minHits }

    suspend fun forget(key: String): Boolean = mutex.withLock {
        val memories = loadMemories()
        val removed = memories.removeAll { it.key == key }
        if (removed) saveMemories(memories)
        removed
    }

    fun getAllMemories(): List<MemoryEntry> = loadMemories()
}

private val MEMORY_FILLER_REPLACEMENTS: List<Pair<Regex, String>> = listOf(
    Regex("""\bit is important to note that\b""", RegexOption.IGNORE_CASE) to "",
    Regex("""\bit should be noted that\b""", RegexOption.IGNORE_CASE) to "",
    Regex("""\bplease note that\b""", RegexOption.IGNORE_CASE) to "",
    Regex("""\bkeep in mind that\b""", RegexOption.IGNORE_CASE) to "",
    Regex("""\bdue to the fact that\b""", RegexOption.IGNORE_CASE) to "because",
    Regex("""\bin order to\b""", RegexOption.IGNORE_CASE) to "to",
    Regex("""\bin the event that\b""", RegexOption.IGNORE_CASE) to "if",
    Regex("""\bat this point in time\b""", RegexOption.IGNORE_CASE) to "now",
    Regex("""\ba large number of\b""", RegexOption.IGNORE_CASE) to "many",
)
private val MEMORY_WHITESPACE = Regex("""\s+""")

/**
 * Conservative, meaning-preserving compression for a stored memory: collapses runs of
 * whitespace and swaps a handful of verbose filler phrases for terse equivalents. It
 * deliberately does NOT strip articles, pronouns, subjects ("the user"), or negations —
 * aggressive keyword-stripping would risk corrupting a memory's meaning. The goal is to
 * keep memories reference-dense so more fit the fixed on-device memory budget.
 */
fun compressMemoryText(text: String): String {
    var result = text
    for ((pattern, replacement) in MEMORY_FILLER_REPLACEMENTS) {
        result = pattern.replace(result, replacement)
    }
    return MEMORY_WHITESPACE.replace(result, " ").trim()
}
