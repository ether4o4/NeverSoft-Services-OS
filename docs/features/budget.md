# Budget Governor

**Last verified:** 2026-05-22

MorsVitaEst talks to paid LLM providers and runs autonomously in the background (heartbeat self-checks and scheduled tasks). The budget governor is the spend guard rail: it tracks token usage per day, lets the user cap daily spend, and automatically pauses background activity when the cap is reached. It also exposes a manual kill switch that suspends all autonomous work on demand.

## Concepts

### Token Usage

Every LLM call's token usage is recorded against the current local day, attributed to the part of the app that made the call. Counts are **exact** when the provider reports them in its response (`usage` for OpenAI-compatible and Anthropic, `usageMetadata` for Gemini — Gemini's thinking tokens are folded into the output total). When a provider omits usage, MorsVitaEst falls back to a character-based estimate (~4 characters per token) of the request and response, so the totals stay meaningful across all 24 providers.

### Usage Source

Each call is tagged with where it came from, so the user can see what is driving spend:

- **Chat** — interactive messages the user sends.
- **Heartbeat** — periodic background self-checks.
- **Tasks** — scheduled task executions.
- **Other** — internal calls such as history compaction and silent helper calls.

### Daily Token Budget

A hard cap on total tokens (input + output) per local day. `0` means unlimited (the default). Usage counters roll over automatically at local midnight.

### Auto-Pause on Breach

When enabled (the default), reaching the daily budget suspends autonomous work — heartbeat checks and scheduled tasks stop making LLM calls until the budget resets at local midnight or the user raises the cap. Interactive chat is **not** blocked: the user can always keep talking to MorsVitaEst, and a manual heartbeat refresh still runs. Email and SMS polling also keep running because they make no LLM calls.

### Kill Switch

A manual "pause background activity" toggle that suspends autonomous work regardless of budget. Independent of the budget cap — useful for quieting the agent without changing limits.

## Execution Flow

1. After each LLM response, the repository extracts token usage (exact or estimated) and records it against the active source for the current local day.
2. The background scheduler consults the governor before each poll cycle. If autonomous work is paused — either by the kill switch or because the daily budget is reached — scheduled tasks and the heartbeat are skipped for that cycle. The budget is re-checked between tasks so a long batch can't overshoot.
3. The first time the daily budget pauses autonomous work, a single push notification is sent (only when the app is backgrounded; the settings panel shows the paused state regardless). The notice fires once per pause episode and resets when work is permitted again.
4. Counters reset at local midnight; paused autonomous work resumes automatically on the next poll.

## What Is and Isn't Gated

| Activity | Gated by budget / kill switch? |
|---|---|
| Heartbeat self-checks (scheduled) | Yes |
| Scheduled tasks | Yes |
| Manual heartbeat refresh (Settings) | No — user-initiated |
| Interactive chat | No — user-initiated |
| Email / SMS polling | No — no LLM call involved |

## Settings UI

The budget section lives in the **Agent** tab, below Heartbeat. It shows:

- **Used today** — total tokens and call count for the current local day, with a per-source breakdown (only sources that spent tokens are listed).
- **Daily token budget** — a snap-to-preset slider (Unlimited, 50k, 100k, 250k, 500k, 1M, 2M, 5M) with the current value displayed.
- **Pause background activity at the budget** — toggles auto-pause on breach.
- **Pause background activity now** — the manual kill switch; a notice appears below it while active.
- **Reset today** — clears the running day's counters (appears once any usage has been recorded). Does not change the configured budget.

## Storage

- Budget configuration (cap, auto-pause flag, kill-switch flag) and the running day's usage are stored as serialized JSON in app settings.
- Both are device-local and intentionally excluded from settings export/import — a token cap and today's counters are properties of a device, not of a portable profile.

## Accuracy Notes

- Major providers (OpenAI, Anthropic, Gemini, DeepSeek, Groq, Mistral, OpenRouter, and others) report exact usage, so the daily cap is precise for them. Providers that omit usage are estimated and may under- or over-count.
- The budget is measured in tokens, not currency — pricing varies per provider and model, and a token cap stays correct without a pricing table to maintain.

## Key Files

| File | Purpose |
|---|---|
| `composeApp/src/commonMain/.../data/BudgetManager.kt` | Usage accounting, day rollover, budget config, and the `mayRunAutonomous` policy |
| `composeApp/src/commonMain/.../data/RemoteDataRepository.kt` | Records usage after each LLM call; tags the active source; exposes budget accessors |
| `composeApp/src/commonMain/.../data/TaskScheduler.kt` | Gates heartbeat + scheduled tasks on the governor; one-time breach notification |
| `composeApp/src/commonMain/.../data/AppSettings.kt` | Persisted budget config and usage JSON |
| `composeApp/src/commonMain/.../network/dtos/openaicompatible/OpenAICompatibleChatResponseDto.kt` | `usage` parsing + `tokenUsage()` |
| `composeApp/src/commonMain/.../network/dtos/anthropic/AnthropicChatResponseDto.kt` | `usage` parsing + `tokenUsage()` |
| `composeApp/src/commonMain/.../network/dtos/gemini/GeminiChatResponseDto.kt` | `usageMetadata` parsing + `tokenUsage()` |
| `composeApp/src/commonMain/.../ui/settings/BudgetSection.kt` | Budget settings UI |
| `composeApp/src/commonMain/.../ui/settings/SettingsViewModel.kt` | Budget state + action handlers |
