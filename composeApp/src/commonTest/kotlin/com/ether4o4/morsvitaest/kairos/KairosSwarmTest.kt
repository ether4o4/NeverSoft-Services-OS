package com.ether4o4.morsvitaest .morsvitaestros

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KairosSwarmTest {

    @Test
    fun `tool based implementation routes through spark builder and synthesis`() {
        val route = KairosSwarm.route(
            KairosSwarmRequest(
                text = "Wire Termux Ollama into the mobile app",
                requiresTools = true,
                changesCode = true,
            ),
        )

        assertEquals(
            listOf(KairosColorRole.RED, KairosColorRole.GREEN, KairosColorRole.PURPLE),
            route,
        )
    }

    @Test
    fun `capability discovery routes through scout and synthesis`() {
        val route = KairosSwarm.route(
            KairosSwarmRequest(
                text = "Scan trending AI frameworks",
                discoversCapabilities = true,
            ),
        )

        assertEquals(listOf(KairosColorRole.YELLOW, KairosColorRole.PURPLE), route)
    }

    @Test
    fun `high risk code change includes mythos review`() {
        val route = KairosSwarm.route(
            KairosSwarmRequest(
                text = "Promote a downloaded tool into trusted runtime",
                requiresTools = true,
                changesCode = true,
                highRisk = true,
            ),
        )

        assertTrue(KairosColorRole.BLUE in route)
        assertTrue(KairosColorRole.RED in route)
        assertTrue(KairosColorRole.GREEN in route)
        assertTrue(KairosColorRole.PURPLE in route)
    }
}
