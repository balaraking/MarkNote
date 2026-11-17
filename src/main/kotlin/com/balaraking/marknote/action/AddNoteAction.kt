package com.balaraking.marknote.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.balaraking.marknote.i18n.NoteBundle
import com.balaraking.marknote.model.Note
import com.balaraking.marknote.service.DocumentTextProvider
import com.balaraking.marknote.service.NoteAnchorService
import com.balaraking.marknote.service.NoteMarkerService
import com.balaraking.marknote.service.NoteService
import com.balaraking.marknote.ui.NoteDialog
import com.balaraking.marknote.util.ProjectPathUtils

class AddNoteAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return

        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        val relativePath = ProjectPathUtils.toProjectRelativePath(project, file)
        if (relativePath == null) {
            Messages.showErrorDialog(
                project,
                NoteBundle.message("msg.file.not.in.project"),
                NoteBundle.message("title.error")
            )
            return
        }

        val line = editor.caretModel.logicalPosition.line
        val dialog = NoteDialog(project, null)
        if (dialog.showAndGet()) {
            val content = dialog.contentResult
            val service = project.getService(NoteService::class.java)
            val anchorService = project.getService(NoteAnchorService::class.java)
            val markerService = project.getService(NoteMarkerService::class.java)

            val provider = DocumentTextProvider(editor.document)
            val anchor = anchorService.captureAnchor(provider, line)

            val note = Note(
                relativePath = relativePath,
                line = line,
                content = content,
                anchor = anchor
            )
            service.saveNote(note)
            markerService.createMarker(note.id, editor.document, line)
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible =
            project != null && editor != null && file != null && ProjectPathUtils.toProjectRelativePath(project, file) != null
    }
}
