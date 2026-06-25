package com.ether4o4.morsvitaest.data

import com.ether4o4.morsvitaest.getAppFilesDirectory
import java.io.File

private const val LEGACY_FILE_NAME = "conversations.enc"

actual fun readLegacyConversationFile(): ByteArray? {
    val file = File(getAppFilesDirectory(), LEGACY_FILE_NAME)
    return if (file.exists()) file.readBytes() else null
}

actual fun deleteLegacyConversationFile() {
    val file = File(getAppFilesDirectory(), LEGACY_FILE_NAME)
    if (file.exists()) file.delete()
}

private const val BLOB_DIR_NAME = "conversation_blobs"

private fun conversationBlobDir(): File = File(getAppFilesDirectory(), BLOB_DIR_NAME)

actual fun conversationBlobExists(ref: String): Boolean = File(conversationBlobDir(), ref).exists()

actual fun writeConversationBlob(ref: String, bytes: ByteArray) {
    val dir = conversationBlobDir()
    dir.mkdirs()
    File(dir, ref).writeBytes(bytes)
}

actual fun readConversationBlob(ref: String): ByteArray? {
    val file = File(conversationBlobDir(), ref)
    return if (file.exists()) file.readBytes() else null
}

actual fun sweepConversationBlobs(referenced: Set<String>) {
    val dir = conversationBlobDir()
    if (!dir.isDirectory) return
    dir.listFiles()?.forEach { file -> if (file.name !in referenced) file.delete() }
}
