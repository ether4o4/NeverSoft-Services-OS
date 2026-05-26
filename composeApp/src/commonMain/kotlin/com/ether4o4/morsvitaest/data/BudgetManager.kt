package com.ether4o4.morsvitaest.data

import androidx.compose.runtime.Immutable
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Where an LLM call originated. Used to attribute token usage in the budget telemetry so the
 * user can see how much of the day's spend came from interactive chat vs. background work.
 */
@Serializable
enum class UsageSource { CHAT, HEARTBEAT, TASK, OTHER }

/**
 * Token counts for a single LLM call. Providers report these in their response payload; when a
 * provider omits them we fall back to a character-based estimate at the call site.
 */
@Immutable
@Serializable
data class TokenUsage(
    val inputTokens: Long = 0L,
    val outputTokens: Long = 0L,
) {
    val totalTokens: Long get() = inputTokens + outputTokens

    companion object {
        val ZERO = TokenUsage()
    }
}

/** Per-[UsageSource] aggregate within a single local day. */
@Immutable
@Serializable
data class SourceUsage(
    val inputTokens: Long = 0L,
    val outputTokens: Long = 0L,
    val calls: Long = 0L,
) {
    val totalTokens: Long get() = inputTokens + outputTokens
}

/**
 * One local day's worth of usage, keyed by [epochDay] (days since the Unix epoch in the user's
 * local time zone) so it rolls over automatically at local midnight.
 */
@Immutable
@Serializable
data class DailyUsage(
    val epochDay: Long = 0L,
    val bySource: Map<UsageSource, SourceUsage> = emptyMap(),
) {
    val totalTokens: Long get() = bySource.values.sumOf { it.totalTokens }
    val totalCalls: Long get() = bySource.values.sumOf { it.calls }
}

@Immutable
@Serializable
data class BudgetConfig(
    /** Hard cap on total tokens (input + output) per local day. `0` = unlimited. */
    val dailyTokenBudget: Long = 0L,
    /** When true, autonomous work (heartbeat + scheduled tasks) is suspended once the daily budget is reached. */
    val pauseAutonomousOnBreach: Boolean = true,
    /** Manual kill switch: when true, autonomous work is suspended regardless of budget. */
    val autonomousPaused: Boolean = false,
)

enum class PauseReason { MANUAL, BUDGET }

/** Result of [BudgetManager.mayRunAutonomous]. */
sealed interface BudgetDecision {
    /** Autonomous work is permitted. */
    data object Allowed : BudgetDecision

    /** Autonomous work is suspended; [reason] explains why. */
    data class Paused(val reason: PauseReason) : BudgetDecision
}

/**
 * The budget governor — the "cost is a leak" guard rail for an always-on agent that talks to paid
 * LLM providers. It tracks token usage per local day (attributed by [UsageSource]) and enforces an
 * optional daily token budget plus a manual kill switch over autonomous work.
 *
 * Token accounting is exact when the provider reports usage and a character-based estimate
 * otherwise; see the budget feature doc for the accuracy contract.
 *
 * The [clock] and [zone] are injectable so day-rollover behaviour is deterministic in tests.
 */
@OptIn(ExperimentalTime::class)
class BudgetManager(
    private val appSettings: AppSettings,
    private val clock: () -> Instant = { Clock.System.now() },
    private val zone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) {

    private val json = SharedJson

    fun getConfig(): BudgetConfig {
        val raw = appSettings.getBudgetConfigJson()
        if (raw.isEmpty()) return BudgetConfig()
        return try {
            json.decodeFromString<BudgetConfig>(raw)
        } catch (_: Exception) {
            BudgetConfig()
        }
    }

    fun saveConfig(config: BudgetConfig) {
        appSettings.setBudgetConfigJson(json.encodeToString(config))
    }

    fun setDailyTokenBudget(tokens: Long) {
        saveConfig(getConfig().copy(dailyTokenBudget = tokens.coerceAtLeast(0L)))
    }

    fun setPauseAutonomousOnBreach(enabled: Boolean) {
        saveConfig(getConfig().copy(pauseAutonomousOnBreach = enabled))
    }

    fun setAutonomousPaused(paused: Boolean) {
        saveConfig(getConfig().copy(autonomousPaused = paused))
    }

    /** Today's usage, rolling over automatically at local midnight. */
    fun getUsageToday(): DailyUsage {
        val stored = loadUsage()
        val today = currentEpochDay()
        return if (stored.epochDay == today) stored else DailyUsage(epochDay = today)
    }

    /** Record one LLM call's token usage against the current local day. */
    fun record(source: UsageSource, usage: TokenUsage) {
        val today = currentEpochDay()
        val current = loadUsage().let { if (it.epochDay == today) it else DailyUsage(epochDay = today) }
        val prev = current.bySource[source] ?: SourceUsage()
        val updated = current.copy(
            bySource = current.bySource + (
                source to SourceUsage(
                    inputTokens = prev.inputTokens + usage.inputTokens,
                    outputTokens = prev.outputTokens + usage.outputTokens,
                    calls = prev.calls + 1L,
                )
                ),
        )
        saveUsage(updated)
    }

    /** Reset the running day's counters. Does not touch [BudgetConfig]. */
    fun resetUsageToday() {
        saveUsage(DailyUsage(epochDay = currentEpochDay()))
    }

    /** True when a daily budget is set and today's total tokens meet or exceed it. */
    fun isOverBudget(): Boolean {
        val budget = getConfig().dailyTokenBudget
        return budget > 0L && getUsageToday().totalTokens >= budget
    }

    /** Whether the scheduler may run autonomous work (heartbeat + scheduled tasks) right now. */
    fun mayRunAutonomous(): BudgetDecision {
        val config = getConfig()
        if (config.autonomousPaused) return BudgetDecision.Paused(PauseReason.MANUAL)
        if (config.pauseAutonomousOnBreach &&
            config.dailyTokenBudget > 0L &&
            getUsageToday().totalTokens >= config.dailyTokenBudget
        ) {
            return BudgetDecision.Paused(PauseReason.BUDGET)
        }
        return BudgetDecision.Allowed
    }

    private fun loadUsage(): DailyUsage {
        val raw = appSettings.getBudgetUsageJson()
        if (raw.isEmpty()) return DailyUsage(epochDay = currentEpochDay())
        return try {
            json.decodeFromString<DailyUsage>(raw)
        } catch (_: Exception) {
            DailyUsage(epochDay = currentEpochDay())
        }
    }

    private fun saveUsage(usage: DailyUsage) {
        appSettings.setBudgetUsageJson(json.encodeToString(usage))
    }

    private fun currentEpochDay(): Long = clock().toLocalDateTime(zone()).date.toEpochDays()
}
