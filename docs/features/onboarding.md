# Onboarding & Help

**Last verified:** 2026-06-07

MorsVitaEst ships ready to use: a free, built-in AI is on from the first launch with no account
and no API key. To make that obvious — and to point newcomers at the parts of the app that are
easy to miss — the app has a one-time welcome tour and an always-available help assistant.

## First-run welcome tour

The very first time the app is opened, a short stepped welcome appears over the home screen. It
covers, in four quick cards:

1. The built-in AI is already on — just open the Workspace and start chatting.
2. The help bubble (the round “?”) opens the assistant for help at any time.
3. The Workspace is one screen with three tabs — Chat, Multi chat, and Shell — and the gear there
   opens Settings.
4. You can add your own models in Services and connect tool servers via MCP, and the home boxes'
   gears jump straight to each.

The user can step Back/Next, or Skip at any time. Once finished or skipped it does not appear
again on later launches. The help panel can replay it on demand, which does not change whether it
auto-shows.

## Help assistant (tap-to-help bubble)

A round “?” bubble floats in the corner of the home hub; the Workspace carries its own “?” chip in
its tab strip (so the bubble never covers the chat input), and the Settings screens lead with their
own inline guide cards. Tapping the bubble or the chip opens the **Help & setup** panel, which has:

- **A one-line-each guide** to Chat, Multi chat, Shell, Services, and MCP.
- **Quick action buttons** that actually take you where you need to go: *Add an AI* opens the
  Services settings, *Connect MCP* opens the Tools settings, and *Replay tour* re-shows the welcome.
- **A live chat** with the built-in assistant. It runs against the free built-in tier specifically —
  not whatever provider you've since selected — so help is available the instant the app is
  installed, before anything is configured. The assistant is briefed on the app's layout so it can
  explain setup steps in plain language; prompt suggestions seed common questions like adding a model
  or connecting an MCP server.

## Key Files

| File | Purpose |
|---|---|
| `composeApp/src/commonMain/kotlin/com/ether4o4/morsvitaest/ui/onboarding/WelcomeTour.kt` | The stepped first-run welcome/tour overlay |
| `composeApp/src/commonMain/kotlin/com/ether4o4/morsvitaest/ui/help/HelpBubble.kt` | The floating bubble and the Help & setup panel (guide, quick actions, suggestions, chat) |
| `composeApp/src/commonMain/kotlin/com/ether4o4/morsvitaest/ui/help/HelpAssistantViewModel.kt` | Single-turn help chat backed by the free built-in AI, with the app-aware setup persona |
| `composeApp/src/commonMain/kotlin/com/ether4o4/morsvitaest/App.kt` | Shows the tour on first launch, hosts the bubble/panel, and wires the quick-action deep links |
| `composeApp/src/commonMain/kotlin/com/ether4o4/morsvitaest/data/AppSettings.kt` | Stores whether the welcome has been seen |
