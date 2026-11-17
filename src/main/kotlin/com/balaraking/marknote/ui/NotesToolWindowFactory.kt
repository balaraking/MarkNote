package com.balaraking.marknote.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorOpenRequest
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.PopupHandler
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.balaraking.marknote.i18n.NoteBundle
import com.balaraking.marknote.message.NoteChangeListener
import com.balaraking.marknote.model.Note
import com.balaraking.marknote.model.NoteStatus
import com.balaraking.marknote.service.NoteService
import com.balaraking.marknote.util.ProjectPathUtils
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.DefaultListModel
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer
import javax.swing.ListSelectionModel

class NotesToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = NotesPanel(project)
        val contentManager = toolWindow.contentManager
        val content = contentManager.factory.createContent(panel, "", false)
        Disposer.register(content, panel)
        contentManager.addContent(content)
    }
}

class NotesPanel(private val project: Project) : JPanel(BorderLayout()), Disposable {
    private val service = project.getService(NoteService::class.java)
    private val listModel = DefaultListModel<Note>()
    private val list = JBList(listModel)

    init {
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION
        list.cellRenderer = NoteListCellRenderer()

        val toolbarGroup = DefaultActionGroup().apply {
            add(object : AnAction(NoteBundle.message("action.navigate.file"), null, AllIcons.Actions.Forward) {
                override fun actionPerformed(e: AnActionEvent) {
                    list.selectedValue?.let { navigateToNote(it) }
                }

                override fun update(e: AnActionEvent) {
                    e.presentation.isEnabled = list.selectedValue?.status == NoteStatus.ATTACHED
                }
            })
            add(object : AnAction(NoteBundle.message("popup.title.edit"), null, AllIcons.Actions.Edit) {
                override fun actionPerformed(e: AnActionEvent) {
                    list.selectedValue?.let { NoteDialog(project, it).show() }
                }

                override fun update(e: AnActionEvent) {
                    e.presentation.isEnabled = list.selectedValue != null
                }
            })
            add(object : AnAction(NoteBundle.message("popup.title.rebind"), null, AllIcons.Actions.Refresh) {
                override fun actionPerformed(e: AnActionEvent) {
                    list.selectedValue?.let { RebindNoteDialog(project, it).show() }
                }

                override fun update(e: AnActionEvent) {
                    e.presentation.isEnabled = list.selectedValue != null
                }
            })
            add(object : AnAction(NoteBundle.message("popup.delete"), null, AllIcons.Actions.GC) {
                override fun actionPerformed(e: AnActionEvent) {
                    list.selectedValue?.let { service.deleteNote(it.id) }
                }

                override fun update(e: AnActionEvent) {
                    e.presentation.isEnabled = list.selectedValue != null
                }
            })
        }

        val toolbar = ActionManager.getInstance().createActionToolbar("CodeNotesToolbar", toolbarGroup, true)
        toolbar.targetComponent = this
        add(toolbar.component, BorderLayout.NORTH)

        refreshList()

        // Double click: navigate if attached; open Rebind dialog if unresolved
        list.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val note = list.selectedValue ?: return
                    if (note.status == NoteStatus.ATTACHED) {
                        navigateToNote(note)
                    } else {
                        RebindNoteDialog(project, note).show()
                    }
                }
            }
        })

        // Right-click context menu: Navigate, Edit, Rebind, Delete
        list.addMouseListener(object : PopupHandler() {
            override fun invokePopup(comp: Component, x: Int, y: Int) {
                val idx = list.locationToIndex(Point(x, y))
                if (idx != -1) {
                    list.selectedIndex = idx
                    val note = listModel.getElementAt(idx)
                    val actionGroup = DefaultActionGroup().apply {
                        if (note.status == NoteStatus.ATTACHED) {
                            add(object : AnAction(NoteBundle.message("action.navigate.file")) {
                                override fun actionPerformed(e: AnActionEvent) {
                                    navigateToNote(note)
                                }
                            })
                            addSeparator()
                        }
                        add(object : AnAction(NoteBundle.message("popup.title.edit")) {
                            override fun actionPerformed(e: AnActionEvent) {
                                NoteDialog(project, note).show()
                            }
                        })
                        add(object : AnAction(NoteBundle.message("popup.title.rebind")) {
                            override fun actionPerformed(e: AnActionEvent) {
                                RebindNoteDialog(project, note).show()
                            }
                        })
                        add(object : AnAction(NoteBundle.message("popup.delete")) {
                            override fun actionPerformed(e: AnActionEvent) {
                                service.deleteNote(note.id)
                            }
                        })
                    }
                    val popupMenu = ActionManager.getInstance().createActionPopupMenu("NotesToolWindowPopup", actionGroup)
                    popupMenu.component.show(comp, x, y)
                }
            }
        })

        add(JBScrollPane(list), BorderLayout.CENTER)

        project.messageBus.connect(this).subscribe(NoteChangeListener.TOPIC, object : NoteChangeListener {
            override fun onNotesChanged() {
                refreshList()
            }
        })
    }

    private fun navigateToNote(note: Note) {
        val file = ProjectPathUtils.resolveProjectFile(project, note.relativePath)
        if (file != null) {
            val descriptor = OpenFileDescriptor(project, file, note.line, 0)
            FileEditorManager.getInstance(project).requestOpenTextEditor(
                descriptor,
                FileEditorOpenRequest.withFocus(true)
            )
        } else {
            Messages.showWarningDialog(
                project,
                NoteBundle.message("msg.file.missing"),
                NoteBundle.message("title.info")
            )
        }
    }

    private fun refreshList() {
        listModel.clear()
        service.getNotes()
            .sortedWith(compareBy({ it.relativePath }, { it.line }))
            .forEach { listModel.addElement(it) }
    }

    override fun dispose() {
        listModel.clear()
    }
}

class NoteListCellRenderer : ListCellRenderer<Note> {
    private val label = JLabel()

    override fun getListCellRendererComponent(
        list: JList<out Note>?,
        value: Note?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        if (value != null) {
            val status = if (value.status == NoteStatus.UNRESOLVED) NoteBundle.message("prefix.unresolved") else ""
            val preview = NoteEditorPresentation.formatShortPreview(value.content)
            label.text = "$status${value.relativePath}:${value.line + 1} - $preview"
        }

        if (isSelected) {
            label.background = list?.selectionBackground
            label.foreground = list?.selectionForeground
        } else {
            label.background = list?.background
            label.foreground = list?.foreground
        }
        label.isOpaque = true
        return label
    }
}
