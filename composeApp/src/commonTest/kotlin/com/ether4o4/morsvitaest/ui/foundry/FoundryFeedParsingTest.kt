package com.ether4o4.morsvitaest.ui.foundry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FoundryFeedParsingTest {

    @Test
    fun extracts_url_from_markdown_image() {
        val content = "New model dropped ![preview](https://cdn.example.com/a/b.png) — check it out"
        assertEquals("https://cdn.example.com/a/b.png", extractFeedThumbnailUrl(content))
    }

    @Test
    fun extracts_bare_image_link_with_query() {
        val content = "Trending: https://img.example.com/x.jpg?w=64&h=64 right now"
        assertEquals("https://img.example.com/x.jpg?w=64&h=64", extractFeedThumbnailUrl(content))
    }

    @Test
    fun prefers_markdown_image_over_bare_link() {
        val content = "see https://example.com/late.jpg and ![first](https://example.com/early.png)"
        // The markdown image wins regardless of position in the text.
        assertEquals("https://example.com/early.png", extractFeedThumbnailUrl(content))
    }

    @Test
    fun returns_null_when_no_image_present() {
        assertNull(extractFeedThumbnailUrl("Just some text with a https://example.com/page link"))
        assertNull(extractFeedThumbnailUrl(""))
    }

    @Test
    fun matches_image_extensions_case_insensitively() {
        assertEquals("https://example.com/A.PNG", extractFeedThumbnailUrl("hero https://example.com/A.PNG"))
    }

    @Test
    fun strips_markdown_image_markup_from_text() {
        val cleaned = stripFeedMarkdownMedia("Title ![alt](https://example.com/a.png) tail")
        assertEquals("Title  tail", cleaned)
    }

    @Test
    fun unwraps_markdown_links_to_their_label() {
        val cleaned = stripFeedMarkdownMedia("Read [the paper](https://example.com/p) now")
        assertEquals("Read the paper now", cleaned)
    }

    @Test
    fun leaves_plain_text_untouched() {
        val text = "Nothing to strip here."
        assertEquals(text, stripFeedMarkdownMedia(text))
    }
}
