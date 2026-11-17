package com.balaraking.marknote.ui

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.Project
import com.balaraking.marknote.service.NoteService
import javax.swing.Icon

class NoteGutterIconRenderer(
    private val project: Project,
    private val noteId: String
) : GutterIconRenderer() {

    override fun getIcon(): Icon = NoteIcons.MARK_GUTTER

    override fun getTooltipText(): String? {
        val note = project.getService(NoteService::class.java)?.getNotes()?.find { it.id == noteId }
        return note?.content
    }

    override fun getClickAction(): AnAction {
        return object : AnAction() {
            override fun actionPerformed(e: AnActionEvent) {
                val note = project.getService(NoteService::class.java)?.getNotes()?.find { it.id == noteId } ?: return
                NoteDialog(project, note).show()
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NoteGutterIconRenderer) return false
        return noteId == other.noteId
    }

    override fun hashCode(): Int = noteId.hashCode()
}
