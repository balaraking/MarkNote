package com.soulx.marknote.listener

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.Disposer
import com.soulx.marknote.model.NoteStatus
import com.soulx.marknote.service.DocumentTextProvider
import com.soulx.marknote.service.NoteAnchorService
import com.soulx.marknote.service.NoteMarkerService
import com.soulx.marknote.service.NoteService
import com.soulx.marknote.tracker.NoteDocumentTracker
import com.soulx.marknote.ui.NoteEditorPresentation
import com.soulx.marknote.util.ProjectPathUtils

class EditorOpenListener : EditorFactoryListener {
    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        val project = editor.project ?: return
        if (project.isDisposed) return
        val document = editor.document
        val file = FileDocumentManager.getInstance().getFile(document) ?: return
        val relativePath = ProjectPathUtils.toProjectRelativePath(project, file) ?: return

        val noteService = project.getService(NoteService::class.java)
        val markerService = project.getService(NoteMarkerService::class.java)
        val anchorService = project.getService(NoteAnchorService::class.java)
        val presentation = project.getService(NoteEditorPresentation::class.java)
        val provider = DocumentTextProvider(document)

        ApplicationManager.getApplication().runReadAction {
            noteService.getNotesForFile(relativePath).forEach { note ->
                val matchedLine = anchorService.findMatch(provider, note.anchor)
                if (matchedLine != null) {
                    val newAnchor = anchorService.captureAnchor(provider, matchedLine)
                    noteService.updateStatusAndLine(note.id, NoteStatus.ATTACHED, matchedLine, newAnchor)
                    markerService.createMarker(note.id, document, matchedLine)
                } else {
                    noteService.updateStatusAndLine(note.id, NoteStatus.UNRESOLVED)
                    markerService.removeMarker(note.id)
                }
            }
        }

        val tracker = project.getService(NoteDocumentTracker::class.java)
        if (document.getUserData(NoteDocumentTracker.LISTENER_KEY) == null) {
            val listenerDisposable = Disposer.newDisposable()
            Disposer.register(tracker, listenerDisposable)
            document.putUserData(NoteDocumentTracker.LISTENER_KEY, tracker)
            document.putUserData(NoteDocumentTracker.LISTENER_DISPOSABLE_KEY, listenerDisposable)
            document.addDocumentListener(tracker, listenerDisposable)
        }

        presentation.refreshEditor(editor)
    }

    override fun editorReleased(event: EditorFactoryEvent) {
        val editor = event.editor
        val project = editor.project ?: return
        if (project.isDisposed) return

        val presentation = project.getService(NoteEditorPresentation::class.java)
        presentation.clearEditor(editor)

        val document = editor.document
        val remainingEditors = EditorFactory.getInstance().getEditors(document, project)
        if (remainingEditors.isEmpty()) {
            val tracker = project.getService(NoteDocumentTracker::class.java)
            if (document.getUserData(NoteDocumentTracker.LISTENER_KEY) == tracker) {
                document.getUserData(NoteDocumentTracker.LISTENER_DISPOSABLE_KEY)?.let(Disposer::dispose)
                document.putUserData(NoteDocumentTracker.LISTENER_KEY, null)
                document.putUserData(NoteDocumentTracker.LISTENER_DISPOSABLE_KEY, null)
            }
            tracker.removeDocument(document)
        }
    }
}
