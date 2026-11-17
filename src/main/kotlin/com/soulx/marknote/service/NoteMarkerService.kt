package com.soulx.marknote.service

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap

class NoteMarkerService(private val project: Project) : Disposable {
    private val markers = ConcurrentHashMap<String, RangeMarker>()

    fun createMarker(noteId: String, document: Document, line: Int) {
        if (line < 0 || line >= document.lineCount) return
        removeMarker(noteId)
        val start = document.getLineStartOffset(line)
        val end = document.getLineEndOffset(line)
        val marker = document.createRangeMarker(start, end)
        marker.isGreedyToLeft = false
        marker.isGreedyToRight = false
        markers[noteId] = marker
    }

    fun getMarker(noteId: String): RangeMarker? = markers[noteId]

    fun removeMarker(noteId: String) {
        markers.remove(noteId)?.dispose()
    }

    fun clearAll() {
        val ids = markers.keys.toList()
        for (id in ids) {
            markers.remove(id)?.dispose()
        }
    }

    override fun dispose() {
        clearAll()
    }
}
