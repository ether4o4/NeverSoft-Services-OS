# Kairos Infrastructure

**Last verified:** 2026-05-26

Kairos turns MorsVitaEst from an assistant app into a governed agent substrate. The app keeps the MorsVitaEst feature floor: persistent memory, soul, multi-provider fallback, on-device inference, tool execution, MCP, heartbeat, encrypted storage, TTS, Linux sandbox, dynamic UI, and mobile-first Android operation. Kairos adds a capability-growth loop around that floor.

## Core idea

Kairos does not blindly install agent frameworks. It discovers, reads, scores, quarantines, stages, and promotes capabilities. Code from Agent Zero, OpenClaw, Hermes-style agents, ZeroClaw, Runkai, Colour_Ceauxdid, or future GitHub trends is source material until it earns trust.

## Capability

A capability is any portable improvement unit:

- agent profile or swarm role
- tool definition
- skill/prompt/workflow
- provider adapter
- eval harness
- voice pipeline
- Termux/Ollama/local-model recipe
- governance pattern such as budget, kill switch, cache, circuit breaker, ledger, or promotion gate

## Decisions

| Decision | Meaning |
|---|---|
| `PROMOTE_CANDIDATE` | Safe enough to propose for trusted implementation, still human-promoted |
| `STAGE` | Useful but incomplete; needs docs, license, tests, or wrapper design |
| `QUARANTINE` | Potentially useful but privileged; do not execute directly |
| `REJECT` | Destructive, untrusted, or malformed |

## Mobile-first boundary

Termux/Ollama support is core scope. Any imported capability must either work on Android directly, work inside MorsVitaEst's Linux sandbox, or be expressible as a mobile-safe wrapper. Desktop-only capabilities may be retained as reference patterns but cannot redefine the app architecture.

## Swarm direction

Colour_Ceauxdid's color agents become one Kairos mind split for safety and implementation:

- Red/Spark: execution and tools
- Blue/Mythos: architecture and continuity
- Green: implementation/runtime
- Yellow: exploration and discovery
- Purple: synthesis and user-facing voice

The split is a routing and review discipline, not five unrelated chatbots.

## Key files

| File | Purpose |
|---|---|
| `composeApp/src/commonMain/.../kairos/CapabilityVetter.kt` | Pure vetting logic for discovered capabilities |
| `composeApp/src/commonMain/.../kairos/CapabilityHeartbeat.kt` | Standing heartbeat prompt for capability discovery |
| `composeApp/src/commonMain/.../kairos/KairosSwarm.kt` | Five-color routing primitive for splitting one Kairos mind by risk and task shape |
| `composeApp/src/commonTest/.../kairos/CapabilityVetterTest.kt` | Regression tests for promotion/quarantine/reject decisions |
