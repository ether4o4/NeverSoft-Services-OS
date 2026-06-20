# Headless Engine Bridge

**Last verified:** 2026-06-09

MorsVitaEst can run as a pure backend — its full engine with none of its own
screens — so a separate host application can supply the user interface while MVE
supplies the intelligence, the providers, the memory, and the Linux sandbox.
This is what lets the NeverSoft OS launcher act as the desktop "shell" while MVE
acts as the underlying "kernel."

## Why it exists

The MVE Compose app is two layers stacked together: an engine (everything that
talks to providers, runs tools, stores conversations, drives the sandbox) and a
set of screens on top. A host that already has its own UI only wants the engine.
The headless bridge exposes that lower layer on its own, without requiring the
screens or the Compose runtime to be present.

The MVE app itself is unchanged by this — it still loads the engine and the
screens together, exactly as before. The engine is simply also reachable on its
own.

## Concepts

### Engine vs. UI separation

The dependency graph is described in two halves: an engine half (every service
the app needs to function) and a UI half (only the screen-backing pieces). The
full app loads both; a headless host loads only the engine half. Splitting them
changed no behavior for the existing app — the same services are wired the same
way — it only made the engine independently loadable.

### Headless boot

A host starts the engine through a single entry point, gets back a handle to the
running kernel, and later stops it to release resources. Booting in this mode
brings up the entire engine — conversations, providers, memory, tasks, MCP, the
sandbox controller — with no screens attached. A host that owns platform-
specific pieces (on Android, the real proot sandbox) contributes them at boot
time; on desktop those pieces are no-op stubs, which is why the engine can boot
and be tested with no Android runtime present.

### Bridge surface

The host doesn't touch the engine's internals directly. It talks to a curated
facade that exposes the slice a shell actually drives:

- **Chat** — observe the live transcript, send a turn through the full pipeline,
  start a new conversation, clear the current one.
- **Providers** — list configured services, read and set API keys, enable or
  disable each one.
- **Core toggles** — sandbox on/off, daemon (24/7 background) on/off.
- **Sandbox** — check status, trigger first-run setup, run shell commands, and
  list/read/write files in the Linux environment.

Everything on the facade either forwards straight to the engine or builds on the
sandbox; it adds no parallel state of its own.

### Convenience helpers

Two higher-level helpers sit on top of the sandbox because hosts ask for them
repeatedly:

- **Keyword filename search** — walk a directory and return every file whose
  name contains a search term, for "find everything about X" workflows.
- **Validation-gated write** — stage proposed file content, run a caller-supplied
  check command against the staged copy, and only promote it to the real path if
  the check passes. A failed check leaves the existing file untouched, so a
  broken edit cannot take down a working component. This is the building block
  for "edit this and don't let me save it until it actually runs."

## Platform notes

Headless boot works on every target the engine compiles for. The sandbox and
on-device inference remain Android-only; on other platforms they are inert, so a
headless host on desktop gets chat and configuration but the no-op sandbox.

## Key Files

| File | Purpose |
|---|---|
| `composeApp/src/commonMain/.../AppModule.kt` | Engine half / UI half split; full graph composes both |
| `composeApp/src/commonMain/.../bridge/HeadlessEngine.kt` | Boots the engine with no UI; returns the kernel handle |
| `composeApp/src/commonMain/.../bridge/MveEngine.kt` | Curated facade the host calls; chat, providers, toggles, sandbox, helpers |
| `composeApp/src/desktopTest/.../bridge/HeadlessEngineTest.kt` | Proves the engine boots headless without Compose |
| `composeApp/src/commonMain/.../data/DataRepository.kt` | The engine interface the facade delegates to |
| `composeApp/src/commonMain/.../SandboxController.kt` | The Linux sandbox interface the facade delegates to |
