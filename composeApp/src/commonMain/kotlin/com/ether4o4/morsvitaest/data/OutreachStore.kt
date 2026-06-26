package com.ether4o4.morsvitaest.data

import kotlinx.serialization.Serializable

/** Per-local-day send tally for outreach. Reset implicitly: a tally whose [date]
 *  isn't today counts as zero. */
@Serializable
data class OutreachDaily(val date: String = "", val count: Int = 0)

/**
 * Guardrail state for autonomous cold outreach (e.g. music-curator emails). Backs
 * `send_outreach_email`: a persistent set of already-contacted addresses (so the same
 * recipient is never emailed twice) and a per-day send counter (so a hard daily cap can
 * be enforced in code — the model physically can't exceed it, however it's prompted).
 *
 * Addresses are normalized to trimmed lowercase before compare/store.
 */
class OutreachStore(private val appSettings: AppSettings) {

    private val json = SharedJson

    fun contacted(): Set<String> {
        val raw = appSettings.getOutreachContactedJson()
        if (raw.isEmpty()) return emptySet()
        return runCatching { json.decodeFromString<Set<String>>(raw) }.getOrDefault(emptySet())
    }

    fun isContacted(email: String): Boolean = email.trim().lowercase() in contacted()

    fun markContacted(email: String) {
        val updated = contacted() + email.trim().lowercase()
        appSettings.setOutreachContactedJson(json.encodeToString(updated))
    }

    /** Sends recorded for [today] (a local-date string, e.g. "2026-06-25"); 0 if the
     *  stored tally is from a different day. */
    fun sentToday(today: String): Int {
        val raw = appSettings.getOutreachDailyJson()
        if (raw.isEmpty()) return 0
        val daily = runCatching { json.decodeFromString<OutreachDaily>(raw) }.getOrNull() ?: return 0
        return if (daily.date == today) daily.count else 0
    }

    fun recordSent(today: String) {
        appSettings.setOutreachDailyJson(json.encodeToString(OutreachDaily(today, sentToday(today) + 1)))
    }

    fun dailyCap(): Int = appSettings.getOutreachDailyCap()
}
