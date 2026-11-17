package com.balaraking.marknote.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DummyTextProvider(private val lines: List<String>) : TextProvider {
    override val lineCount: Int get() = lines.size
    override fun getLineText(line: Int): String = if (line in lines.indices) lines[line] else ""
}

class NoteAnchorServiceTest {

    private lateinit var service: NoteAnchorService

    @BeforeEach
    fun setup() {
        service = NoteAnchorService()
    }

    @Test
    fun `normalization trims outer whitespace and normalizes newlines without collapsing inner spaces`() {
        assertEquals("val   a = 1", service.normalize("   val   a = 1  "))
        assertEquals("fun test() {", service.normalize("fun test() {\r\n"))
    }

    @Test
    fun `captureAnchor captures up to two surrounding lines including blank lines`() {
        val code = listOf("line1", "", "target", "line4", "line5")
        val provider = DummyTextProvider(code)

        val anchor = service.captureAnchor(provider, 2)
        assertEquals("target", anchor.lineText)
        assertEquals(listOf("line1", ""), anchor.previousLines)
        assertEquals(listOf("line4", "line5"), anchor.nextLines)
        assertTrue(anchor.contextHash.isNotEmpty())
    }

    @Test
    fun `findMatch resolves unique line after lines inserted above`() {
        val oldCode = listOf("line1", "line2", "target", "line4")
        val anchor = service.captureAnchor(DummyTextProvider(oldCode), 2)

        val newCode = listOf("header1", "header2", "line1", "line2", "target", "line4")
        assertEquals(4, service.findMatch(DummyTextProvider(newCode), anchor))
    }

    @Test
    fun `findMatch resolves unique line after lines deleted above`() {
        val oldCode = listOf("header1", "header2", "line1", "target", "line4")
        val anchor = service.captureAnchor(DummyTextProvider(oldCode), 3)

        val newCode = listOf("line1", "target", "line4")
        assertEquals(1, service.findMatch(DummyTextProvider(newCode), anchor))
    }

    @Test
    fun `findMatch disambiguates duplicate line text via unique context hash`() {
        val code = listOf("fun a() {", "    return 1", "}", "fun b() {", "    return 1", "}")
        val provider = DummyTextProvider(code)

        val anchorB = service.captureAnchor(provider, 4)
        assertEquals(4, service.findMatch(provider, anchorB))
    }

    @Test
    fun `findMatch disambiguates duplicate line text when one surrounding line changed`() {
        val oldCode = listOf("fun a() {", "    val x = 1", "    return 0", "}", "fun b() {", "    val y = 2", "    return 0", "}")
        val anchorB = service.captureAnchor(DummyTextProvider(oldCode), 6)

        val newCode = listOf("fun a() {", "    val x = 1", "    return 0", "}", "fun b_renamed() {", "    val y = 2", "    return 0", "}")
        assertEquals(6, service.findMatch(DummyTextProvider(newCode), anchorB))
    }

    @Test
    fun `findMatch returns null when duplicate candidates have identical context hash`() {
        val code = listOf(
            "header",
            "if (flag) {",
            "    doWork()",
            "}",
            "tail",
            "header",
            "if (flag) {",
            "    doWork()",
            "}",
            "tail"
        )
        val provider = DummyTextProvider(code)
        val anchor = service.captureAnchor(provider, 2)

        assertNull(service.findMatch(provider, anchor), "Identical duplicate blocks must be unresolved, never pick first")
    }

    @Test
    fun `findMatch returns null when duplicate candidates tie on partial context score`() {
        val oldCode = listOf("blockStart", "    process()", "blockEnd")
        val anchor = service.captureAnchor(DummyTextProvider(oldCode), 1)

        val newCode = listOf(
            "blockStart",
            "    process()",
            "otherEnd",
            "otherStart",
            "    process()",
            "blockEnd"
        )
        assertNull(service.findMatch(DummyTextProvider(newCode), anchor))
    }

    @Test
    fun `findMatch returns null when target line is deleted or completely replaced`() {
        val oldCode = listOf("line1", "val secret = 42", "line3")
        val anchor = service.captureAnchor(DummyTextProvider(oldCode), 1)

        val deletedCode = listOf("line1", "line3")
        assertNull(service.findMatch(DummyTextProvider(deletedCode), anchor))

        val replacedCode = listOf("line1", "val publicValue = 100", "line3")
        assertNull(service.findMatch(DummyTextProvider(replacedCode), anchor))
    }
}
