package com.ether4o4.morsvitaest.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NewsFeedParsingTest {

    @Test
    fun parses_rss_items_with_media_thumbnail() {
        val xml = """
            <rss><channel>
              <title>Example News</title>
              <item>
                <title>First story</title>
                <link>https://example.com/first</link>
                <description>A short summary.</description>
                <media:thumbnail url="https://img.example.com/first.jpg?w=600&amp;h=400"/>
              </item>
            </channel></rss>
        """.trimIndent()

        val articles = parseFeed(xml)
        assertEquals(1, articles.size)
        val a = articles.first()
        assertEquals("First story", a.title)
        assertEquals("Example News", a.source)
        assertEquals("https://example.com/first", a.link)
        // &amp; is decoded so the query string isn't broken.
        assertEquals("https://img.example.com/first.jpg?w=600&h=400", a.imageUrl)
        assertEquals("A short summary.", a.summary)
    }

    @Test
    fun parses_image_enclosure() {
        val xml = """
            <rss><channel><title>Feed</title>
              <item>
                <title>Enclosure story</title>
                <link>https://example.com/e</link>
                <enclosure url="https://cdn.example.com/pic.jpg" type="image/jpeg" length="1234"/>
              </item>
            </channel></rss>
        """.trimIndent()

        val a = parseFeed(xml).single()
        assertEquals("https://cdn.example.com/pic.jpg", a.imageUrl)
    }

    @Test
    fun parses_atom_entry_link_href_and_inline_image() {
        val xml = """
            <feed><title>Atom Feed</title>
              <entry>
                <title>Atom story</title>
                <link href="https://example.com/atom" rel="alternate"/>
                <content type="html"><![CDATA[<p>Body <img src="https://img.example.com/hero.png"/> end</p>]]></content>
              </entry>
            </feed>
        """.trimIndent()

        val a = parseFeed(xml).single()
        assertEquals("Atom story", a.title)
        assertEquals("Atom Feed", a.source)
        assertEquals("https://example.com/atom", a.link)
        assertEquals("https://img.example.com/hero.png", a.imageUrl)
    }

    @Test
    fun strips_html_and_decodes_entities_in_summary() {
        val xml = """
            <rss><channel><title>Feed</title>
              <item>
                <title>HTML &amp; entities</title>
                <link>https://example.com/x</link>
                <description><![CDATA[<p>Tom &amp; Jerry <b>fight</b></p>]]></description>
              </item>
            </channel></rss>
        """.trimIndent()

        val a = parseFeed(xml).single()
        assertEquals("HTML & entities", a.title)
        assertEquals("Tom & Jerry fight", a.summary)
    }

    @Test
    fun item_without_image_yields_null_thumbnail() {
        val xml = """
            <rss><channel><title>Feed</title>
              <item><title>No image</title><link>https://example.com/n</link><description>text</description></item>
            </channel></rss>
        """.trimIndent()

        val a = parseFeed(xml).single()
        assertNull(a.imageUrl)
    }

    @Test
    fun ignores_entries_without_a_title() {
        val xml = """
            <rss><channel><title>Feed</title>
              <item><link>https://example.com/empty</link></item>
              <item><title>Real</title><link>https://example.com/real</link></item>
            </channel></rss>
        """.trimIndent()

        val articles = parseFeed(xml)
        assertEquals(1, articles.size)
        assertTrue(articles.first().title == "Real")
    }
}
