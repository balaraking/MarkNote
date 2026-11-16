package com.soulx.marknote.model

import java.util.UUID

enum class NoteStatus {
    ATTACHED, UNRESOLVED
}

data class LineAnchor(
    var lineText: String = "",
    var previousLines: List<String> = emptyList(),
    var nextLines: List<String> = emptyList(),
    var contextHash: String = ""
)

data class Note(
    var id: String = UUID.randomUUID().toString(),
    var relativePath: String = "",
    var line: Int = 0,
    var content: String = "",
    var anchor: LineAnchor = LineAnchor(),
    var status: NoteStatus = NoteStatus.ATTACHED
)