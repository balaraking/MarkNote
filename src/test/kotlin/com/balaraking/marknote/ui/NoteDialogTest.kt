package com.balaraking.marknote.ui

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.components.JBTextArea
import javax.swing.JEditorPane
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

class NoteDialogTest : BasePlatformTestCase() {

    fun testSourceChangesRefreshMarkdownPreview() {
        val dialog = NoteDialog(project, null)
        val source = dialog.privateField<JBTextArea>("textArea")
        val preview = dialog.privateField<JEditorPane>("previewPane")

        assertFalse(dialog.privateField<Boolean>("isPreviewMode"))

        source.text = "# Updated title"

        assertTrue(preview.text.contains("Updated title"))
        assertTrue(preview.text.contains("<h1"))
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> NoteDialog.privateField(name: String): T {
        return NoteDialog::class.java.getDeclaredField(name).let { field ->
            field.isAccessible = true
            field.get(this) as T
        }
    }
}
