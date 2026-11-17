package com.soulx.marknote.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.soulx.marknote.message.NoteChangeListener
import com.soulx.marknote.model.LineAnchor
import com.soulx.marknote.model.Note
import com.soulx.marknote.model.NoteStatus

@State(
    name = "CodeReadingNoteLite",
    storages = [Storage("CodeReadingNoteLite.xml")]
)
class NoteService(private val project: Project) : PersistentStateComponent<NoteService.State>, NoteRepository {

    class State {
        var schemaVersion: Int = 1
        var notes: MutableList<Note> = mutableListOf()
    }

    private var myState = State()
    private val lock = Any()

    override fun getState(): State = synchronized(lock) {
        State().apply {
            schemaVersion = myState.schemaVersion
            notes = myState.notes.map { it.deepCopy() }.toMutableList()
        }
    }

    override fun loadState(state: State) {
        synchronized(lock) {
            myState = State().apply {
                schemaVersion = if (state.schemaVersion > 0) state.schemaVersion else 1
                notes = state.notes.map { it.deepCopy() }.toMutableList()
            }
        }
    }

    override fun getNotes(): List<Note> = synchronized(lock) {
        myState.notes.map { it.deepCopy() }
    }

    override fun getNotesForFile(relativePath: String): List<Note> = synchronized(lock) {
        myState.notes.filter { it.relativePath == relativePath }.map { it.deepCopy() }
    }

    override fun saveNote(note: Note) {
        synchronized(lock) {
            myState.notes.add(note.deepCopy())
        }
        notifyChange()
    }

    override fun updateNote(note: Note) {
        var changed = false
        synchronized(lock) {
            val idx = myState.notes.indexOfFirst { it.id == note.id }
            if (idx != -1) {
                myState.notes[idx] = note.deepCopy()
                changed = true
            }
        }
        if (changed) notifyChange()
    }

    override fun deleteNote(id: String) {
        val removed = synchronized(lock) {
            myState.notes.removeIf { it.id == id }
        }
        if (!project.isDisposed) {
            project.getService(NoteMarkerService::class.java)?.removeMarker(id)
        }
        if (removed) notifyChange()
    }

    fun updateNoteContent(id: String, content: String) {
        var changed = false
        synchronized(lock) {
            val note = myState.notes.find { it.id == id }
            if (note != null && note.content != content) {
                note.content = content
                changed = true
            }
        }
        if (changed) notifyChange()
    }

    fun updateStatusAndLine(id: String, status: NoteStatus, newLine: Int? = null, newAnchor: LineAnchor? = null) {
        var changed = false
        synchronized(lock) {
            val note = myState.notes.find { it.id == id }
            if (note != null) {
                val targetLine = newLine ?: note.line
                val targetAnchor = newAnchor?.deepCopy() ?: note.anchor
                if (note.status != status || note.line != targetLine || note.anchor != targetAnchor) {
                    note.status = status
                    note.line = targetLine
                    note.anchor = targetAnchor
                    changed = true
                }
            }
        }
        if (changed) notifyChange()
    }

    fun rebindNote(id: String, newRelativePath: String, newLine: Int, newAnchor: LineAnchor) {
        var changed = false
        synchronized(lock) {
            val note = myState.notes.find { it.id == id }
            if (note != null) {
                note.relativePath = newRelativePath
                note.line = newLine
                note.anchor = newAnchor.deepCopy()
                note.status = NoteStatus.ATTACHED
                changed = true
            }
        }
        if (changed) notifyChange()
    }

    fun notifyChange() {
        if (!project.isDisposed) {
            val app = ApplicationManager.getApplication()
            if (app == null || app.isDispatchThread) {
                project.messageBus.syncPublisher(NoteChangeListener.TOPIC).onNotesChanged()
            } else {
                app.invokeLater {
                    if (!project.isDisposed) {
                        project.messageBus.syncPublisher(NoteChangeListener.TOPIC).onNotesChanged()
                    }
                }
            }
        }
    }
}
