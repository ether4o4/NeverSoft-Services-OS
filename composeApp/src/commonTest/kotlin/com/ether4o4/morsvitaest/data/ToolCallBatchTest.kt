package com.ether4o4.morsvitaest.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ToolCallBatchTest {

    @Test
    fun `planToolCallBatch keeps calls within the parallel budget executable`() {
        val calls = (1..MAX_PARALLEL_TOOL_CALLS).map { index ->
            Triple("call-$index", "tool_$index", "{}")
        }

        val batch = planToolCallBatch(calls)

        assertEquals(calls, batch.executable)
        assertTrue(batch.skipped.isEmpty())
        assertEquals(MAX_PARALLEL_TOOL_CALLS, batch.totalRequested)
    }

    @Test
    fun `planToolCallBatch skips calls beyond the parallel budget`() {
        val calls = (1..(MAX_PARALLEL_TOOL_CALLS + 2)).map { index ->
            Triple("call-$index", "tool_$index", "{}")
        }

        val batch = planToolCallBatch(calls)

        assertEquals(calls.take(MAX_PARALLEL_TOOL_CALLS), batch.executable)
        assertEquals(calls.drop(MAX_PARALLEL_TOOL_CALLS), batch.skipped)
        assertEquals(MAX_PARALLEL_TOOL_CALLS + 2, batch.totalRequested)
    }

    @Test
    fun `skippedToolCallResult preserves call id and tool name`() {
        val result = skippedToolCallResult(Triple("call-9", "expensive_tool", "{}"), totalRequested = 9)

        assertEquals("call-9", result.first)
        assertEquals("expensive_tool", result.second)
        assertTrue(result.third.contains("success\": false"))
        assertTrue(result.third.contains("requested 9 tool calls"))
        assertTrue(result.third.contains("allows up to $MAX_PARALLEL_TOOL_CALLS"))
    }
}
