package com.soulx.marknote.ui

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.vfs.LocalFileSystem
import com.soulx.marknote.i18n.NoteBundle
import com.soulx.marknote.model.Note
import com.soulx.marknote.service.DocumentTextProvider
import com.soulx.marknote.service.NoteAnchorService
import com.soulx.marknote.service.NoteMarkerService
import com.soulx.marknote.service.NoteService
import com.soulx.marknote.util.ProjectPathUtils
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

class RebindNoteDialog(private val project: Project, private val note: Note) : DialogWrapper(project) {
    private val fileField = TextFieldWithBrowseButton()
    private val lineSpinner = JSpinner(SpinnerNumberModel(1, 1, 1_000_000, 1))

    init {
        title = NoteBundle.message("popup.title.rebind")
        setOKButtonText(NoteBundle.message("popup.save"))
        setCancelButtonText(NoteBundle.message("popup.cancel"))

        val descriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor().apply {
            title = NoteBundle.message("popup.rebind.file.chooser.title")
            description = NoteBundle.message("popup.rebind.file.chooser.description")
            project.basePath?.let { base ->
                LocalFileSystem.getInstance().findFileByPath(base)?.let { setRoots(it) }
            }
        }
        fileField.addBrowseFolderListener(TextBrowseFolderListener(descriptor, project))

        // Pre-populate with active editor file & caret line if inside project, otherwise note's existing path/line
        val activeEditor = FileEditorManager.getInstance(project).selectedTextEditor
        val activeFile = activeEditor?.document?.let { FileDocumentManager.getInstance().getFile(it) }
        val activeRelPath = activeFile?.let { ProjectPathUtils.toProjectRelativePath(project, it) }

        if (activeEditor != null && activeFile != null && activeRelPath != null) {
            fileField.text = activeFile.path
            lineSpinner.value = activeEditor.caretModel.logicalPosition.line + 1
        } else {
            val basePath = project.basePath ?: ""
            fileField.text = if (basePath.isNotEmpty()) "$basePath/${note.relativePath}" else note.relativePath
            lineSpinner.value = maxOf(1, note.line + 1)
        }

        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            insets = Insets(4, 4, 4, 4)
            fill = GridBagConstraints.HORIZONTAL
        }

        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 0.0
        panel.add(JLabel(NoteBundle.message("popup.rebind.file.label")), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        fileField.preferredSize = java.awt.Dimension(360, fileField.preferredSize.height)
        panel.add(fileField, gbc)

        gbc.gridx = 0
        gbc.gridy = 1
        gbc.weightx = 0.0
        panel.add(JLabel(NoteBundle.message("popup.rebind.line.label")), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        panel.add(lineSpinner, gbc)

        return panel
    }

    override fun doOKAction() {
        val rawPath = fileField.text.trim().replace('\\', '/')
        val basePath = project.basePath ?: ""
        val relativePath = ProjectPathUtils.toProjectRelativePath(basePath, rawPath)
            ?: ProjectPathUtils.toProjectRelativePath(basePath, "$basePath/$rawPath")

        if (relativePath == null) {
            Messages.showErrorDialog(
                contentPane,
                NoteBundle.message("msg.rebind.invalid.file"),
                NoteBundle.message("title.error")
            )
            return
        }

        val vf = ProjectPathUtils.resolveProjectFile(project, relativePath)
        val document = vf?.let { FileDocumentManager.getInstance().getDocument(it) }
        if (document == null) {
            Messages.showErrorDialog(
                contentPane,
                NoteBundle.message("msg.rebind.invalid.file"),
                NoteBundle.message("title.error")
            )
            return
        }

        val maxLine = maxOf(1, document.lineCount)
        val oneBasedLine = (lineSpinner.value as? Number)?.toInt() ?: 1
        if (oneBasedLine < 1 || oneBasedLine > maxLine) {
            Messages.showErrorDialog(
                contentPane,
                NoteBundle.message("msg.rebind.invalid.line", maxLine),
                NoteBundle.message("title.error")
            )
            return
        }

        val zeroBasedLine = oneBasedLine - 1
        val anchorService = project.getService(NoteAnchorService::class.java)
        val noteService = project.getService(NoteService::class.java)
        val markerService = project.getService(NoteMarkerService::class.java)

        val newAnchor = anchorService.captureAnchor(DocumentTextProvider(document), zeroBasedLine)
        noteService.rebindNote(note.id, relativePath, zeroBasedLine, newAnchor)
        markerService.createMarker(note.id, document, zeroBasedLine)

        super.doOKAction()
    }
}
