package com.balaraking.marknote.ui

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object NoteIcons {
    @JvmField
    val MARK_NOTE: Icon = IconLoader.getIcon("/icons/markNote.svg", NoteIcons::class.java)

    @JvmField
    val MARK_GUTTER: Icon = IconLoader.getIcon("/icons/markNote_gutter.svg", NoteIcons::class.java)

    @JvmField
    val MARK_INLAY: Icon = IconLoader.getIcon("/icons/markNote_inlay.svg", NoteIcons::class.java)
}
