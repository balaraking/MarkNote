package com.soulx.marknote.tracker

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.util.Alarm
import com.soulx.marknote.model.NoteStatus
import com.soulx.marknote.service.DocumentTextProvider
import com.soulx.marknote.service.NoteAnchorService
import com.soulx.marknote.service.NoteMarkerService
import com.soulx.marknote.service.NoteService
import com.soulx.marknote.util.ProjectPathUtils
import java.util.concurrent.ConcurrentHashMap

class NoteDocumentTracker(private val project: Project) : DocumentListener, Disposable {
    companion object {
        val LISTENER_KEY: Key<NoteDocumentTracker> = Key.create("MarkNote.NoteDocumentTracker")
        val LISTENER_DISPOSABLE_KEY: Key<Disposable> = Key.create("MarkNote.NoteDocumentTracker.Disposable")
    }

    private val documentAlarms = ConcurrentHashMap<Document, Alarm>()

    override fun documentChanged(event: DocumentEvent) {
        if (project.isDisposed) return
        val document = event.document
        val file = FileDocumentManager.getInstance().getFile(document) ?: return
        val relativePath = ProjectPathUtils.toProjectRelativePath(project, file) ?: return

        val alarm = documentAlarms.getOrPut(document) { Alarm(Alarm.ThreadToUse.POOLED_THREAD, this) }
        alarm.cancelAllRequests()
        alarm.addRequest({
            ApplicationManager.getApplication().runReadAction {
                if (!project.isDisposed) {
                    processDocumentChange(document, relativePath)
                }
            }
        }, 300)
    }

    fun processDocumentChangeNow(document: Document, relativePath: String) {
        if (project.isDisposed) return
        processDocumentChange(document, relativePath)
    }

    private fun processDocumentChange(document: Document, relativePath: String) {
        val noteService = project.getService(NoteService::class.java)
        val anchorService = project.getService(NoteAnchorService::class.java)
        val markerService = project.getService(NoteMarkerService::class.java)
        val provider = DocumentTextProvider(document)

        val notes = noteService.getNotesForFile(relativePath)

        for (note in notes) {
            val marker = markerService.getMarker(note.id)
            if (marker != null && marker.isValid && marker.startOffset <= document.textLength) {
                val newLine = document.getLineNumber(marker.startOffset)
                val currentLineText = anchorService.normalize(provider.getLineText(newLine))
                // If the line itself was completely replaced with different text while marker survived,
                // verify anchor match instead of blindly sticking to rewritten code.
                if (note.anchor.lineText.isNotEmpty() && currentLineText != note.anchor.lineText) {
                    val matchedLine = anchorService.findMatch(provider, note.anchor)
                    if (matchedLine != null) {
                        val newAnchor = anchorService.captureAnchor(provider, matchedLine)
                        noteService.updateStatusAndLine(note.id, NoteStatus.ATTACHED, matchedLine, newAnchor)
                        markerService.createMarker(note.id, document, matchedLine)
                    } else {
                        noteService.updateStatusAndLine(note.id, NoteStatus.UNRESOLVED)
                        markerService.removeMarker(note.id)
                    }
                } else {
                    val newAnchor = anchorService.captureAnchor(provider, newLine)
                    noteService.updateStatusAndLine(note.id, NoteStatus.ATTACHED, newLine, newAnchor)
                }
            } else {
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
    }

    fun removeDocument(document: Document) {
        documentAlarms.remove(document)?.let { alarm ->
            alarm.cancelAllRequests()
            Disposer.dispose(alarm)
        }
    }

    override fun dispose() {
        val docs = documentAlarms.keys.toList()
        for (doc in docs) {
            removeDocument(doc)
        }
    }
}
