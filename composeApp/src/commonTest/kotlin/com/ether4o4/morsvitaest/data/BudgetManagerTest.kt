package com.ether4o4.morsvitaest.data

import com.russhwolf.settings.MapSettings
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class BudgetManagerTest {

    // Epoch day 100 (UTC) — an arbitrary fixed day for deterministic accounting.
    private val day100Ms = 100L * 86_400_000L

    private fun manager(now: () -> Instant): BudgetManager =
        BudgetManager(AppSettings(MapSettings()), clock = now, zone = { TimeZone.UTC })

    @Test
    fun records_and_aggregates_usage_by_source() {
        val manager = manager { Instant.fromEpochMilliseconds(day100Ms) }

        manager.record(UsageSource.CHAT, TokenUsage(inputTokens = 10, outputTokens = 20))
        manager.record(UsageSource.CHAT, TokenUsage(inputTokens = 5, outputTokens = 5))
        manager.record(UsageSource.HEARTBEAT, TokenUsage(inputTokens = 100, outputTokens = 0))

        val today = manager.getUsageToday()
        assertEquals(140L, today.totalTokens)
        assertEquals(3L, today.totalCalls)
        assertEquals(40L, today.bySource[UsageSource.CHAT]?.totalTokens)
        assertEquals(2L, today.bySource[UsageSource.CHAT]?.calls)
        assertEquals(100L, today.bySource[UsageSource.HEARTBEAT]?.totalTokens)
    }

    @Test
    fun rolls_over_at_local_day_boundary() {
        var now = Instant.fromEpochMilliseconds(day100Ms)
        val manager = manager { now }

        manager.record(UsageSource.CHAT, TokenUsage(50, 50))
        assertEquals(100L, manager.getUsageToday().totalTokens)

        // Advance one day — the previous day's counters must not leak into the new day.
        now = Instant.fromEpochMilliseconds(day100Ms + 86_400_000L)
        assertEquals(0L, manager.getUsageToday().totalTokens)
        assertEquals(0L, manager.getUsageToday().totalCalls)

        manager.record(UsageSource.TASK, TokenUsage(10, 0))
        assertEquals(10L, manager.getUsageToday().totalTokens)
    }

    @Test
    fun manual_pause_blocks_autonomous_work() {
        val manager = manager { Instant.fromEpochMilliseconds(day100Ms) }
        assertEquals(BudgetDecision.Allowed, manager.mayRunAutonomous())

        manager.setAutonomousPaused(true)
        val decision = manager.mayRunAutonomous()
        assertTrue(decision is BudgetDecision.Paused && decision.reason == PauseReason.MANUAL)
    }

    @Test
    fun budget_breach_blocks_autonomous_work() {
        val manager = manager { Instant.fromEpochMilliseconds(day100Ms) }
        manager.setDailyTokenBudget(100)

        manager.record(UsageSource.CHAT, TokenUsage(60, 50)) // 110 >= 100
        assertTrue(manager.isOverBudget())

        val decision = manager.mayRunAutonomous()
        assertTrue(decision is BudgetDecision.Paused && decision.reason == PauseReason.BUDGET)
    }

    @Test
    fun over_budget_still_allows_when_auto_pause_disabled() {
        val manager = manager { Instant.fromEpochMilliseconds(day100Ms) }
        manager.setDailyTokenBudget(100)
        manager.setPauseAutonomousOnBreach(false)

        manager.record(UsageSource.CHAT, TokenUsage(200, 0))
        assertTrue(manager.isOverBudget())
        assertEquals(BudgetDecision.Allowed, manager.mayRunAutonomous())
    }

    @Test
    fun unlimited_budget_is_never_over() {
        val manager = manager { Instant.fromEpochMilliseconds(day100Ms) }
        // Default dailyTokenBudget = 0 means unlimited.
        manager.record(UsageSource.CHAT, TokenUsage(1_000_000, 0))
        assertFalse(manager.isOverBudget())
        assertEquals(BudgetDecision.Allowed, manager.mayRunAutonomous())
    }

    @Test
    fun reset_clears_todays_counters() {
        val manager = manager { Instant.fromEpochMilliseconds(day100Ms) }
        manager.record(UsageSource.CHAT, TokenUsage(10, 10))

        manager.resetUsageToday()

        assertEquals(0L, manager.getUsageToday().totalTokens)
        assertEquals(0L, manager.getUsageToday().totalCalls)
    }
}
