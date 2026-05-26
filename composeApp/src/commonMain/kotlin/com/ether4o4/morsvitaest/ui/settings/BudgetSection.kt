package com.ether4o4.morsvitaest.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ether4o4.morsvitaest.data.BudgetConfig
import com.ether4o4.morsvitaest.data.DailyUsage
import com.ether4o4.morsvitaest.data.UsageSource
import com.ether4o4.morsvitaest.ui.handCursor
import morsvitaest.composeapp.generated.resources.Res
import morsvitaest.composeapp.generated.resources.settings_budget
import morsvitaest.composeapp.generated.resources.settings_budget_auto_pause
import morsvitaest.composeapp.generated.resources.settings_budget_daily_cap
import morsvitaest.composeapp.generated.resources.settings_budget_description
import morsvitaest.composeapp.generated.resources.settings_budget_paused
import morsvitaest.composeapp.generated.resources.settings_budget_pause_now
import morsvitaest.composeapp.generated.resources.settings_budget_reset
import morsvitaest.composeapp.generated.resources.settings_budget_source_chat
import morsvitaest.composeapp.generated.resources.settings_budget_source_heartbeat
import morsvitaest.composeapp.generated.resources.settings_budget_source_other
import morsvitaest.composeapp.generated.resources.settings_budget_source_task
import morsvitaest.composeapp.generated.resources.settings_budget_unlimited
import morsvitaest.composeapp.generated.resources.settings_budget_usage_value
import morsvitaest.composeapp.generated.resources.settings_budget_used_today
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource

private val BUDGET_PRESETS = persistentListOf(0, 50_000, 100_000, 250_000, 500_000, 1_000_000, 2_000_000, 5_000_000)

@Composable
internal fun BudgetSection(
    config: BudgetConfig,
    usageToday: DailyUsage,
    onChangeDailyTokenBudget: (Long) -> Unit,
    onTogglePauseAutonomousOnBreach: (Boolean) -> Unit,
    onToggleAutonomousPaused: (Boolean) -> Unit,
    onResetUsage: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.settings_budget),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(Res.string.settings_budget_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        // Today's usage summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.settings_budget_used_today),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(
                    Res.string.settings_budget_usage_value,
                    formatTokenCount(usageToday.totalTokens),
                    usageToday.totalCalls,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        // Per-source breakdown (only sources that actually spent tokens today)
        UsageSource.entries.forEach { source ->
            val bucket = usageToday.bySource[source]
            if (bucket != null && bucket.totalTokens > 0L) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = sourceLabel(source),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatTokenCount(bucket.totalTokens),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        PresetSlider(
            currentValue = config.dailyTokenBudget.toInt(),
            presets = BUDGET_PRESETS,
            fallbackIndex = 0,
            label = { stringResource(Res.string.settings_budget_daily_cap) },
            formatValue = { tokens ->
                if (tokens == 0) stringResource(Res.string.settings_budget_unlimited) else formatTokenCount(tokens.toLong())
            },
            onValueChanged = { onChangeDailyTokenBudget(it.toLong()) },
        )

        Spacer(Modifier.height(8.dp))

        BudgetSwitchRow(
            label = stringResource(Res.string.settings_budget_auto_pause),
            checked = config.pauseAutonomousOnBreach,
            onCheckedChange = onTogglePauseAutonomousOnBreach,
        )
        BudgetSwitchRow(
            label = stringResource(Res.string.settings_budget_pause_now),
            checked = config.autonomousPaused,
            onCheckedChange = onToggleAutonomousPaused,
        )

        if (config.autonomousPaused) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.settings_budget_paused),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (usageToday.totalCalls > 0L) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onResetUsage,
                modifier = Modifier.align(CenterHorizontally).handCursor(),
            ) {
                Text(stringResource(Res.string.settings_budget_reset))
            }
        }
    }
}

@Composable
private fun BudgetSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.handCursor(),
        )
    }
}

@Composable
private fun sourceLabel(source: UsageSource): String = when (source) {
    UsageSource.CHAT -> stringResource(Res.string.settings_budget_source_chat)
    UsageSource.HEARTBEAT -> stringResource(Res.string.settings_budget_source_heartbeat)
    UsageSource.TASK -> stringResource(Res.string.settings_budget_source_task)
    UsageSource.OTHER -> stringResource(Res.string.settings_budget_source_other)
}

/** Compact human-readable token count: 999 → "999", 1500 → "1.5k", 1_200_000 → "1.2M". */
private fun formatTokenCount(n: Long): String = when {
    n >= 1_000_000L -> formatWithUnit(n, 1_000_000L) + "M"
    n >= 1_000L -> formatWithUnit(n, 1_000L) + "k"
    else -> n.toString()
}

private fun formatWithUnit(n: Long, unit: Long): String {
    val whole = n / unit
    val tenths = (n % unit) * 10L / unit
    return if (tenths == 0L) whole.toString() else "$whole.$tenths"
}
