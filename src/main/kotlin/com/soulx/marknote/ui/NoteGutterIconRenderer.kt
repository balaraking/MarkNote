package com.soulx.marknote.ui

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.Project
import com.soulx.marknote.model.Note
import com.soulx.marknote.service.NoteService
import javax.swing.Icon
import com.intellij.icons.AllIcons

class NoteGutterIconRenderer(private val project: Project, private val note: Note) : GutterIconRenderer() {
    override fun getIcon(): Icon = AllIcons.General.Note
    
    override fun getTooltipText(): String = note.content
    
    override fun getClickAction(): AnAction? {
        return object : AnAction() {
            override fun actionPerformed(e: AnActionEvent) {
                // Handle note click, show popup to edit or delete
            }
        }
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NoteGutterIconRenderer) return false
        return note.id == other.note.id
    }
    
    override fun hashCode(): Int = note.id.hashCode()
}