package com.example.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SlugsTest {
    @Test
    fun `lowercases and joins the words with hyphens`() {
        assertEquals("hello-world", slugify("Hello, World!"))
    }

    @Test
    fun `keeps letters beyond ASCII`() {
        assertEquals("crème-brûlée", slugify("Crème Brûlée"))
    }

    @Test
    fun `cuts a long slug at a word boundary`() {
        assertEquals("a-quick", slugify("A quick brown fox", maxLength = 9))
    }

    @Test
    fun `cuts a single long word where it must`() {
        assertEquals("abc", slugify("abcdef", maxLength = 3))
    }

    @Test
    fun `leaves a short slug alone`() {
        assertEquals("short", slugify("Short", maxLength = 10))
    }

    @Test
    fun `refuses a length that is not positive`() {
        assertFailsWith<IllegalArgumentException> { slugify("title", maxLength = 0) }
    }
}
