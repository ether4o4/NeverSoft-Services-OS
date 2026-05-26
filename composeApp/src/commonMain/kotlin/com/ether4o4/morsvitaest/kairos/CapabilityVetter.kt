package com.ether4o4.morsvitaest .morsvitaestros

import kotlinx.serialization.Serializable

@Serializable
enum class CapabilityKind {
    AGENT,
    EVAL,
    FRAMEWORK,
    PROMPT,
    PROVIDER,
    SKILL,
    TOOL,
    WORKFLOW,
    UNKNOWN,
}

@Serializable
enum class CapabilityPermission {
    BACKGROUND_AUTONOMY,
    DEVICE_CONTROL,
    FILESYSTEM_READ,
    FILESYSTEM_WRITE,
    INSTALL_DEPENDENCIES,
    NETWORK,
    OUTBOUND_MESSAGE,
    READ_ONLY,
    SECRET_ACCESS,
    SHELL,
}

@Serializable
data class CapabilityCandidate(
    val id: String,
    val name: String,
    val sourceUrl: String,
    val kind: CapabilityKind = CapabilityKind.UNKNOWN,
    val summary: String = "",
    val license: String? = null,
    val stars: Int = 0,
    val forks: Int = 0,
    val daysSinceUpdate: Int? = null,
    val permissions: Set<CapabilityPermission> = emptySet(),
    val signals: List<String> = emptyList(),
    val referenceSystems: List<String> = emptyList(),
)

@Serializable
enum class FindingSeverity {
    INFO,
    WARNING,
    HIGH,
    CRITICAL,
}

@Serializable
data class VettingFinding(
    val severity: FindingSeverity,
    val code: String,
    val message: String,
)

@Serializable
enum class VettingDecision {
    PROMOTE_CANDIDATE,
    STAGE,
    QUARANTINE,
    REJECT,
}

@Serializable
data class CapabilityVettingReport(
    val candidateId: String,
    val decision: VettingDecision,
    val score: Int,
    val findings: List<VettingFinding>,
) {
    val requiresHumanPromotion: Boolean
        get() = decision != VettingDecision.REJECT
}

object CapabilityVetter {
    private val destructivePatterns = listOf(
        Regex("""rm\s+-rf\s+[/~*$]""", RegexOption.IGNORE_CASE),
        Regex("""del\s+/[fsq]""", RegexOption.IGNORE_CASE),
        Regex("""format\s+[a-z]:""", RegexOption.IGNORE_CASE),
        Regex("""invoke-expression|iex\s""", RegexOption.IGNORE_CASE),
        Regex("""curl\s+[^|]+[|]\s*(sh|bash|pwsh|powershell)""", RegexOption.IGNORE_CASE),
        Regex("""wget\s+[^|]+[|]\s*(sh|bash|pwsh|powershell)""", RegexOption.IGNORE_CASE),
        Regex("""base64\s+(-d|--decode).*[|]\s*(sh|bash|pwsh|powershell)""", RegexOption.IGNORE_CASE),
    )

    private val privilegedPermissions = setOf(
        CapabilityPermission.BACKGROUND_AUTONOMY,
        CapabilityPermission.DEVICE_CONTROL,
        CapabilityPermission.FILESYSTEM_WRITE,
        CapabilityPermission.INSTALL_DEPENDENCIES,
        CapabilityPermission.OUTBOUND_MESSAGE,
        CapabilityPermission.SECRET_ACCESS,
        CapabilityPermission.SHELL,
    )

    fun evaluate(candidate: CapabilityCandidate): CapabilityVettingReport {
        val findings = mutableListOf<VettingFinding>()
        val signalText = candidate.signals.joinToString("\n")

        if (candidate.name.isBlank() || candidate.id.isBlank()) {
            findings += VettingFinding(
                FindingSeverity.CRITICAL,
                "missing_identity",
                "Candidate must have a stable id and name before it can enter the registry.",
            )
        }

        if (!candidate.sourceUrl.startsWith("https://") && !candidate.sourceUrl.startsWith("file:")) {
            findings += VettingFinding(
                FindingSeverity.HIGH,
                "untrusted_source",
                "Remote candidates must come from HTTPS or a local file quarantine path.",
            )
        }

        if (candidate.license.isNullOrBlank()) {
            findings += VettingFinding(
                FindingSeverity.WARNING,
                "missing_license",
                "No license was detected; keep this as reference material until usage rights are clear.",
            )
        }

        if (candidate.daysSinceUpdate != null && candidate.daysSinceUpdate > 730) {
            findings += VettingFinding(
                FindingSeverity.WARNING,
                "stale_repo",
                "The source has not been updated in more than two years.",
            )
        }

        if (destructivePatterns.any { it.containsMatchIn(signalText) }) {
            findings += VettingFinding(
                FindingSeverity.CRITICAL,
                "destructive_script",
                "Signals include destructive or pipe-to-shell install behavior.",
            )
        }

        val privileged = candidate.permissions.intersect(privilegedPermissions)
        if (privileged.isNotEmpty()) {
            findings += VettingFinding(
                FindingSeverity.HIGH,
                "privileged_capability",
                "Candidate requests privileged capability: ${privileged.joinToString()}.",
            )
        }

        val score = calculateScore(candidate, findings)
        val decision = when {
            findings.any { it.severity == FindingSeverity.CRITICAL } -> VettingDecision.REJECT
            findings.any { it.severity == FindingSeverity.HIGH } -> VettingDecision.QUARANTINE
            findings.any { it.severity == FindingSeverity.WARNING } -> VettingDecision.STAGE
            score >= 70 -> VettingDecision.PROMOTE_CANDIDATE
            else -> VettingDecision.STAGE
        }

        return CapabilityVettingReport(
            candidateId = candidate.id,
            decision = decision,
            score = score,
            findings = findings,
        )
    }

    private fun calculateScore(candidate: CapabilityCandidate, findings: List<VettingFinding>): Int {
        var score = 40
        if (!candidate.license.isNullOrBlank()) score += 10
        if (candidate.kind != CapabilityKind.UNKNOWN) score += 8
        if (candidate.permissions.all { it == CapabilityPermission.READ_ONLY || it == CapabilityPermission.NETWORK }) score += 10
        if (candidate.referenceSystems.isNotEmpty()) score += (candidate.referenceSystems.size * 4).coerceAtMost(16)
        if (candidate.stars >= 100) score += 8
        if (candidate.stars >= 1_000) score += 8
        if ((candidate.daysSinceUpdate ?: Int.MAX_VALUE) <= 90) score += 10

        for (finding in findings) {
            score -= when (finding.severity) {
                FindingSeverity.INFO -> 0
                FindingSeverity.WARNING -> 8
                FindingSeverity.HIGH -> 30
                FindingSeverity.CRITICAL -> 100
            }
        }

        return score.coerceIn(0, 100)
    }
}
