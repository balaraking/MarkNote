package com.balaraking.marknote.ui

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.jupiter.api.Assertions.assertTrue

class MarkdownHtmlRendererTest : BasePlatformTestCase() {

    fun testRendersMarkdownHeadingsAndEmphasisAsHtml() {
        val html = MarkdownHtmlRenderer.toHtml("# Title\n\n**bold**")

        assertTrue(html.contains("<h1>Title</h1>"))
        assertTrue(html.contains("<strong>bold</strong>"))
    }

    fun testRendersAnExplanatoryPlaceholderForEmptyMarkdown() {
        val html = MarkdownHtmlRenderer.toHtml("  \n")

        assertTrue(html.contains("暂无可预览内容") || html.contains("Nothing to preview."))
    }
}
