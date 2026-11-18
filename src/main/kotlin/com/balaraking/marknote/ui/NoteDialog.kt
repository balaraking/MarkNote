package com.balaraking.marknote.ui

import com.balaraking.marknote.i18n.NoteBundle
import com.balaraking.marknote.model.Note
import com.balaraking.marknote.service.NoteService
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.HTMLEditorKitBuilder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JEditorPane
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class NoteDialog(private val project: Project, private val note: Note?) : DialogWrapper(project) {
    private val textArea = JBTextArea()
    private val previewPane = JEditorPane()
    private val previewScrollPane = JBScrollPane(previewPane)
    private val cardLayout = CardLayout()
    private val contentCards = JPanel(cardLayout)
    private val noteService = project.getService(NoteService::class.java)

    private var isPreviewMode = false
    var contentResult: String = ""

    init {
        title = if (note == null) NoteBundle.message("popup.title.add") else NoteBundle.message("popup.title.edit")
        setOKButtonText(NoteBundle.message("popup.save"))
        setCancelButtonText(NoteBundle.message("popup.cancel"))

        configureSourceEditor()
        configurePreviewPane()

        textArea.text = note?.content ?: ""
        init()
    }

    private fun configureSourceEditor() {
        textArea.lineWrap = true
        textArea.wrapStyleWord = true
        textArea.border = JBUI.Borders.empty(6, 8)
        textArea.font = EditorColorsManager.getInstance().globalScheme.getFont(EditorFontType.PLAIN)
        textArea.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = refreshPreview()

            override fun removeUpdate(e: DocumentEvent) = refreshPreview()

            override fun changedUpdate(e: DocumentEvent) = refreshPreview()
        })
    }

    private fun configurePreviewPane() {
        previewPane.contentType = "text/html"
        previewPane.editorKit = HTMLEditorKitBuilder.simple()
        previewPane.isEditable = false
        previewPane.isOpaque = false
        previewPane.addHyperlinkListener(BrowserHyperlinkListener.INSTANCE)
    }

    override fun createCenterPanel(): JComponent {
        val rootPanel = JPanel(BorderLayout(0, JBUI.scale(4)))
        rootPanel.preferredSize = Dimension(JBUI.scale(480), JBUI.scale(240))

        val headerPanel = JPanel(BorderLayout())
        val modeGroup = DefaultActionGroup().apply {
            add(object : ToggleAction(
                NoteBundle.message("popup.mode.source"),
                null,
                AllIcons.Actions.EditSource
            ), DumbAware {
                override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
                override fun isSelected(e: AnActionEvent): Boolean = !isPreviewMode
                override fun setSelected(e: AnActionEvent, state: Boolean) {
                    if (state) switchMode(preview = false)
                }
            })
            add(object : ToggleAction(
                NoteBundle.message("popup.mode.preview"),
                null,
                AllIcons.Actions.Preview
            ), DumbAware {
                override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
                override fun isSelected(e: AnActionEvent): Boolean = isPreviewMode
                override fun setSelected(e: AnActionEvent, state: Boolean) {
                    if (state) switchMode(preview = true)
                }
            })
        }

        val modeToolbar = ActionManager.getInstance().createActionToolbar("MarkNoteDialogModeToolbar", modeGroup, true)
        modeToolbar.targetComponent = rootPanel
        headerPanel.add(modeToolbar.component, BorderLayout.EAST)

        contentCards.add(JBScrollPane(textArea), CARD_SOURCE)
        contentCards.add(previewScrollPane, CARD_PREVIEW)
        cardLayout.show(contentCards, CARD_SOURCE)

        rootPanel.add(headerPanel, BorderLayout.NORTH)
        rootPanel.add(contentCards, BorderLayout.CENTER)
        return rootPanel
    }

    private fun switchMode(preview: Boolean) {
        if (isPreviewMode == preview) return
        isPreviewMode = preview
        if (preview) {
            refreshPreview()
            SwingUtilities.invokeLater {
                previewScrollPane.verticalScrollBar.value = 0
            }
            cardLayout.show(contentCards, CARD_PREVIEW)
        } else {
            cardLayout.show(contentCards, CARD_SOURCE)
            textArea.requestFocusInWindow()
        }
    }

    private fun refreshPreview() {
        previewPane.text = MarkdownHtmlRenderer.toHtml(textArea.text)
        previewPane.caretPosition = 0
    }

    override fun getPreferredFocusedComponent(): JComponent = textArea

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

    private companion object {
        const val CARD_SOURCE = "SOURCE"
        const val CARD_PREVIEW = "PREVIEW"
    }
}
