package com.soulx.marknote.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.soulx.marknote.service.NoteService
import java.awt.BorderLayout
import javax.swing.DefaultListModel
import javax.swing.JPanel

class NotesToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val service = project.getService(NoteService::class.java)
        val panel = JPanel(BorderLayout())
        
        val listModel = DefaultListModel<String>()
        service.getNotes().forEach { 
            listModel.addElement("${it.relativePath}:${it.line} - ${it.content}") 
        }
        
        val list = JBList(listModel)
        panel.add(JBScrollPane(list), BorderLayout.CENTER)
        
        val contentManager = toolWindow.contentManager
        val content = contentManager.factory.createContent(panel, "", false)
        contentManager.addContent(content)
    }
}