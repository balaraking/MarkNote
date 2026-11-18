package com.balaraking.marknote.ui

import com.balaraking.marknote.i18n.NoteBundle
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser

object MarkdownHtmlRenderer {
    private val flavour = GFMFlavourDescriptor()

    fun toHtml(markdown: String): String {
        val trimmed = markdown.trim()
        val bodyHtml = if (trimmed.isEmpty()) {
            "<p style='color:${ColorUtil.toHtmlColor(JBColor.GRAY)};font-style:italic;'>" +
                NoteBundle.message("popup.preview.empty") +
                "</p>"
        } else {
            val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(markdown)
            HtmlGenerator(markdown, parsedTree, flavour, false).generateHtml()
        }

        val labelFont = UIUtil.getLabelFont()
        val codeFontName = EditorColorsManager.getInstance().globalScheme.editorFontName
        val fgColor = ColorUtil.toHtmlColor(UIUtil.getLabelForeground())
        val linkColor = ColorUtil.toHtmlColor(JBUI.CurrentTheme.Link.Foreground.ENABLED)
        val borderColor = ColorUtil.toHtmlColor(JBColor.border())
        val codeBgColor = ColorUtil.toHtmlColor(JBColor(0xF2F4F7, 0x2B2D30))
        val quoteColor = ColorUtil.toHtmlColor(JBColor.GRAY)

        return """
            <html>
            <head>
            <style>
                body {
                    font-family: "${labelFont.family}", sans-serif;
                    font-size: ${labelFont.size}pt;
                    color: $fgColor;
                    margin: 6px 8px;
                    line-height: 1.45;
                }
                h1, h2, h3, h4, h5, h6 {
                    margin-top: 8px;
                    margin-bottom: 4px;
                    font-weight: bold;
                }
                h1 { font-size: 1.35em; border-bottom: 1px solid $borderColor; padding-bottom: 2px; }
                h2 { font-size: 1.2em; }
                h3 { font-size: 1.08em; }
                p { margin: 4px 0; }
                ul, ol { margin: 4px 0 4px 18px; padding-left: 0; }
                li { margin: 2px 0; }
                a { color: $linkColor; text-decoration: none; }
                code {
                    font-family: "$codeFontName", monospace;
                    background-color: $codeBgColor;
                    padding: 1px 4px;
                }
                pre {
                    font-family: "$codeFontName", monospace;
                    background-color: $codeBgColor;
                    border: 1px solid $borderColor;
                    padding: 6px 8px;
                    margin: 6px 0;
                }
                pre code {
                    background-color: transparent;
                    padding: 0;
                }
                blockquote {
                    border-left: 3px solid $borderColor;
                    margin: 6px 0;
                    padding-left: 8px;
                    color: $quoteColor;
                }
                table {
                    border-collapse: collapse;
                    margin: 6px 0;
                    width: 100%;
                }
                th, td {
                    border: 1px solid $borderColor;
                    padding: 4px 6px;
                    text-align: left;
                }
                th {
                    background-color: $codeBgColor;
                    font-weight: bold;
                }
                hr {
                    border: 0;
                    border-top: 1px solid $borderColor;
                    margin: 8px 0;
                }
            </style>
            </head>
            <body>$bodyHtml</body>
            </html>
        """.trimIndent()
    }
}
