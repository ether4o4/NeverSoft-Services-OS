package com.ether4o4.morsvitaest.data

import kotlinx.browser.localStorage
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val LEGACY_STORAGE_KEY = "kaimutableConversations"

@OptIn(ExperimentalEncodingApi::class)
actual fun readLegacyConversationFile(): ByteArray? {
    val stored = localStorage.getItem(LEGACY_STORAGE_KEY) ?: return null
    if (stored.startsWith("{")) return null // Already plain JSON, not legacy
    return try {
        Base64.decode(stored)
    } catch (_: Exception) {
        null
    }
}

actual fun deleteLegacyConversationFile() {
    localStorage.removeItem(LEGACY_STORAGE_KEY)
}

private const val BLOB_KEY_PREFIX = "convblob_"

actual fun conversationBlobExists(ref: String): Boolean =
    localStorage.getItem("$BLOB_KEY_PREFIX$ref") != null

@OptIn(ExperimentalEncodingApi::class)
actual fun writeConversationBlob(ref: String, bytes: ByteArray) {
    // localStorage has a small quota; failures are non-fatal (the blob just won't persist).
    runCatching { localStorage.setItem("$BLOB_KEY_PREFIX$ref", Base64.encode(bytes)) }
}

@OptIn(ExperimentalEncodingApi::class)
actual fun readConversationBlob(ref: String): ByteArray? =
    localStorage.getItem("$BLOB_KEY_PREFIX$ref")?.let { runCatching { Base64.decode(it) }.getOrNull() }

actual fun sweepConversationBlobs(referenced: Set<String>) {
    val toRemove = mutableListOf<String>()
    for (i in 0 until localStorage.length) {
        val key = localStorage.key(i) ?: continue
        if (key.startsWith(BLOB_KEY_PREFIX) && key.removePrefix(BLOB_KEY_PREFIX) !in referenced) {
            toRemove.add(key)
        }
    }
    toRemove.forEach { localStorage.removeItem(it) }
}
