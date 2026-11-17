package com.balaraking.marknote.service

import com.balaraking.marknote.model.Note

interface NoteRepository {
    fun getNotes(): List<Note>
    fun getNotesForFile(relativePath: String): List<Note>
    fun saveNote(note: Note)
    fun updateNote(note: Note)
    fun deleteNote(id: String)
}
