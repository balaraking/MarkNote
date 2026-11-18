package com.balaraking.marknote.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.balaraking.marknote.i18n.NoteBundle
import com.balaraking.marknote.model.NotePlacement
import com.balaraking.marknote.ui.NoteDialog
import com.balaraking.marknote.util.ProjectPathUtils

open class BaseAddNoteAction(private val placement: NotePlacement) : AnAction() {
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

        val dialog = NoteDialog(project, null)
        if (dialog.showAndGet()) {
            NoteCreationSupport.saveNote(project, editor, file, dialog.contentResult, placement)
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

class AddNoteAction : BaseAddNoteAction(NotePlacement.INLINE)
