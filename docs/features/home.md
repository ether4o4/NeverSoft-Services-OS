# Home & Navigation

**Last verified:** 2026-06-22

The app opens to a brushed-metal home screen (Page 1). A two-tab bar — **Home** and **Workspace** —
switches between the home and the chat/sandbox workspace. The bar is shown on the home for every
platform so phones can always reach the workspace; in the workspace the same bar appears on larger
screens, while phones rely on the workspace's own controls and the system back gesture.

## Home (Page 1)

The home has no background panel of its own: two equal-height boxes (News and Heartbeat) sit directly
on the screen with nothing boxed behind them. Both boxes are tinted with the **launcher theme** — the
same color that paints the taskbar, Start menu, keyboard, and widgets — and re-tint live the moment
the theme changes. (Multi chat is no longer a pill here; it's a tab in the clock pop-up's
Chat / Shell / Multi chat workspace.)

- **News box (top)** — a manual-refresh news reader. Each story is one of the user's RSS/Atom sources'
  newest items and shows the story's **own** article picture (pulled from the feed's media / enclosure /
  inline-image markup), not the website's favicon. Tapping a row opens the article in the browser. A
  refresh icon (↻) reloads on demand — the box does not auto-poll. The ＋ icon opens a sheet to paste
  RSS/Atom source links or remove them; until the user adds any, a built-in set of image-rich tech
  sources is used. When empty, a hint points to the refresh and add-source actions.
- **Heartbeat box (bottom)** — the assistant's "what's new" updates: each assistant message in the
  dedicated heartbeat conversation is a card, newest first, with an inline-image thumbnail when the
  update carries one. Pull down (or tap ↻) to run a heartbeat immediately and reload. When there are no
  updates yet, a hint points to the Heartbeat settings. See [heartbeat.md](heartbeat.md).

The provider/MCP integration entries (Services, MCP, Hugging Face, Ollama, LLM chooser) are **not** on
this page; they live in Settings, reachable from the settings gear.

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
| `composeApp/src/commonMain/kotlin/com/ether4o4/morsvitaest/ui/foundry/FoundryHome.kt` | Home layout: floating themed Multi-chat pill + themed News box + Heartbeat box, news-sources sheet (no background panel) |
| `composeApp/src/commonMain/kotlin/com/ether4o4/morsvitaest/ui/foundry/FoundryHomeViewModel.kt` | Builds the heartbeat feed and the RSS news feed; manual refresh + source management |
| `composeApp/src/commonMain/kotlin/com/ether4o4/morsvitaest/data/NewsRepository.kt` | Fetches and parses RSS/Atom sources into stories with real article thumbnails |
| `composeApp/src/commonMain/kotlin/com/ether4o4/morsvitaest/ui/foundry/FoundryComponents.kt` | Brushed-metal pills, cards, and tiles |
| `composeApp/src/commonMain/kotlin/com/ether4o4/morsvitaest/ui/foundry/FoundryTokens.kt` | The metal finish tokens (brushed fills, bevels, gloss) |
