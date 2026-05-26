package com.ether4o4.morsvitaest .morsvitaestros

object CapabilityHeartbeat {
    const val DEFAULT_DISCOVERY_TASK_DESCRIPTION = "Kairos capability discovery"

    val defaultDiscoveryTaskPrompt: String = """
        Run a lightweight Kairos capability discovery pass.

        Goal: find useful AI agent frameworks, tools, skills, provider adapters, eval harnesses,
        voice systems, local-LLM/Termux patterns, and mobile-first agent infrastructure patterns
        that could improve this app without breaking the MorsVitaEst feature floor.

        Scope:
        - Prioritize mobile-first, Android, Termux, Ollama, local model, MCP, tool-routing,
          memory, swarm, voice, eval, and safety/governance capabilities.
        - Treat ether4o4/MorsVitaEst features as the non-regression baseline.
        - Treat Colour_Ceauxdid's voice and color-swarm logic as source material for Kairos.
        - Watch Agent Zero, OpenClaw, Hermes-style agents, ZeroClaw, Runkai, and related repos.

        Rules:
        - Do not install, execute, or trust downloaded code during discovery.
        - Summarize candidates as: name, URL, capability, why useful, requested permissions,
          risk, and whether it should be staged, quarantined, or ignored.
        - Prefer extracting patterns into safe native code over copying foreign code.
        - If nothing is worth attention, respond exactly: HEARTBEAT_OK
    """.trimIndent()
}
