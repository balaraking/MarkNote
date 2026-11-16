package com.soulx.marknote.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.soulx.marknote.i18n.NoteBundle
import com.soulx.marknote.model.Note
import com.soulx.marknote.model.LineAnchor
import com.soulx.marknote.service.NoteService

class AddNoteAction : AnAction(NoteBundle.message("action.add.note.text")) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        
        val line = editor.caretModel.logicalPosition.line
        val content = Messages.showInputDialog(
            project,
            "Enter note content:",
            NoteBundle.message("popup.title"),
            null
        ) ?: return
        
        if (content.isNotBlank()) {
            val service = project.getService(NoteService::class.java)
            val basePath = project.basePath ?: ""
            val relativePath = file.path.removePrefix(basePath).removePrefix("/")
            
            val note = Note(
                relativePath = relativePath,
                line = line,
                content = content,
                anchor = LineAnchor(lineText = "Sample Line Text")
            )
            service.addNote(note)
        }
    }
}