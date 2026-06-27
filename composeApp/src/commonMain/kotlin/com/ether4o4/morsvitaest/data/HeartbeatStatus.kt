package com.ether4o4.morsvitaest.data

import androidx.compose.runtime.Immutable

/**
 * Why the heartbeat is or isn't running right now. Surfaced on the home so the
 * proactive assistant can never silently die — if it stops emitting, the reason
 * (and the fix) is visible instead of the user just wondering why it went quiet.
 */
enum class HeartbeatRunState {
    /** Running normally — scheduling on, heartbeat enabled, budget OK, within active hours. */
    ACTIVE,

    /** Master scheduling switch is off — nothing autonomous runs. */
    SCHEDULING_OFF,

    /** Heartbeat itself is disabled in its settings. */
    HEARTBEAT_OFF,

    /** Daily token budget reached — autonomous work paused until midnight or a higher cap. */
    BUDGET_PAUSED,

    /** Autonomous activity manually switched off (kill switch). */
    KILL_SWITCH,

    /** Outside the active-hours window — will resume on its own. */
    ASLEEP,
}

@Immutable
data class HeartbeatStatus(
    val state: HeartbeatRunState,
    val activeHoursStart: Int = 8,
    val activeHoursEnd: Int = 22,
)
