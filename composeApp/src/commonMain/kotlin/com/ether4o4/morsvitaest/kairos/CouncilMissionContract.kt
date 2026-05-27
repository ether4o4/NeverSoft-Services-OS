package com.ether4o4.morsvitaest.kairos

import kotlinx.serialization.Serializable

@Serializable
enum class CouncilMissionOrigin {
    COLOUR_CEAUXDID,
    MORSVITAEST,
    HUMAN_OPERATOR,
    EXTERNAL_AGENT,
}

@Serializable
enum class CouncilMissionRisk {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}

@Serializable
enum class CouncilExecutionMode {
    PLAN_ONLY,
    APPROVAL_REQUIRED,
    MOBILE_SAFE_AUTONOMY,
    FULL_AUTONOMY,
}

@Serializable
data class CouncilAuthorityEnvelope(
    val executionMode: CouncilExecutionMode = CouncilExecutionMode.APPROVAL_REQUIRED,
    val mobileSafeOnly: Boolean = true,
    val allowNetwork: Boolean = true,
    val allowFilesystemWrite: Boolean = false,
    val allowShell: Boolean = false,
    val allowSecretAccess: Boolean = false,
    val maxToolCalls: Int = 6,
)

@Serializable
data class CouncilAgentAllocation(
    val agentId: String,
    val role: String,
    val preferredRuntime: AgentRuntimeKind? = null,
    val reason: String = "",
)

@Serializable
data class CouncilToolAllocation(
    val toolId: String,
    val reason: String = "",
    val required: Boolean = true,
)

@Serializable
data class CouncilSkillAllocation(
    val skillId: String,
    val reason: String = "",
    val required: Boolean = false,
)

@Serializable
data class CouncilMissionPacket(
    val missionId: String,
    val title: String,
    val intent: String,
    val origin: CouncilMissionOrigin = CouncilMissionOrigin.COLOUR_CEAUXDID,
    val risk: CouncilMissionRisk = CouncilMissionRisk.MEDIUM,
    val agents: List<CouncilAgentAllocation> = emptyList(),
    val tools: List<CouncilToolAllocation> = emptyList(),
    val skills: List<CouncilSkillAllocation> = emptyList(),
    val authority: CouncilAuthorityEnvelope = CouncilAuthorityEnvelope(),
)

@Serializable
enum class CouncilIntakeStatus {
    ACCEPTED,
    NEEDS_APPROVAL,
    PARTIAL,
    REJECTED,
}

@Serializable
data class CouncilIntakeFinding(
    val code: String,
    val message: String,
    val severity: FindingSeverity = FindingSeverity.INFO,
)

@Serializable
data class CouncilIntakeToolDecision(
    val toolId: String,
    val accepted: Boolean,
    val requiresApproval: Boolean,
    val reason: String,
)

@Serializable
data class CouncilIntakeAgentDecision(
    val agentId: String,
    val accepted: Boolean,
    val reason: String,
)

@Serializable
data class CouncilIntakeReport(
    val missionId: String,
    val status: CouncilIntakeStatus,
    val acceptedAgents: List<CouncilIntakeAgentDecision>,
    val toolDecisions: List<CouncilIntakeToolDecision>,
    val findings: List<CouncilIntakeFinding>,
) {
    val acceptedToolCount: Int
        get() = toolDecisions.count { it.accepted }

    val blockedToolCount: Int
        get() = toolDecisions.count { !it.accepted }

    val requiresApproval: Boolean
        get() = status == CouncilIntakeStatus.NEEDS_APPROVAL || toolDecisions.any { it.requiresApproval }
}
