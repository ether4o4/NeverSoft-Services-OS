# Compare

**Last verified:** 2026-06-07

Compare runs two models side by side on the same question, and can optionally let them talk to
each other. It is the **Multi chat** tab of the workspace, and is also reached from the
"Compare your favorite LLMs" action on the home screen (which opens the workspace on that tab).

## Layout

Two panes sit side by side. Each pane has its own model picker — drawn from the configured service
instances — at the top, and its own scrolling list of replies below. A shared input at the bottom
sends one prompt to both panes. A **Merge** toggle switches between the two modes. When shown as the
workspace's Multi chat tab, the screen drops its own back control because the tab strip is the
navigation.

## Modes

### Plain compare (Merge off)

Both models answer the same prompt once, independently — neither sees the other's reply. This is a
straight side-by-side comparison of how two models respond to the same question.

### Merge (Merge on)

The two models reply to each other. The first model answers the question; the second responds to the
first; the first responds back; and so on. Each model is capped at **two replies per rotation** — one
rotation runs first, second, first, second and then stops and waits for the next prompt. This rate
limit keeps the automatic back-and-forth from running away (and from burning tokens). On every turn
after the first, the model is given the original question plus the running cross-talk and asked to
respond to the other model's latest point.

## Behavior

- Each pane calls its selected model through the same per-instance request path the rest of the app
  uses; there is no separate inference path.
- If one model errors, that failure appears as that pane's own message instead of aborting the whole
  exchange — the other pane still runs.
- Sending a new prompt cancels any rotation still in flight and clears the panes.
- Compare needs at least one configured service; with none, it shows a hint to add services in
  settings. The built-in Free tier is not offered as a compare pane.

## Key Files

| File | Purpose |
|---|---|
| `composeApp/src/commonMain/kotlin/com/ether4o4/morsvitaest/ui/compare/CompareOrchestrator.kt` | Pure rotation engine: plain vs merge, the two-replies-each cap, prompt construction |
| `composeApp/src/commonMain/kotlin/com/ether4o4/morsvitaest/ui/compare/CompareViewModel.kt` | Pane selection, merge toggle, runs a rotation against the inference layer |
| `composeApp/src/commonMain/kotlin/com/ether4o4/morsvitaest/ui/compare/CompareScreen.kt` | Two-pane UI, per-pane model picker, shared input, merge toggle |
| `composeApp/src/commonTest/kotlin/com/ether4o4/morsvitaest/ui/compare/CompareOrchestratorTest.kt` | Tests for the cap, independence, transcript feeding, and the configurable budget |
