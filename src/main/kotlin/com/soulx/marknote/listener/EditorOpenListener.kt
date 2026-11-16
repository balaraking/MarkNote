package com.soulx.marknote.listener

import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener

class EditorOpenListener : EditorFactoryListener {
    override fun editorCreated(event: EditorFactoryEvent) {
        // Initialize RangeMarkers, Gutter Icons, and Inlays here
    }
    
    override fun editorReleased(event: EditorFactoryEvent) {
        // Clean up resources
    }
}