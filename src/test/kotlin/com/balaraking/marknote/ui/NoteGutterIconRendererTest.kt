package com.balaraking.marknote.ui

import com.balaraking.marknote.model.Note
import com.balaraking.marknote.service.NoteService
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.jupiter.api.Assertions.assertEquals

class NoteGutterIconRendererTest : BasePlatformTestCase() {

    fun testTooltipRemovesMarkdownMarkersFromShortContent() {
        val note = Note(id = "tooltip-short", content = "# Heading\n- **bold** and `code`")
        project.getService(NoteService::class.java).saveNote(note)

        val tooltip = NoteGutterIconRenderer(project, note.id).tooltipText

        assertEquals("Heading bold and code", tooltip)
    }

    fun testTooltipCleansAndTruncatesLongContent() {
        val note = Note(id = "tooltip-long", content = "- **${"a".repeat(230)}**")
        project.getService(NoteService::class.java).saveNote(note)

        val tooltip = NoteGutterIconRenderer(project, note.id).tooltipText

        assertEquals(NoteEditorPresentation.formatTooltipPreview(note.content), tooltip)
    }
}
