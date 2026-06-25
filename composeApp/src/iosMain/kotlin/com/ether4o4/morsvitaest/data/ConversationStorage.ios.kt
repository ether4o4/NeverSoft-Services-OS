@file:OptIn(ExperimentalForeignApi::class)

package com.ether4o4.morsvitaest.data

import com.ether4o4.morsvitaest.getAppFilesDirectory
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.dataWithBytes
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

private const val LEGACY_FILE_NAME = "conversations.enc"
private const val BLOB_DIR_NAME = "conversation_blobs"

actual fun readLegacyConversationFile(): ByteArray? {
    val path = "${getAppFilesDirectory()}/$LEGACY_FILE_NAME"
    val data = NSData.dataWithContentsOfFile(path) ?: return null
    val size = data.length.toInt()
    if (size == 0) return null
    val bytes = ByteArray(size)
    bytes.usePinned { pinned ->
        memcpy(pinned.addressOf(0), data.bytes, data.length)
    }
    return bytes
}

actual fun deleteLegacyConversationFile() {
    val path = "${getAppFilesDirectory()}/$LEGACY_FILE_NAME"
    NSFileManager.defaultManager.removeItemAtPath(path, null)
}

private fun conversationBlobDir(): String {
    val dir = "${getAppFilesDirectory()}/$BLOB_DIR_NAME"
    NSFileManager.defaultManager.createDirectoryAtPath(dir, withIntermediateDirectories = true, attributes = null, error = null)
    return dir
}

actual fun conversationBlobExists(ref: String): Boolean =
    NSFileManager.defaultManager.fileExistsAtPath("${conversationBlobDir()}/$ref")

actual fun writeConversationBlob(ref: String, bytes: ByteArray) {
    val path = "${conversationBlobDir()}/$ref"
    val data = if (bytes.isEmpty()) {
        NSData()
    } else {
        bytes.usePinned { pinned ->
            NSData.dataWithBytes(pinned.addressOf(0), bytes.size.toULong())
        }
    }
    data.writeToFile(path, atomically = true)
}

actual fun readConversationBlob(ref: String): ByteArray? {
    val path = "${conversationBlobDir()}/$ref"
    val data = NSData.dataWithContentsOfFile(path) ?: return null
    val size = data.length.toInt()
    if (size == 0) return ByteArray(0)
    val bytes = ByteArray(size)
    bytes.usePinned { pinned ->
        memcpy(pinned.addressOf(0), data.bytes, data.length)
    }
    return bytes
}

actual fun sweepConversationBlobs(referenced: Set<String>) {
    val dir = conversationBlobDir()
    val fm = NSFileManager.defaultManager
    val entries = fm.contentsOfDirectoryAtPath(dir, null) ?: return
    for (entry in entries) {
        val name = entry as? String ?: continue
        if (name !in referenced) fm.removeItemAtPath("$dir/$name", null)
    }
}
