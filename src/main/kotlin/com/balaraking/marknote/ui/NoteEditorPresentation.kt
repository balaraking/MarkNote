package com.balaraking.marknote.ui

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
import com.balaraking.marknote.i18n.NoteBundle
import com.balaraking.marknote.message.NoteChangeListener
import com.balaraking.marknote.model.NotePlacement
import com.balaraking.marknote.model.NoteStatus
import com.balaraking.marknote.service.NoteService
import com.balaraking.marknote.util.ProjectPathUtils
import java.awt.Graphics
import java.awt.Rectangle

class NoteEditorPresentation(private val project: Project) : NoteChangeListener, Disposable {

    private val editorHighlighters = mutableMapOf<Editor, MutableList<RangeHighlighter>>()
    private val editorInlays = mutableMapOf<Editor, MutableList<Inlay<*>>>()
    private val editorBlockInlays = mutableMapOf<Editor, MutableList<Inlay<*>>>()

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
        val blockInlays = mutableListOf<Inlay<*>>()
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

            if (note.placement == NotePlacement.TOP) {
                val blockInlay = editor.inlayModel.addBlockElement(
                    document.getLineStartOffset(note.line),
                    false,
                    true,
                    0,
                    TopNoteInlayRenderer(note.content)
                )
                if (blockInlay != null) blockInlays.add(blockInlay)
            } else {
                val shortPreview = formatShortPreview(note.content)
                val inlay = editor.inlayModel.addAfterLineEndElement(
                    document.getLineEndOffset(note.line),
                    true,
                    ShortPreviewInlayRenderer("$prefix$shortPreview")
                )
                if (inlay != null) inlays.add(inlay)
            }
        }

        editorHighlighters[editor] = highlighters
        editorInlays[editor] = inlays
        editorBlockInlays[editor] = blockInlays
    }

    fun clearEditor(editor: Editor) {
        editorHighlighters.remove(editor)?.forEach { if (it.isValid) it.dispose() }
        editorInlays.remove(editor)?.forEach { if (it.isValid) it.dispose() }
        editorBlockInlays.remove(editor)?.forEach { if (it.isValid) it.dispose() }
    }

    override fun dispose() {
        val editors = editorHighlighters.keys.toList() + editorInlays.keys.toList() + editorBlockInlays.keys.toList()
        editors.distinct().forEach { clearEditor(it) }
    }

    companion object {
        fun formatShortPreview(content: String): String {
            val singleLine = content.replace("\r\n", " ").replace("\n", " ").trim()
            return if (singleLine.length > 50) singleLine.substring(0, 47) + "..." else singleLine
        }
    }
}

class ShortPreviewInlayRenderer(internal val text: String) : EditorCustomElementRenderer {
    internal val markerIcon = NoteIcons.MARK_INLAY

    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        val font = inlay.editor.colorsScheme.getFont(EditorFontType.ITALIC)
        val metrics = inlay.editor.contentComponent.getFontMetrics(font)
        return markerIcon.iconWidth + ICON_GAP + metrics.stringWidth(text)
    }

    override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes) {
        g.color = JBColor.GRAY
        g.font = inlay.editor.colorsScheme.getFont(EditorFontType.ITALIC)
        val iconY = targetRegion.y + (targetRegion.height - markerIcon.iconHeight) / 2
        markerIcon.paintIcon(inlay.editor.contentComponent, g, targetRegion.x, iconY)
        g.drawString(text, targetRegion.x + markerIcon.iconWidth + ICON_GAP, targetRegion.y + inlay.editor.ascent)
    }

    private companion object {
        const val ICON_GAP = 4
    }
}

class TopNoteInlayRenderer(internal val text: String) : EditorCustomElementRenderer {
    private val lines = text.replace("\r\n", "\n").replace("\r", "\n").split('\n')

    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        val font = inlay.editor.colorsScheme.getFont(EditorFontType.ITALIC)
        val metrics = inlay.editor.contentComponent.getFontMetrics(font)
        return lines.maxOfOrNull(metrics::stringWidth) ?: 0
    }

    override fun calcHeightInPixels(inlay: Inlay<*>): Int = inlay.editor.lineHeight * lines.size

    override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes) {
        g.color = JBColor.GRAY
        g.font = inlay.editor.colorsScheme.getFont(EditorFontType.ITALIC)
        val lineHeight = inlay.editor.lineHeight
        val baselineOffset = (lineHeight - g.fontMetrics.height) / 2 + g.fontMetrics.ascent
        lines.forEachIndexed { index, line ->
            g.drawString(line, targetRegion.x, targetRegion.y + baselineOffset + index * lineHeight)
        }
    }
}
