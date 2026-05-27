package com.ether4o4.morsvitaest.kairos

import kotlinx.serialization.Serializable

@Serializable
data class MorsToolRegistration(
    val id: String,
    val name: String,
    val description: String,
    val permissions: Set<CapabilityPermission> = emptySet(),
    val mobileSafe: Boolean = true,
    val writesState: Boolean = false,
)

@Serializable
data class MorsRuntimeSnapshot(
    val agents: List<HostedAgentProfile>,
    val tools: List<MorsToolRegistration>,
)

object MorsRuntimeRegistry {
    val defaultTools = listOf(
        MorsToolRegistration(
            id = "ollama-api",
            name = "Ollama API",
            description = "List local models and route chat or generation through an Ollama-compatible endpoint.",
            permissions = setOf(CapabilityPermission.NETWORK, CapabilityPermission.READ_ONLY),
            mobileSafe = true,
        ),
        MorsToolRegistration(
            id = "termux-command",
            name = "Termux Command",
            description = "Send a constrained command to the mobile Termux bridge.",
            permissions = setOf(CapabilityPermission.SHELL, CapabilityPermission.DEVICE_CONTROL),
            mobileSafe = true,
            writesState = true,
        ),
        MorsToolRegistration(
            id = "repo-inspector",
            name = "Repo Inspector",
            description = "Read project files, summarize structure, and prepare implementation plans.",
            permissions = setOf(CapabilityPermission.FILESYSTEM_READ, CapabilityPermission.READ_ONLY),
            mobileSafe = true,
        ),
        MorsToolRegistration(
            id = "file-edit",
            name = "File Edit",
            description = "Apply scoped code or configuration changes inside an approved workspace.",
            permissions = setOf(CapabilityPermission.FILESYSTEM_READ, CapabilityPermission.FILESYSTEM_WRITE),
            mobileSafe = false,
            writesState = true,
        ),
        MorsToolRegistration(
            id = "api-model",
            name = "API Model",
            description = "Route reasoning to an OpenAI-compatible or custom provider endpoint.",
            permissions = setOf(CapabilityPermission.NETWORK, CapabilityPermission.SECRET_ACCESS),
            mobileSafe = true,
        ),
    )

    val defaultAgents = listOf(
        HostedAgentProfile(
            id = "mobile-operator",
            name = "Mobile Operator",
            runtime = AgentRuntimeEndpoint(
                kind = AgentRuntimeKind.TERMUX,
                displayName = "Termux Ollama",
                baseUrl = "http://127.0.0.1:11434",
            ),
            colorRole = KairosColorRole.RED,
            toolAuthority = ToolAuthority.APPROVAL_REQUIRED,
            allowedToolIds = setOf("ollama-api", "termux-command", "repo-inspector"),
            notes = "Default local execution slot for phone-first Ollama and Termux work.",
        ),
        HostedAgentProfile(
            id = "architect",
            name = "Runtime Architect",
            runtime = AgentRuntimeEndpoint(
                kind = AgentRuntimeKind.API,
                displayName = "OpenAI-compatible planner",
            ),
            colorRole = KairosColorRole.BLUE,
            toolAuthority = ToolAuthority.READ_ONLY,
            allowedToolIds = setOf("repo-inspector", "api-model"),
            notes = "Planning and verification slot. Reads context before Mors executes.",
        ),
    )

    fun snapshot(): MorsRuntimeSnapshot = MorsRuntimeSnapshot(
        agents = defaultAgents,
        tools = defaultTools,
    )
}

object MorsExecutionThrone {
    private val privilegedPermissions = setOf(
        CapabilityPermission.BACKGROUND_AUTONOMY,
        CapabilityPermission.DEVICE_CONTROL,
        CapabilityPermission.FILESYSTEM_WRITE,
        CapabilityPermission.INSTALL_DEPENDENCIES,
        CapabilityPermission.OUTBOUND_MESSAGE,
        CapabilityPermission.SECRET_ACCESS,
        CapabilityPermission.SHELL,
    )

    fun validate(
        packet: CouncilMissionPacket,
        snapshot: MorsRuntimeSnapshot = MorsRuntimeRegistry.snapshot(),
    ): CouncilIntakeReport {
        val agentsById = snapshot.agents.associateBy { it.id }
        val toolsById = snapshot.tools.associateBy { it.id }
        val findings = mutableListOf<CouncilIntakeFinding>()

        if (packet.missionId.isBlank()) {
            findings += CouncilIntakeFinding(
                code = "missing_mission_id",
                message = "Council packet must carry a stable mission id.",
                severity = FindingSeverity.CRITICAL,
            )
        }

        if (packet.title.isBlank() || packet.intent.isBlank()) {
            findings += CouncilIntakeFinding(
                code = "missing_intent",
                message = "Council packet must include a title and execution intent.",
                severity = FindingSeverity.HIGH,
            )
        }

        if (packet.authority.maxToolCalls <= 0) {
            findings += CouncilIntakeFinding(
                code = "invalid_tool_budget",
                message = "Tool call budget must allow at least one call.",
                severity = FindingSeverity.HIGH,
            )
        }

        if (packet.tools.size > packet.authority.maxToolCalls) {
            findings += CouncilIntakeFinding(
                code = "tool_budget_exceeded",
                message = "Council requested ${packet.tools.size} tools, above the ${packet.authority.maxToolCalls} tool-call budget.",
                severity = FindingSeverity.HIGH,
            )
        }

        if (packet.risk == CouncilMissionRisk.CRITICAL && packet.authority.executionMode != CouncilExecutionMode.PLAN_ONLY) {
            findings += CouncilIntakeFinding(
                code = "critical_requires_plan_only",
                message = "Critical-risk missions are held at plan-only until a human promotes them.",
                severity = FindingSeverity.CRITICAL,
            )
        }

        val agentDecisions = packet.agents.map { allocation ->
            val agent = agentsById[allocation.agentId]
            when {
                agent == null -> CouncilIntakeAgentDecision(
                    agentId = allocation.agentId,
                    accepted = false,
                    reason = "No hosted agent is registered for this allocation.",
                )

                allocation.preferredRuntime != null && agent.runtime.kind != allocation.preferredRuntime -> CouncilIntakeAgentDecision(
                    agentId = allocation.agentId,
                    accepted = false,
                    reason = "Registered runtime is ${agent.runtime.kind}, not ${allocation.preferredRuntime}.",
                )

                else -> CouncilIntakeAgentDecision(
                    agentId = allocation.agentId,
                    accepted = true,
                    reason = "Hosted agent is registered and runtime-compatible.",
                )
            }
        }

        val acceptedAgentIds = agentDecisions.filter { it.accepted }.map { it.agentId }.toSet()
        if (packet.agents.isEmpty()) {
            findings += CouncilIntakeFinding(
                code = "no_agent_allocated",
                message = "Council did not allocate an agent; Mors can validate but will not execute.",
                severity = FindingSeverity.WARNING,
            )
        } else if (acceptedAgentIds.isEmpty()) {
            findings += CouncilIntakeFinding(
                code = "no_agent_accepted",
                message = "No allocated agent passed runtime validation.",
                severity = FindingSeverity.HIGH,
            )
        }

        val authority = packet.authority
        val toolDecisions = packet.tools.map { allocation ->
            val tool = toolsById[allocation.toolId]
            when {
                tool == null -> CouncilIntakeToolDecision(
                    toolId = allocation.toolId,
                    accepted = false,
                    requiresApproval = false,
                    reason = "Tool is not registered in the Mors runtime.",
                )

                authority.mobileSafeOnly && !tool.mobileSafe -> CouncilIntakeToolDecision(
                    toolId = allocation.toolId,
                    accepted = false,
                    requiresApproval = false,
                    reason = "Tool is not marked mobile-safe for this mission envelope.",
                )

                !authority.allowNetwork && CapabilityPermission.NETWORK in tool.permissions -> CouncilIntakeToolDecision(
                    toolId = allocation.toolId,
                    accepted = false,
                    requiresApproval = false,
                    reason = "Mission envelope blocks network access.",
                )

                !authority.allowFilesystemWrite && CapabilityPermission.FILESYSTEM_WRITE in tool.permissions -> CouncilIntakeToolDecision(
                    toolId = allocation.toolId,
                    accepted = false,
                    requiresApproval = false,
                    reason = "Mission envelope blocks filesystem writes.",
                )

                !authority.allowShell && CapabilityPermission.SHELL in tool.permissions -> CouncilIntakeToolDecision(
                    toolId = allocation.toolId,
                    accepted = false,
                    requiresApproval = false,
                    reason = "Mission envelope blocks shell execution.",
                )

                !authority.allowSecretAccess && CapabilityPermission.SECRET_ACCESS in tool.permissions -> CouncilIntakeToolDecision(
                    toolId = allocation.toolId,
                    accepted = false,
                    requiresApproval = false,
                    reason = "Mission envelope blocks secret access.",
                )

                else -> {
                    val privileged = tool.permissions.any { it in privilegedPermissions } || tool.writesState
                    val needsApproval = authority.executionMode == CouncilExecutionMode.APPROVAL_REQUIRED ||
                        packet.risk == CouncilMissionRisk.HIGH ||
                        privileged

                    CouncilIntakeToolDecision(
                        toolId = allocation.toolId,
                        accepted = true,
                        requiresApproval = needsApproval,
                        reason = if (needsApproval) {
                            "Tool accepted, but Mors requires approval before execution."
                        } else {
                            "Tool accepted under the current mission envelope."
                        },
                    )
                }
            }
        }

        val rejectedRequiredTools = packet.tools
            .filter { it.required }
            .map { it.toolId }
            .filter { toolId -> toolDecisions.any { it.toolId == toolId && !it.accepted } }

        if (rejectedRequiredTools.isNotEmpty()) {
            findings += CouncilIntakeFinding(
                code = "required_tool_blocked",
                message = "Required tools were blocked: ${rejectedRequiredTools.joinToString()}.",
                severity = FindingSeverity.HIGH,
            )
        }

        val status = when {
            findings.any { it.severity == FindingSeverity.CRITICAL } -> CouncilIntakeStatus.REJECTED
            agentDecisions.any { !it.accepted } || rejectedRequiredTools.isNotEmpty() -> CouncilIntakeStatus.PARTIAL
            toolDecisions.any { it.requiresApproval } -> CouncilIntakeStatus.NEEDS_APPROVAL
            else -> CouncilIntakeStatus.ACCEPTED
        }

        return CouncilIntakeReport(
            missionId = packet.missionId,
            status = status,
            acceptedAgents = agentDecisions,
            toolDecisions = toolDecisions,
            findings = findings,
        )
    }

    fun sampleCouncilPacket(): CouncilMissionPacket = CouncilMissionPacket(
        missionId = "mors-mobile-runtime-001",
        title = "Attach local mobile model and inspect execution path",
        intent = "Use a Termux-hosted model as one execution agent while Mors owns the tool boundary.",
        risk = CouncilMissionRisk.MEDIUM,
        agents = listOf(
            CouncilAgentAllocation(
                agentId = "mobile-operator",
                role = "mobile execution",
                preferredRuntime = AgentRuntimeKind.TERMUX,
                reason = "Run small local models and Termux-safe checks from the phone.",
            ),
            CouncilAgentAllocation(
                agentId = "architect",
                role = "plan verification",
                preferredRuntime = AgentRuntimeKind.API,
                reason = "Review the allocation before execution.",
            ),
        ),
        tools = listOf(
            CouncilToolAllocation(
                toolId = "ollama-api",
                reason = "List available Termux/Ollama models and route chat.",
            ),
            CouncilToolAllocation(
                toolId = "repo-inspector",
                reason = "Read the local project state before changing anything.",
            ),
            CouncilToolAllocation(
                toolId = "termux-command",
                reason = "Run tightly scoped local commands after approval.",
            ),
            CouncilToolAllocation(
                toolId = "file-edit",
                reason = "Requested by the Council, blocked until a wider envelope grants write access.",
                required = false,
            ),
        ),
        skills = listOf(
            CouncilSkillAllocation(
                skillId = "mobile-llm-bootstrap",
                reason = "Prepare Termux/Ollama setup and model discovery.",
            ),
            CouncilSkillAllocation(
                skillId = "execution-ledger",
                reason = "Record what Mors allowed, blocked, and ran.",
            ),
        ),
        authority = CouncilAuthorityEnvelope(
            executionMode = CouncilExecutionMode.APPROVAL_REQUIRED,
            mobileSafeOnly = true,
            allowNetwork = true,
            allowFilesystemWrite = false,
            allowShell = true,
            allowSecretAccess = false,
            maxToolCalls = 6,
        ),
    )
}
