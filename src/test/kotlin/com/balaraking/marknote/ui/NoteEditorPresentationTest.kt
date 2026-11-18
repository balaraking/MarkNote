package com.balaraking.marknote.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NoteEditorPresentationTest {

    @Test
    fun `short preview removes markdown markers across lines`() {
        val content = """
            # Heading
            - **Bold** *italic* `code`
            > [link](https://example.com)
            ```kotlin
            result
            ```
        """.trimIndent()

        assertEquals(
            "Heading Bold italic code link result",
            NoteEditorPresentation.formatShortPreview(content)
        )
    }

    @Test
    fun `short preview truncates after markdown is removed`() {
        val content = "**${"a".repeat(60)}**"

        assertEquals("a".repeat(47) + "...", NoteEditorPresentation.formatShortPreview(content))
    }

    @Test
    fun `tooltip preview cleans long markdown and adds one ellipsis`() {
        val content = "- **${"a".repeat(230)}**"

        val preview = NoteEditorPresentation.formatTooltipPreview(content)

        assertTrue(preview.length <= 200)
        assertTrue(preview.endsWith("..."))
        assertFalse(preview.contains("**"))
        assertFalse(preview.endsWith("......"))
    }

}
