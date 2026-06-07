# Home & Navigation

**Last verified:** 2026-06-07

The app opens to a brushed-metal home screen (Page 1). A two-tab bar — **Home** and **Workspace** —
switches between the home and the chat/sandbox workspace. The bar is shown on the home for every
platform so phones can always reach the workspace; in the workspace the same bar appears on larger
screens, while phones rely on the workspace's own controls and the system back gesture.

## Home (Page 1)

- **Title plate** — the wordmark.
- **Compare action** — a prominent button that opens the workspace on its Multi chat tab (see [compare.md](compare.md)).
- **News feed** — a live feed at the top, sourced from the Heartbeat engine: each heartbeat update is
  a card, newest first. Each row shows a thumbnail: if the update's text carries an image (a markdown
  image or a bare image link), it loads as the thumbnail; otherwise the row falls back to the source's
  initial. Pull down (or tap the refresh icon) to run a heartbeat immediately and reload. When there
  are no updates yet, a hint points to the Heartbeat settings. See [heartbeat.md](heartbeat.md).
- **Integration boxes** — Services, MCP, Hugging Face, Ollama, and an LLM chooser. Tapping a box's body
  jumps to the matching settings section. Tapping its gear opens a small "glass" config sheet first —
  a plain-language blurb plus a tiny numbered recipe and a single button into the full settings — so
  the gear never dead-ends straight onto a dense settings screen.

## Workspace (Page 2)

The Workspace is one box with a tab strip across the top — **Chat**, **Multi chat**, and **Shell** —
plus a settings gear that is always present. Chat is the normal assistant (see [chat.md](chat.md));
Multi chat is the two-model Compare (see [compare.md](compare.md)); Shell is the Alpine sandbox
terminal (see [sandbox.md](sandbox.md)). The Shell tab only appears where the sandbox runtime exists
(Android); other platforms show just Chat and Multi chat.

Settings is reached from the workspace's settings gear (on every tab) and from the home's per-box
gears; it is a pushed screen with a back button rather than a top-level tab. The settings gear is one
of the spots the first-run tour points out.

## Key Files

| File | Purpose |
|---|---|
| `composeApp/src/commonMain/kotlin/com/ether4o4/morsvitaest/App.kt` | Navigation graph, the Home/Workspace tab bar, and route wiring |
| `composeApp/src/commonMain/kotlin/com/ether4o4/morsvitaest/ui/workspace/WorkspaceScreen.kt` | The unified workspace: Chat / Multi chat / Shell tab strip + settings gear |
| `composeApp/src/commonMain/kotlin/com/ether4o4/morsvitaest/ui/foundry/FoundryHome.kt` | Home layout: title plate, compare action, news feed, integration boxes |
| `composeApp/src/commonMain/kotlin/com/ether4o4/morsvitaest/ui/foundry/FoundryHomeViewModel.kt` | Builds the news feed from heartbeat updates; pull-to-refresh trigger |
| `composeApp/src/commonMain/kotlin/com/ether4o4/morsvitaest/ui/foundry/FoundryComponents.kt` | Brushed-metal pills, cards, and tiles |
| `composeApp/src/commonMain/kotlin/com/ether4o4/morsvitaest/ui/foundry/FoundryTokens.kt` | The metal finish tokens (brushed fills, bevels, gloss) |
