package com.soulx.marknote.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.soulx.marknote.i18n.NoteBundle
import com.soulx.marknote.model.NoteStatus
import com.soulx.marknote.service.DocumentTextProvider
import com.soulx.marknote.service.NoteAnchorService
import com.soulx.marknote.service.NoteMarkerService
import com.soulx.marknote.service.NoteService
import com.soulx.marknote.ui.NoteEditorPresentation
import com.soulx.marknote.util.ProjectPathUtils

class RebindNoteAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        val relativePath = ProjectPathUtils.toProjectRelativePath(project, file) ?: return
        val service = project.getService(NoteService::class.java)

        val unresolvedNotes = service.getNotesForFile(relativePath).filter { it.status == NoteStatus.UNRESOLVED }
        if (unresolvedNotes.isEmpty()) {
            Messages.showInfoMessage(project, NoteBundle.message("msg.no.unresolved"), NoteBundle.message("title.info"))
            return
        }

        val noteToRebind = if (unresolvedNotes.size == 1) {
            unresolvedNotes.first()
        } else {
            val titles = unresolvedNotes.map {
                NoteBundle.message("popup.rebind.item.format", it.line + 1, NoteEditorPresentation.formatShortPreview(it.content))
            }.toTypedArray()
            val selection = Messages.showDialog(
                project,
                NoteBundle.message("popup.rebind.select.prompt"),
                NoteBundle.message("popup.title.rebind"),
                titles,
                0,
                Messages.getQuestionIcon()
            )
            if (selection in unresolvedNotes.indices) unresolvedNotes[selection] else return
        }

        val line = editor.caretModel.logicalPosition.line
        val anchorService = project.getService(NoteAnchorService::class.java)
        val markerService = project.getService(NoteMarkerService::class.java)
        val provider = DocumentTextProvider(editor.document)

        val newAnchor = anchorService.captureAnchor(provider, line)
        service.rebindNote(noteToRebind.id, relativePath, line, newAnchor)
        markerService.createMarker(noteToRebind.id, editor.document, line)

        Messages.showInfoMessage(project, NoteBundle.message("msg.rebind.success"), NoteBundle.message("title.info"))
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val relativePath = if (project != null && file != null) ProjectPathUtils.toProjectRelativePath(project, file) else null
        if (project == null || relativePath == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }
        val service = project.getService(NoteService::class.java)
        val hasUnresolved = service.getNotesForFile(relativePath).any { it.status == NoteStatus.UNRESOLVED }
        e.presentation.isEnabledAndVisible = hasUnresolved
    }
}
