package com.ether4o4.morsvitaest.notifications

import com.ether4o4.morsvitaest.data.NotificationRecord

actual class NotificationReader actual constructor() {
    actual fun isSupported(): Boolean = false
    actual fun hasAccess(): Boolean = false
    actual suspend fun getById(id: String): NotificationRecord? = null
    actual suspend fun search(query: String, limit: Int, packageName: String?): List<NotificationRecord> = emptyList()
}
