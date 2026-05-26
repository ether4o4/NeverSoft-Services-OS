package com.ether4o4.morsvitaest.ui.markdown

import com.ether4o4.morsvitaest.ui.dynamicui.AlertNode
import com.ether4o4.morsvitaest.ui.dynamicui.ColumnNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MorsVitaEstUiBlockIntegrationTest {

    @Test
    fun `morsvitaest-ui fence produces MorsVitaEstUiBlock`() {
        val md = """
            ```morsvitaest-ui
            {"type":"alert","title":"Heads up","message":"Hello"}
            ```
        """.trimIndent()
        val block = parseMarkdown(md).blocks.single()
        assertTrue(block is MorsVitaEstUiBlock)
        val alert = block.node as AlertNode
        assertEquals("Heads up", alert.title)
        assertEquals("Hello", alert.message)
    }

    @Test
    fun `malformed morsvitaest-ui fence produces MorsVitaEstUiError`() {
        val md = """
            ```morsvitaest-ui
            not json at all
            ```
        """.trimIndent()
        val block = parseMarkdown(md).blocks.single()
        assertTrue(block is MorsVitaEstUiError)
    }

    @Test
    fun `ndjson multi-line morsvitaest-ui wraps children in a column`() {
        val md = """
            ```morsvitaest-ui
            {"type":"text","value":"a"}
            {"type":"text","value":"b"}
            ```
        """.trimIndent()
        val block = parseMarkdown(md).blocks.single()
        assertTrue(block is MorsVitaEstUiBlock)
        val col = block.node as ColumnNode
        assertEquals(2, col.children.size)
    }

    @Test
    fun `morsvitaest-ui block surrounded by markdown produces three blocks`() {
        val md = """
            Before

            ```morsvitaest-ui
            {"type":"alert","message":"hi"}
            ```

            After
        """.trimIndent()
        val blocks = parseMarkdown(md).blocks
        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is Paragraph)
        assertTrue(blocks[1] is MorsVitaEstUiBlock)
        assertTrue(blocks[2] is Paragraph)
    }

    @Test
    fun `split-block pattern with json fence is treated as morsvitaest-ui`() {
        val md = """
            morsvitaest-ui
            ```json
            {"type":"alert","message":"hi"}
            ```
        """.trimIndent()
        val block = parseMarkdown(md).blocks.single()
        assertTrue(block is MorsVitaEstUiBlock)
    }

    @Test
    fun `morsvitaest-ui block speakable text walks the node tree`() {
        val md = """
            Intro.

            ```morsvitaest-ui
            {"type":"alert","title":"Heads up","message":"Take care"}
            ```

            Outro.
        """.trimIndent()
        val spoken = parseMarkdown(md).toSpeakableText()
        assertTrue(spoken.contains("Intro"))
        assertTrue(spoken.contains("Heads up"))
        assertTrue(spoken.contains("Take care"))
        assertTrue(spoken.contains("Outro"))
    }
}
