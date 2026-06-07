package com.ether4o4.morsvitaest.ui.compare

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompareOrchestratorTest {

    @Test
    fun merge_caps_at_two_replies_each() = runTest {
        val orchestrator = CompareOrchestrator()
        val messages = mutableListOf<CompareMessage>()
        orchestrator.runRotation(
            seed = "hi",
            labelA = "Alpha",
            labelB = "Beta",
            merge = true,
            ask = { pane, _ -> pane.name },
            onMessage = { messages.add(it) },
        )
        assertEquals(4, messages.size)
        assertEquals(2, messages.count { it.pane == ComparePane.A })
        assertEquals(2, messages.count { it.pane == ComparePane.B })
        assertEquals(
            listOf(ComparePane.A, ComparePane.B, ComparePane.A, ComparePane.B),
            messages.map { it.pane },
        )
    }

    @Test
    fun plain_compare_answers_seed_once_each_independently() = runTest {
        val orchestrator = CompareOrchestrator()
        val messages = mutableListOf<CompareMessage>()
        val prompts = mutableListOf<String>()
        orchestrator.runRotation(
            seed = "what is 2+2",
            labelA = "Alpha",
            labelB = "Beta",
            merge = false,
            ask = { _, prompt ->
                prompts.add(prompt)
                "4"
            },
            onMessage = { messages.add(it) },
        )
        assertEquals(2, messages.size)
        assertEquals(1, messages.count { it.pane == ComparePane.A })
        assertEquals(1, messages.count { it.pane == ComparePane.B })
        // Both panes get the bare seed — no cross-talk in plain compare.
        assertTrue(prompts.all { it == "what is 2+2" })
    }

    @Test
    fun merge_feeds_prior_replies_into_later_prompts() = runTest {
        val orchestrator = CompareOrchestrator()
        val prompts = mutableListOf<String>()
        orchestrator.runRotation(
            seed = "Q",
            labelA = "Alpha",
            labelB = "Beta",
            merge = true,
            ask = { pane, prompt ->
                prompts.add(prompt)
                "${pane.name}-reply"
            },
            onMessage = {},
        )
        // A's first prompt is the bare seed.
        assertEquals("Q", prompts[0])
        // B's first prompt quotes the original question and A's reply.
        assertTrue(prompts[1].contains("Original question"))
        assertTrue(prompts[1].contains("A-reply"))
        // A's second prompt also carries the running transcript.
        assertTrue(prompts[2].contains("B-reply"))
    }

    @Test
    fun reply_budget_is_configurable() = runTest {
        val orchestrator = CompareOrchestrator(repliesPerRotation = 1)
        val messages = mutableListOf<CompareMessage>()
        orchestrator.runRotation(
            seed = "hi",
            labelA = "Alpha",
            labelB = "Beta",
            merge = true,
            ask = { pane, _ -> pane.name },
            onMessage = { messages.add(it) },
        )
        assertEquals(2, messages.size)
        assertEquals(1, messages.count { it.pane == ComparePane.A })
        assertEquals(1, messages.count { it.pane == ComparePane.B })
    }
}
