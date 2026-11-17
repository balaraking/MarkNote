package com.balaraking.marknote.integration

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.openapi.vfs.VirtualFile
import com.balaraking.marknote.model.Note
import com.balaraking.marknote.model.NoteStatus
import com.balaraking.marknote.i18n.NoteBundle
import com.balaraking.marknote.service.DocumentTextProvider
import com.balaraking.marknote.service.NoteAnchorService
import com.balaraking.marknote.service.NoteMarkerService
import com.balaraking.marknote.service.NoteService
import com.balaraking.marknote.tracker.NoteDocumentTracker
import com.balaraking.marknote.ui.NoteEditorPresentation
import com.balaraking.marknote.ui.NoteGutterIconRenderer
import com.balaraking.marknote.ui.NoteIcons
import com.balaraking.marknote.ui.ShortPreviewInlayRenderer
import com.balaraking.marknote.util.ProjectPathUtils
import java.util.Locale
import java.util.ResourceBundle

class NoteEditorIntegrationTest : BasePlatformTestCase() {

    private fun configureProjectFile(name: String, text: String): VirtualFile {
        val psiFile = myFixture.addFileToProject(name, text)
        myFixture.openFileInEditor(psiFile.virtualFile)
        return psiFile.virtualFile
    }

    private fun noteHighlighters(): List<RangeHighlighter> =
        myFixture.editor.markupModel.allHighlighters.filter { it.gutterIconRenderer is NoteGutterIconRenderer }

    private fun noteInlays(): List<Inlay<*>> =
        myFixture.editor.inlayModel.getAfterLineEndElementsInRange(0, myFixture.editor.document.textLength)
            .filter { it.renderer is ShortPreviewInlayRenderer }

    fun testInlayPrefixHasNoLeadingWhitespace() {
        val prefixes = listOf(
            NoteBundle.message("prefix.inlay"),
            ResourceBundle.getBundle("messages.NoteBundle", Locale.ENGLISH).getString("prefix.inlay"),
            ResourceBundle.getBundle("messages.NoteBundle", Locale.SIMPLIFIED_CHINESE).getString("prefix.inlay")
        )

        prefixes.forEach { prefix ->
            assertFalse(prefix.startsWith(" "))
            assertFalse(prefix.contains("//"))
            assertTrue(prefix.endsWith(" "))
        }
    }

    fun testCustomMarkNoteIconIsUsedForGutterMarker() {
        val vf = configureProjectFile("IconSample.kt", "fun main() {}\n")
        val relPath = ProjectPathUtils.toProjectRelativePath(project, vf)!!
        val document = myFixture.editor.document
        val noteService = project.getService(NoteService::class.java)
        val anchorService = project.getService(NoteAnchorService::class.java)
        val markerService = project.getService(NoteMarkerService::class.java)
        val presentation = project.getService(NoteEditorPresentation::class.java)

        val anchor = anchorService.captureAnchor(DocumentTextProvider(document), 0)
        val note = Note(relativePath = relPath, line = 0, content = "Icon note", anchor = anchor)
        noteService.saveNote(note)
        markerService.createMarker(note.id, document, 0)
        presentation.refreshEditor(myFixture.editor)

        val renderer = noteHighlighters().single().gutterIconRenderer
        assertNotNull(renderer)
        assertNotNull(renderer!!.icon)
        assertFalse(renderer.icon === AllIcons.General.Note)
        assertTrue(renderer.icon.iconWidth <= 16)
        assertTrue(renderer.icon.iconHeight <= 16)
        assertTrue(renderer.icon.iconWidth < NoteIcons.MARK_NOTE.iconWidth)

        val preview = noteInlays().single().renderer as ShortPreviewInlayRenderer
        assertFalse(preview.text.startsWith(" "))
        assertFalse(preview.text.contains("//"))
        assertSame(NoteIcons.MARK_INLAY, preview.markerIcon)
    }

    fun testLineInsertionAboveAndDeletionAboveFollowMarker() {
        val vf = configureProjectFile(
            "Sample.kt",
            "package demo\n\nfun run() {\n    val target = 42\n    println(target)\n}\n"
        )

        val relPath = ProjectPathUtils.toProjectRelativePath(project, vf)!!
        val document = myFixture.editor.document
        val noteService = project.getService(NoteService::class.java)
        val anchorService = project.getService(NoteAnchorService::class.java)
        val markerService = project.getService(NoteMarkerService::class.java)
        val tracker = project.getService(NoteDocumentTracker::class.java)
        val presentation = project.getService(NoteEditorPresentation::class.java)

        val anchor = anchorService.captureAnchor(DocumentTextProvider(document), 3)
        val note = Note(
            relativePath = relPath,
            line = 3,
            content = "Important target variable",
            anchor = anchor
        )
        noteService.saveNote(note)
        markerService.createMarker(note.id, document, 3)
        presentation.refreshEditor(myFixture.editor)

        assertEquals(1, noteHighlighters().size)
        assertEquals(1, noteInlays().size)

        // Insert 2 lines above line 3
        WriteCommandAction.runWriteCommandAction(project) {
            document.insertString(0, "// comment 1\n// comment 2\n")
        }
        tracker.processDocumentChangeNow(document, relPath)
        presentation.refreshEditor(myFixture.editor)

        val afterInsert = noteService.getNotes().first { it.id == note.id }
        assertEquals(NoteStatus.ATTACHED, afterInsert.status)
        assertEquals(5, afterInsert.line)

        // Delete 1 line above
        WriteCommandAction.runWriteCommandAction(project) {
            val line0End = document.getLineEndOffset(0) + 1
            document.deleteString(0, line0End)
        }
        tracker.processDocumentChangeNow(document, relPath)
        presentation.refreshEditor(myFixture.editor)

        val afterDelete = noteService.getNotes().first { it.id == note.id }
        assertEquals(NoteStatus.ATTACHED, afterDelete.status)
        assertEquals(4, afterDelete.line)
    }

    fun testTrailingEmptyLineAtEofTracksProperly() {
        val vf = configureProjectFile("EofSample.kt", "fun main() {}\n")
        val relPath = ProjectPathUtils.toProjectRelativePath(project, vf)!!
        val document = myFixture.editor.document
        val lastLine = document.lineCount - 1
        assertEquals(document.textLength, document.getLineStartOffset(lastLine))

        val noteService = project.getService(NoteService::class.java)
        val anchorService = project.getService(NoteAnchorService::class.java)
        val markerService = project.getService(NoteMarkerService::class.java)
        val tracker = project.getService(NoteDocumentTracker::class.java)

        val anchor = anchorService.captureAnchor(DocumentTextProvider(document), lastLine)
        val note = Note(relativePath = relPath, line = lastLine, content = "EOF trailing line note", anchor = anchor)
        noteService.saveNote(note)
        markerService.createMarker(note.id, document, lastLine)

        WriteCommandAction.runWriteCommandAction(project) {
            document.insertString(0, "// top line\n")
        }
        tracker.processDocumentChangeNow(document, relPath)

        val updated = noteService.getNotes().first { it.id == note.id }
        assertEquals(NoteStatus.ATTACHED, updated.status)
        assertEquals(lastLine + 1, updated.line)
    }

    fun testTargetLineReplacementTransitionsToUnresolvedAndClearsPresentation() {
        val vf = configureProjectFile(
            "Rewrite.kt",
            "fun alpha() {\n    val originalLine = 1\n}\n"
        )
        val relPath = ProjectPathUtils.toProjectRelativePath(project, vf)!!
        val document = myFixture.editor.document
        val noteService = project.getService(NoteService::class.java)
        val anchorService = project.getService(NoteAnchorService::class.java)
        val markerService = project.getService(NoteMarkerService::class.java)
        val tracker = project.getService(NoteDocumentTracker::class.java)
        val presentation = project.getService(NoteEditorPresentation::class.java)

        val anchor = anchorService.captureAnchor(DocumentTextProvider(document), 1)
        val note = Note(relativePath = relPath, line = 1, content = "Note on original line", anchor = anchor)
        noteService.saveNote(note)
        markerService.createMarker(note.id, document, 1)
        presentation.refreshEditor(myFixture.editor)

        assertEquals(1, noteHighlighters().size)
        assertEquals(1, noteInlays().size)

        // Completely rewrite line 1
        WriteCommandAction.runWriteCommandAction(project) {
            val start = document.getLineStartOffset(1)
            val end = document.getLineEndOffset(1)
            document.replaceString(start, end, "    val completelyDifferentCode = 999")
        }
        tracker.processDocumentChangeNow(document, relPath)
        presentation.refreshEditor(myFixture.editor)

        val unresolved = noteService.getNotes().first { it.id == note.id }
        assertEquals(NoteStatus.UNRESOLVED, unresolved.status)
        assertEquals(0, noteHighlighters().size)
        assertEquals(0, noteInlays().size)

        // Rebind to line 1
        val newAnchor = anchorService.captureAnchor(DocumentTextProvider(document), 1)
        noteService.rebindNote(note.id, relPath, 1, newAnchor)
        markerService.createMarker(note.id, document, 1)
        presentation.refreshEditor(myFixture.editor)

        val rebound = noteService.getNotes().first { it.id == note.id }
        assertEquals(NoteStatus.ATTACHED, rebound.status)
        assertEquals(1, noteHighlighters().size)
        assertEquals(1, noteInlays().size)

        // Delete note and verify cleanup
        noteService.deleteNote(note.id)
        presentation.refreshEditor(myFixture.editor)
        assertEquals(0, noteHighlighters().size)
        assertEquals(0, noteInlays().size)
        assertNull(markerService.getMarker(note.id))
    }
}
