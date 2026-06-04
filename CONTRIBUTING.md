# Contributing to MorsVitaEst

MVE is solo-developed in public. PRs, issues, and ideas are welcome.

## Before you open a PR

1. **Run the test + lint suite locally.**
   ```bash
   ./gradlew :composeApp:desktopTest
   ./gradlew :composeApp:check
   ```
2. **Run the Android build if you touched anything Android-specific.**
   ```bash
   ./gradlew :androidApp:assembleFossDebug
   ```
3. **Update or add a feature doc.** Specs live in [`docs/features/`](./docs/features/). When you modify behavior in an area that has a doc, update the doc + the "Last verified" header date. If your PR introduces a new user-visible feature, add a doc.
4. **Keep PRs focused.** A bug fix, a feature, or a refactor — pick one. Mixed PRs are hard to review.

## Code style

- **Kotlin Multiplatform + Compose.** `commonMain` first; only drop to `androidMain` / `desktopMain` / `iosMain` when you genuinely need a platform API. Most "Android-only" features are really "we haven't backported them yet" — flag that explicitly in the PR.
- **Koin for DI.** No constructor magic, no field injection. If your new class needs the repository, add it to the Koin module.
- **`russhwolf.settings.Settings` for persistence.** No `SharedPreferences`, no `NSUserDefaults`. The KMP wrapper handles both.
- **No telemetry, no analytics, no phone-home.** This is structural to the project. If you need to know "is this code running" — log it locally or add it behind a manual debug-mode toggle.
- **Comments explain *why*, not *what*.** Names should already say what. Reserve comments for hidden invariants, surprising workarounds, or subtle behavior the next reader would miss.

## Where things live

| You want to change… | Look in… |
|---|---|
| The default soul / system prompt | `composeApp/src/commonMain/composeResources/values/strings.xml` (`default_soul`) |
| How prompts are assembled per turn | `composeApp/src/commonMain/kotlin/com/ether4o4/morsvitaest/data/ChatSystemPromptBuilder.kt` |
| A specific AI provider's API call | `composeApp/src/commonMain/kotlin/com/ether4o4/morsvitaest/service/` + the `network/dtos/` package |
| The local GGUF / llama.cpp engine | `composeApp/src/androidMain/assets/sandbox/morsllm.sh` (shell-side) + `composeApp/src/androidMain/kotlin/com/ether4o4/morsvitaest/sandbox/GgufServerManager.kt` (Kotlin-side) |
| The Linux sandbox itself (proot, terminal) | `composeApp/src/androidMain/kotlin/com/ether4o4/morsvitaest/SandboxController.android.kt` |
| MCP tools and popular servers | `composeApp/src/commonMain/kotlin/com/ether4o4/morsvitaest/mcp/` |
| Projects (the persistent-context container) | `composeApp/src/commonMain/kotlin/com/ether4o4/morsvitaest/data/Project.kt` + `ui/settings/ProjectsSettings.kt` |
| Settings UI (any tab) | `composeApp/src/commonMain/kotlin/com/ether4o4/morsvitaest/ui/settings/` |

## Reporting bugs

Open a GitHub issue with:

- The version (Settings → bottom → version)
- Platform (Android / Desktop) + OS version
- What you did, what you expected, what actually happened
- If it's a GGUF / sandbox issue: paste the output of `morsllm diagnose` from the in-app terminal

## Asking questions

PR comments or issues both work. There's no Discord / forum yet — feel free to suggest one in an issue if you want to organize community.

## Sponsoring

MVE is open source and built by one person. Sponsorship pays for hardware, signing certs, store fees, and time. See [SPONSORS.md](./SPONSORS.md).

## License

MVE is open source. By contributing you agree your contribution will be licensed under the same license as the project.
