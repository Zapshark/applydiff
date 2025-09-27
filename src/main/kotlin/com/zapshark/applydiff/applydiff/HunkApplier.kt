package com.zapshark.applydiff

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import kotlin.math.max
import kotlin.math.min

object HunkApplier {
    data class Failure(val hunkIndex: Int, val reason: String)

    object Validator {
        fun validate(doc: Document, diff: ParsedDiff): List<Failure> {
            val failures = mutableListOf<Failure>()
            diff.hunks.forEachIndexed { idx, h ->
                if (h.startLineNew >= 1) {
                    val range = docRangeForLines(doc, h.startLineNew, h.oldCount)
                    val oldText = doc.getText(range)
                    val oldLinesNorm = normalize(h.oldLines).joinToString("\n")
                    if (!looseEquals(oldText, oldLinesNorm)) {
                        failures += Failure(idx, "Old text does not match at +${h.startLineNew}.")
                    }
                }
            }
            return failures
        }
    }

    fun apply(doc: Document, diff: ParsedDiff, force: Boolean) {
        diff.suggestionReplacement?.let {
            val range = selectionOrWhole(doc)
            doc.replaceString(range.startOffset, range.endOffset, it)
        }

        diff.hunks.forEach { h ->
            if (h.startLineNew == -1) {
                val range = selectionOrWhole(doc)
                doc.replaceString(range.startOffset, range.endOffset, h.newLines.joinToString("\n"))
            } else {
                val range = docRangeForLines(doc, h.startLineNew, h.oldCount)
                if (!force) {
                    val oldText = doc.getText(range)
                    val oldLinesNorm = normalize(h.oldLines).joinToString("\n")
                    if (!looseEquals(oldText, oldLinesNorm)) return@forEach
                }
                doc.replaceString(range.startOffset, range.endOffset, h.newLines.joinToString("\n"))
            }
        }
    }

    private fun selectionOrWhole(doc: Document): TextRange = TextRange(0, doc.textLength)

    fun docRangeForLines(doc: Document, startLine1Based: Int, lineCount: Int): TextRange {
        val startLine = max(0, startLine1Based - 1)
        val endLine = min(doc.lineCount, startLine + lineCount)
        val startOffset = doc.getLineStartOffset(startLine)
        val endOffset = if (lineCount == 0) startOffset else doc.getLineEndOffset(endLine - 1)
        return TextRange(startOffset, endOffset)
    }

    private fun normalize(lines: List<String>) = lines.map { it.trimEnd('\r') }

    private fun looseEquals(a: String, b: String): Boolean =
        a.replace("\r\n", "\n").trimEnd() == b.replace("\r\n", "\n").trimEnd()
}
