package com.balaraking.marknote.message

import com.intellij.util.messages.Topic

interface NoteChangeListener {
    companion object {
        val TOPIC = Topic.create("CodeReadingNoteLite Changes", NoteChangeListener::class.java)
    }

    fun onNotesChanged()
}
