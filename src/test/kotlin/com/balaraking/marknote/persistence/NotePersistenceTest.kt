package com.balaraking.marknote.persistence

import com.intellij.openapi.components.State
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.xmlb.XmlSerializer
import com.balaraking.marknote.model.LineAnchor
import com.balaraking.marknote.model.Note
import com.balaraking.marknote.model.NoteStatus
import com.balaraking.marknote.service.NoteService
import java.io.File

class NotePersistenceTest : BasePlatformTestCase() {

    fun testStateStorageConfiguredExclusivelyToCodeReadingNoteLiteXml() {
        val stateAnnotation = NoteService::class.java.getAnnotation(State::class.java)
        assertNotNull(stateAnnotation)
        assertEquals("CodeReadingNoteLite", stateAnnotation.name)
        val storages = stateAnnotation.storages.map { it.value }
        assertEquals(listOf("CodeReadingNoteLite.xml"), storages)
    }

    fun testXmlSerializationRoundTripAndSchemaVersion() {
        val service = project.getService(NoteService::class.java)
        service.loadState(NoteService.State())
        val anchor = LineAnchor(
            lineText = "val count = items.size",
            previousLines = listOf("fun compute(items: List<String>): Int {"),
            nextLines = listOf("return count", "}"),
            contextHash = "abc123hash"
        )
        val note = Note(
            id = "note-1",
            relativePath = "src/App.kt",
            line = 12,
            content = "Check empty list edge case",
            anchor = anchor,
            status = NoteStatus.ATTACHED
        )
        service.saveNote(note)

        val exportedState = service.getState()
        assertEquals(1, exportedState.schemaVersion)
        assertEquals(1, exportedState.notes.size)

        // Mutating exportedState must not mutate internal service state
        exportedState.notes.clear()
        assertEquals(1, service.getNotes().size)

        // Serialize to XML and load into fresh state
        val xmlElement = XmlSerializer.serialize(service.getState())
        val deserialized = XmlSerializer.deserialize(xmlElement, NoteService.State::class.java)

        val targetService = NoteService(project)
        targetService.loadState(deserialized)

        assertEquals(1, targetService.getState().schemaVersion)
        val restoredNotes = targetService.getNotes()
        assertEquals(1, restoredNotes.size)
        assertEquals("note-1", restoredNotes[0].id)
        assertEquals("src/App.kt", restoredNotes[0].relativePath)
        assertEquals(12, restoredNotes[0].line)
        assertEquals("Check empty list edge case", restoredNotes[0].content)
        assertEquals(NoteStatus.ATTACHED, restoredNotes[0].status)
        assertEquals(anchor, restoredNotes[0].anchor)
    }

    fun testLegacyCodeReadingNoteXmlIsIgnoredAndNotModified() {
        val basePath = project.basePath ?: return
        val ideaDir = File(basePath, ".idea").apply { mkdirs() }
        val legacyFile = File(ideaDir, "CodeReadingNote.xml")
        val legacyContent = "<project version=\"4\"><component name=\"CodeReadingNote\"><legacy/></component></project>"
        legacyFile.writeText(legacyContent)

        val service = project.getService(NoteService::class.java)
        assertTrue(service.getNotes().none { it.content.contains("legacy") })
        assertEquals(legacyContent, legacyFile.readText())
    }
}
