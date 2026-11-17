package com.soulx.marknote.service

import com.soulx.marknote.model.LineAnchor
import java.security.MessageDigest

interface TextProvider {
    val lineCount: Int
    fun getLineText(line: Int): String
}

class NoteAnchorService {

    fun captureAnchor(provider: TextProvider, line: Int): LineAnchor {
        val maxLines = provider.lineCount
        if (line < 0 || line >= maxLines) return LineAnchor()

        val lineText = normalize(provider.getLineText(line))
        val prevLines = (maxOf(0, line - 2) until line).map { normalize(provider.getLineText(it)) }
        val nextLines = ((line + 1) until minOf(maxLines, line + 3)).map { normalize(provider.getLineText(it)) }

        val hashInput = prevLines.joinToString("\n") + "\n---\n" + lineText + "\n---\n" + nextLines.joinToString("\n")
        val contextHash = md5(hashInput)

        return LineAnchor(lineText, prevLines, nextLines, contextHash)
    }

    fun findMatch(provider: TextProvider, anchor: LineAnchor): Int? {
        val maxLines = provider.lineCount
        val targetText = anchor.lineText
        if (targetText.isEmpty() && anchor.previousLines.isEmpty() && anchor.nextLines.isEmpty()) {
            return null
        }

        val candidates = mutableListOf<Int>()
        for (i in 0 until maxLines) {
            if (normalize(provider.getLineText(i)) == targetText) {
                candidates.add(i)
            }
        }

        if (candidates.isEmpty()) {
            return null
        }

        if (candidates.size == 1) {
            return candidates[0]
        }

        // Multiple candidates exist: never silently pick the first when ambiguous.
        // 1. Check exact context hash matches first.
        val exactHashMatches = candidates.filter { cand ->
            captureAnchor(provider, cand).contextHash == anchor.contextHash
        }
        if (exactHashMatches.size == 1) {
            return exactHashMatches[0]
        }
        if (exactHashMatches.size > 1) {
            // Identical code + identical surrounding context appears multiple times -> ambiguous -> UNRESOLVED
            return null
        }

        // 2. Partial context window scoring when surrounding lines shifted slightly
        val scored = candidates.map { cand ->
            val candAnchor = captureAnchor(provider, cand)
            val score = positionalOverlap(anchor.previousLines.asReversed(), candAnchor.previousLines.asReversed()) +
                positionalOverlap(anchor.nextLines, candAnchor.nextLines)
            cand to score
        }

        val maxScore = scored.maxOf { it.second }
        if (maxScore <= 0) {
            return null
        }

        val bestCandidates = scored.filter { it.second == maxScore }
        return if (bestCandidates.size == 1) bestCandidates[0].first else null
    }

    fun normalize(text: String): String {
        // Only strip leading/trailing whitespace and normalize CR/LF. Preserve internal whitespace.
        return text.trim().replace("\r\n", "\n").replace("\r", "\n")
    }

    private fun positionalOverlap(expected: List<String>, actual: List<String>): Int {
        val limit = minOf(expected.size, actual.size)
        var score = 0
        for (i in 0 until limit) {
            if (expected[i] == actual[i]) {
                score++
            }
        }
        return score
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

class DocumentTextProvider(private val document: com.intellij.openapi.editor.Document) : TextProvider {
    override val lineCount: Int get() = document.lineCount
    override fun getLineText(line: Int): String {
        if (line < 0 || line >= document.lineCount) return ""
        val start = document.getLineStartOffset(line)
        val end = document.getLineEndOffset(line)
        return document.getText(com.intellij.openapi.util.TextRange(start, end))
    }
}
