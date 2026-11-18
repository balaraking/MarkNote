package com.balaraking.marknote.action

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.balaraking.marknote.model.Note
import com.balaraking.marknote.model.NotePlacement
import com.balaraking.marknote.service.DocumentTextProvider
import com.balaraking.marknote.service.NoteAnchorService
import com.balaraking.marknote.service.NoteMarkerService
import com.balaraking.marknote.service.NoteService
import com.balaraking.marknote.util.ProjectPathUtils

internal object NoteCreationSupport {
    fun saveNote(
        project: Project,
        editor: Editor,
        file: VirtualFile,
        content: String,
        placement: NotePlacement
    ): Note? {
        val relativePath = ProjectPathUtils.toProjectRelativePath(project, file) ?: return null
        val line = editor.caretModel.logicalPosition.line
        val anchorService = project.getService(NoteAnchorService::class.java)
        val markerService = project.getService(NoteMarkerService::class.java)
        val note = Note(
            relativePath = relativePath,
            line = line,
            content = content,
            anchor = anchorService.captureAnchor(DocumentTextProvider(editor.document), line),
            placement = placement
        )
        project.getService(NoteService::class.java).saveNote(note)
        markerService.createMarker(note.id, editor.document, line)
        return note
    }
}
