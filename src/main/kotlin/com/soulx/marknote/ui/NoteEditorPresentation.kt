package com.soulx.marknote.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.soulx.marknote.i18n.NoteBundle
import com.soulx.marknote.message.NoteChangeListener
import com.soulx.marknote.model.NoteStatus
import com.soulx.marknote.service.NoteService
import com.soulx.marknote.util.ProjectPathUtils
import java.awt.Graphics
import java.awt.Rectangle

class NoteEditorPresentation(private val project: Project) : NoteChangeListener, Disposable {

    private val editorHighlighters = mutableMapOf<Editor, MutableList<RangeHighlighter>>()
    private val editorInlays = mutableMapOf<Editor, MutableList<Inlay<*>>>()

    init {
        project.messageBus.connect(this).subscribe(NoteChangeListener.TOPIC, this)
    }

    override fun onNotesChanged() {
        val app = ApplicationManager.getApplication()
        if (app == null || app.isDispatchThread) {
            if (!project.isDisposed) refreshAllEditors()
        } else {
            app.invokeLater {
                if (!project.isDisposed) refreshAllEditors()
            }
        }
    }

    fun refreshAllEditors() {
        val editors = EditorFactory.getInstance().allEditors
        for (editor in editors) {
            if (editor.project == project && !editor.isDisposed) {
                refreshEditor(editor)
            }
        }
    }

    fun refreshEditor(editor: Editor) {
        if (editor.isDisposed) return
        val document = editor.document
        val file = FileDocumentManager.getInstance().getFile(document) ?: return
        val relativePath = ProjectPathUtils.toProjectRelativePath(project, file) ?: return

        val noteService = project.getService(NoteService::class.java)
        val notes = noteService.getNotesForFile(relativePath)

        clearEditor(editor)

        val highlighters = mutableListOf<RangeHighlighter>()
        val inlays = mutableListOf<Inlay<*>>()
        val prefix = NoteBundle.message("prefix.inlay")

        for (note in notes) {
            if (note.status == NoteStatus.UNRESOLVED) continue
            if (note.line < 0 || note.line >= document.lineCount) continue

            val gutterRenderer = NoteGutterIconRenderer(project, note.id)
            val highlighter = editor.markupModel.addRangeHighlighter(
                document.getLineStartOffset(note.line),
                document.getLineEndOffset(note.line),
                0,
                null,
                HighlighterTargetArea.LINES_IN_RANGE
            )
            highlighter.gutterIconRenderer = gutterRenderer
            highlighters.add(highlighter)

            val shortPreview = formatShortPreview(note.content)
            val inlay = editor.inlayModel.addAfterLineEndElement(
                document.getLineEndOffset(note.line),
                true,
                ShortPreviewInlayRenderer("$prefix$shortPreview")
            )
            if (inlay != null) inlays.add(inlay)
        }

        editorHighlighters[editor] = highlighters
        editorInlays[editor] = inlays
    }

    fun clearEditor(editor: Editor) {
        editorHighlighters.remove(editor)?.forEach { if (it.isValid) it.dispose() }
        editorInlays.remove(editor)?.forEach { if (it.isValid) it.dispose() }
    }

    override fun dispose() {
        val editors = editorHighlighters.keys.toList() + editorInlays.keys.toList()
        editors.distinct().forEach { clearEditor(it) }
    }

    companion object {
        fun formatShortPreview(content: String): String {
            val singleLine = content.replace("\r\n", " ").replace("\n", " ").trim()
            return if (singleLine.length > 50) singleLine.substring(0, 47) + "..." else singleLine
        }
    }
}

class ShortPreviewInlayRenderer(private val text: String) : EditorCustomElementRenderer {
    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        val font = inlay.editor.colorsScheme.getFont(EditorFontType.ITALIC)
        val metrics = inlay.editor.contentComponent.getFontMetrics(font)
        return metrics.stringWidth(text)
    }

    override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes) {
        g.color = JBColor.GRAY
        g.font = inlay.editor.colorsScheme.getFont(EditorFontType.ITALIC)
        g.drawString(text, targetRegion.x, targetRegion.y + inlay.editor.ascent)
    }
}
