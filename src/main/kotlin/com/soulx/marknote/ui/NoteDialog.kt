package com.soulx.marknote.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.soulx.marknote.i18n.NoteBundle
import com.soulx.marknote.model.Note
import com.soulx.marknote.service.NoteService
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea

class NoteDialog(private val project: Project, private val note: Note?) : DialogWrapper(project) {
    private val textArea = JTextArea()
    private val noteService = project.getService(NoteService::class.java)

    var contentResult: String = ""

    init {
        title = if (note == null) NoteBundle.message("popup.title.add") else NoteBundle.message("popup.title.edit")
        setOKButtonText(NoteBundle.message("popup.save"))
        setCancelButtonText(NoteBundle.message("popup.cancel"))
        textArea.text = note?.content ?: ""
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        textArea.preferredSize = Dimension(400, 150)
        textArea.lineWrap = true
        textArea.wrapStyleWord = true
        panel.add(JScrollPane(textArea), BorderLayout.CENTER)
        return panel
    }

    override fun doOKAction() {
        contentResult = textArea.text.trim()
        if (contentResult.isEmpty()) {
            JOptionPane.showMessageDialog(
                contentPane,
                NoteBundle.message("msg.note.empty"),
                NoteBundle.message("title.error"),
                JOptionPane.ERROR_MESSAGE
            )
            return
        }

        if (note != null) {
            noteService.updateNoteContent(note.id, contentResult)
        }
        super.doOKAction()
    }

    override fun createActions(): Array<Action> {
        val actions = mutableListOf<Action>(okAction, cancelAction)
        if (note != null) {
            val rebindAction = object : DialogWrapperAction(NoteBundle.message("popup.title.rebind")) {
                override fun doAction(e: ActionEvent?) {
                    close(CANCEL_EXIT_CODE)
                    RebindNoteDialog(project, note).show()
                }
            }
            val deleteAction = object : DialogWrapperAction(NoteBundle.message("popup.delete")) {
                override fun doAction(e: ActionEvent?) {
                    noteService.deleteNote(note.id)
                    close(CANCEL_EXIT_CODE)
                }
            }
            actions.add(0, rebindAction)
            actions.add(0, deleteAction)
        }
        return actions.toTypedArray()
    }
}
