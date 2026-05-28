<p align="center">
  <img src="assets/banner.svg" alt="MorsVitaEst — The Final AI Infrastructure" width="100%" />
</p>

# MorsVitaEst

MorsVitaEst is a mobile-first agent infrastructure project: an AI companion that can talk, reason, use tools, run local models, and grow into a practical operating layer for personal automation.

The name means **death is life**. The project is built around that idea: finished systems should not stay frozen. They should shed old shapes, absorb better ones, and keep becoming more useful without losing their center.

## What We Are Building Toward

MorsVitaEst is not meant to be a single chatbot, a single model wrapper, or a one-off assistant app. The direction is a living agent environment that can run on a phone first, expand through connected tools, and coordinate multiple internal modes of thought without making the user manage every moving part.

- **Mobile-first AI runtime** for Android, with desktop and web kept as useful secondary targets
- **Agent host support** for local models, Termux agents, PowerShell agents, MCP agents, human-approved workflows, and API-key-backed providers
- **Local model support** through Ollama, Termux, OpenAI-compatible endpoints, and on-device engines where practical
- **Multi-agent orchestration** based around five color-coded modes that can separate planning, safety, execution, memory, and creative synthesis
- **Infrastructure-owned tool execution** for search, shell, files, notifications, scheduling, MCP servers, and local bridge commands
- **Voice-ready interface** that can support spoken replies and agent-specific voice personalities
- **Persistent personal context** that helps the system feel continuous without turning every detail into visible machinery
- **Heartbeat behavior** for periodic self-checks, pending work, and quiet resurfacing of things that matter
- **Extensible skill layer** where new capabilities can be reviewed, added, tested, and used without rebuilding the whole app

There is no final version in the usual sense. The end goal is a system designed to keep unfolding: stable enough to trust, flexible enough to change, and personal enough that it becomes more than a generic assistant.

## Core Concept

MorsVitaEst is the agent layer between the user, local devices, remote models, and useful tools.

It should be able to:

- Talk like a companion
- Think like a planner
- Act like an operator
- Remember like a collaborator
- Run locally whenever possible
- Reach outward only when needed
- Stay understandable to the person using it

The app should feel direct and capable, not like a dashboard full of chores. The user should be able to open it on a phone, ask for something real, and watch the system decide which model, tool, mode, or agent lane should handle it.

## Current Foundation

The current codebase already includes major pieces worth carrying forward:

- Cross-platform Kotlin/Compose application structure
- Android, desktop, iOS, and web targets
- Multi-service LLM provider support
- OpenAI-compatible endpoint support
- Local/on-device inference path
- MCP server support
- Dynamic and interactive UI rendering
- Memory, scheduling, heartbeat, and task infrastructure
- Secure settings import/export
- Encrypted local conversation storage
- Tool execution pathways
- Text-to-speech support
- Android Linux sandbox support

MorsVitaEst keeps those capabilities, then redirects the product around agent infrastructure instead of novelty demos or unrelated integrations.

## Mobile-First Local AI

The phone matters. MorsVitaEst should not assume a cloud server, a desktop, or a perfect network.

Primary mobile priorities:

- Connect to local Ollama from Android/Termux when available
- Support OpenAI-compatible local endpoints
- Detect and list usable local models
- Fall back cleanly between local and remote providers
- Keep sensitive keys and config in secure storage
- Provide clear health states when a model server is offline
- Make small LLMs useful through tighter prompts, smaller context, and focused tool routes

The target experience is simple: if a model is running on the phone, MorsVitaEst should be able to see it, use it, and explain what is happening without making the setup feel like a science project.

## Five-Color Agent System

The color framework is the organizing language for the app's internal agency.

- **Red**: action, shell, tools, direct execution
- **Blue**: structure, planning, architecture, review
- **Green**: growth, research, synthesis, capability expansion
- **Yellow**: user experience, communication, visibility, guidance
- **Purple**: safety, boundaries, identity, long-range coherence

The point is not to create five gimmick personas. The point is to split responsibility so the system can move fast without becoming careless. A single request may pass through one color or several depending on risk, complexity, and context.

## Agent Infrastructure

MorsVitaEst is being shaped as an infrastructure app, not just a chat screen.

Core systems:

- **Conversation core**: normal chat, tool calls, model routing, attachments, generated UI
- **Agent host core**: API agents, local model agents, Termux agents, PowerShell agents, MCP agents, and human approval lanes
- **Runtime core**: provider selection, local endpoint health, model capability metadata, runtime endpoint metadata
- **Tool core**: infrastructure-owned MCP servers, native tools, local bridge, shell/sandbox execution, and per-agent authority
- **Memory core**: personal context, preferences, lessons, durable continuity
- **Heartbeat core**: periodic review, scheduled work, quiet reminders, background readiness
- **Skill core**: installable capabilities with metadata, permissions, tests, and rollback paths
- **Safety core**: permission boundaries, budget limits, local/remote separation, explicit user control

The deeper growth layer should stay quiet from the outside. The user sees useful capability. The system handles the shape beneath it.

## Feature Direction

### MVP

- Android-first app identity under MorsVitaEst
- Working chat with local and remote model providers
- Hosted agent profiles for API, local model, Termux, PowerShell, sandbox, MCP, and human-mediated runtimes
- Ollama/Termux endpoint setup and model discovery
- Five-color agent mode selector and routing metadata
- Local tool bridge for private shell, fetch, and voice workflows
- Infrastructure-owned tool authority with approval-required and autonomous modes
- MCP server connection management
- Memory, tasks, heartbeat, and settings import/export
- Clear provider health checks and fallback behavior

### Next Layer

- Agent lane visualization without clutter
- Voice personalities per color or agent role
- Skill registry with trust levels and install states
- Local model profiles for small LLM behavior
- Mobile sandbox workflows for Python, Node, git, and package installs
- Better background execution controls on Android

### Long Arc

- A personal operating layer that can coordinate local models, remote models, tools, files, voice, and scheduled work
- A system that improves through use without making the user babysit every improvement
- An agent that feels continuous across sessions, devices, and tasks

## Installation

Direct builds will be published through GitHub Releases as the project stabilizes:

| Platform | Format | Download |
|----------|--------|----------|
| Android | APK | [GitHub Releases](https://github.com/ether4o4/MorsVitaEst/releases) |
| macOS | DMG | [GitHub Releases](https://github.com/ether4o4/MorsVitaEst/releases) |
| Windows | MSI | [GitHub Releases](https://github.com/ether4o4/MorsVitaEst/releases) |
| Linux | DEB/RPM/AppImage | [GitHub Releases](https://github.com/ether4o4/MorsVitaEst/releases) |
| Web | Static/Web app | [Project site](https://ether4o4.github.io/MorsVitaEst/) |

## Local Model Notes

For Android-first local work, the expected path is:

1. Run a compatible model server on-device or nearby.
2. Point MorsVitaEst at the local OpenAI-compatible or Ollama endpoint.
3. Let the app test the endpoint, list models, and route requests based on what is available.

The app should support cloud models, but the soul of the project is local-first control.

## Supported AI Services

The foundation supports a broad provider layer, including:

Anthropic, OpenAI, Gemini, DeepSeek, Mistral, xAI, OpenRouter, Groq, NVIDIA, Cerebras, Ollama Cloud, Together AI, Hugging Face, Venice AI, Moonshot AI, Z.AI, MiniMax, AIHubMix, Deep Infra, Fireworks AI, OpenCode, OpenAI-compatible APIs, and local/on-device engines.

## MCP Servers

MorsVitaEst supports the [Model Context Protocol](https://modelcontextprotocol.io/) for external tools.

Useful server categories:

- Fetch and web content tools
- Documentation and code lookup
- Structured reasoning tools
- Search and knowledge tools
- Local or private tool bridges
- Domain-specific automation endpoints

MCP is one of the main ways MorsVitaEst becomes more than a text box.

## Development Notes

Common useful commands:

```bash
./gradlew :composeApp:desktopTest
./gradlew :composeApp:assembleDebug
./gradlew :screenshotTests:updateScreenshots
```

Android and local-model workflows should be treated as first-class during development. A change that only works comfortably on desktop is not finished.

## Project Direction

MorsVitaEst is a rebuild of intent, not just a rename. The foundation gives us a working cross-platform assistant. The work now is to make it a personal agent system with its own identity:

- Less demo surface
- More real capability
- Less generic assistant
- More mobile operator
- Less static app
- More living infrastructure

MorsVitaEst is meant to keep becoming.
