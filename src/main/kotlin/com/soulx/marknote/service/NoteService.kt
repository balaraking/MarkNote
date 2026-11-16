package com.soulx.marknote.service

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.soulx.marknote.model.Note
import com.soulx.marknote.model.NoteStatus

@State(
    name = "CodeReadingNoteLite",
    storages = [Storage("CodeReadingNoteLite.xml")]
)
@Service(Service.Level.PROJECT)
class NoteService(private val project: Project) : PersistentStateComponent<NoteService.State> {
    
    class State {
        var notes: MutableList<Note> = mutableListOf()
    }
    
    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    fun getNotes(): List<Note> = myState.notes.toList()
    
    fun addNote(note: Note) {
        myState.notes.add(note)
        // Refresh UI via message bus
    }
    
    fun updateNote(id: String, content: String) {
        myState.notes.find { it.id == id }?.content = content
    }
    
    fun deleteNote(id: String) {
        myState.notes.removeIf { it.id == id }
    }
    
    fun updateStatus(id: String, status: NoteStatus, newLine: Int? = null) {
        val note = myState.notes.find { it.id == id }
        if (note != null) {
            note.status = status
            if (newLine != null) {
                note.line = newLine
            }
        }
    }
}