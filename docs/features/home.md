# Home & Navigation

**Last verified:** 2026-06-07

The app opens to a brushed-metal home screen (Page 1). A two-tab bar — **Home** and **Workspace** —
switches between the home and the chat/sandbox workspace. The bar is shown on the home for every
platform so phones can always reach the workspace; in the workspace the same bar appears on larger
screens, while phones rely on the workspace's own controls and the system back gesture.

## Home (Page 1)

- **Title plate** — the wordmark.
- **Compare action** — a prominent button that opens the Compare screen (see [compare.md](compare.md)).
- **News feed** — a live feed at the top, sourced from the Heartbeat engine: each heartbeat update is
  a card, newest first. Pull down (or tap the refresh icon) to run a heartbeat immediately and reload.
  When there are no updates yet, a hint points to the Heartbeat settings. See [heartbeat.md](heartbeat.md).
- **Integration boxes** — Services, MCP, Hugging Face, Ollama, and an LLM chooser. Each box carries its
  own gear that opens the matching settings section directly (the Services box opens the Services tab,
  the MCP box opens the Tools tab, and so on) rather than a generic settings screen.

## Workspace

The Workspace tab is the chat and sandbox screen (see [chat.md](chat.md) and [sandbox.md](sandbox.md)).
Settings is reached from the workspace's settings icon and from the home's per-box gears; it is a
pushed screen with a back button rather than a top-level tab.

## Key Files

| File | Purpose |
|---|---|
| `composeApp/src/commonMain/kotlin/com/ether4o4/morsvitaest/App.kt` | Navigation graph, the Home/Workspace tab bar, and route wiring |
| `composeApp/src/commonMain/kotlin/com/ether4o4/morsvitaest/ui/foundry/FoundryHome.kt` | Home layout: title plate, compare action, news feed, integration boxes |
| `composeApp/src/commonMain/kotlin/com/ether4o4/morsvitaest/ui/foundry/FoundryHomeViewModel.kt` | Builds the news feed from heartbeat updates; pull-to-refresh trigger |
| `composeApp/src/commonMain/kotlin/com/ether4o4/morsvitaest/ui/foundry/FoundryComponents.kt` | Brushed-metal pills, cards, and tiles |
| `composeApp/src/commonMain/kotlin/com/ether4o4/morsvitaest/ui/foundry/FoundryTokens.kt` | The metal finish tokens (brushed fills, bevels, gloss) |
