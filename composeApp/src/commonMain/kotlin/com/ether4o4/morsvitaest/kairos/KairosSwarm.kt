package com.ether4o4.morsvitaest .morsvitaestros

import kotlinx.serialization.Serializable

@Serializable
enum class KairosColorRole {
    RED,
    BLUE,
    GREEN,
    YELLOW,
    PURPLE,
}

@Serializable
data class KairosRoleProfile(
    val role: KairosColorRole,
    val name: String,
    val purpose: String,
)

@Serializable
data class KairosSwarmRequest(
    val text: String,
    val requiresTools: Boolean = false,
    val changesCode: Boolean = false,
    val discoversCapabilities: Boolean = false,
    val touchesIdentityOrMemory: Boolean = false,
    val highRisk: Boolean = false,
)

object KairosSwarm {
    val profiles = listOf(
        KairosRoleProfile(
            role = KairosColorRole.RED,
            name = "Spark",
            purpose = "Execute concrete tool, device, shell, and implementation moves.",
        ),
        KairosRoleProfile(
            role = KairosColorRole.BLUE,
            name = "Mythos",
            purpose = "Preserve architecture, continuity, scope, and prior decisions.",
        ),
        KairosRoleProfile(
            role = KairosColorRole.GREEN,
            name = "Builder",
            purpose = "Turn the selected plan into stable mobile-first implementation.",
        ),
        KairosRoleProfile(
            role = KairosColorRole.YELLOW,
            name = "Scout",
            purpose = "Explore candidate frameworks, tools, skills, and alternate paths.",
        ),
        KairosRoleProfile(
            role = KairosColorRole.PURPLE,
            name = "Synthesis",
            purpose = "Merge outputs into one coherent voice, UX, and operator-facing answer.",
        ),
    )

    fun route(request: KairosSwarmRequest): List<KairosColorRole> {
        val roles = linkedSetOf<KairosColorRole>()

        if (request.discoversCapabilities) roles += KairosColorRole.YELLOW
        if (request.touchesIdentityOrMemory || request.highRisk) roles += KairosColorRole.BLUE
        if (request.requiresTools) roles += KairosColorRole.RED
        if (request.changesCode) roles += KairosColorRole.GREEN

        roles += KairosColorRole.PURPLE

        if (request.highRisk && request.changesCode) {
            roles += KairosColorRole.BLUE
        }

        return roles.toList()
    }

    fun routeSummary(request: KairosSwarmRequest): String {
        val byRole = profiles.associateBy { it.role }
        return route(request).joinToString(separator = "\n") { role ->
            val profile = byRole.getValue(role)
            "- ${profile.role}: ${profile.name} - ${profile.purpose}"
        }
    }
}
