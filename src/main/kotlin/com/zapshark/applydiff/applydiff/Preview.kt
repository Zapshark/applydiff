package com.zapshark.applydiff

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange

object Preview {
    data class BuildResult(val newText: String, val failures: List<HunkApplier.Failure>)

    fun buildPatchedText(doc: Document, diff: ParsedDiff, force: Boolean, selection: TextRange): BuildResult {
        val failures = if (force) emptyList() else HunkApplier.Validator.validate(doc, diff)
        val original = doc.text
        val edits = mutableListOf<Triple<Int, Int, String>>() // (startOffset, endOffset, replacement)

        // Handle suggestion replacement
        diff.suggestionReplacement?.let {
            edits += Triple(selection.startOffset, selection.endOffset, it)
        }

        // Convert line-based hunks to concrete edits
        diff.hunks.forEachIndexed { idx, h ->
            if (h.startLineNew == -1) {
                edits += Triple(selection.startOffset, selection.endOffset, h.newLines.joinToString("\n"))
            } else {
                val range = HunkApplier.docRangeForLines(doc, h.startLineNew, h.oldCount)
                if (!force) {
                    val oldText = doc.getText(range)
                    val oldLinesNorm = h.oldLines.joinToString("\n").replace("\r\n", "\n").trimEnd()
                    val docNorm = oldText.replace("\r\n", "\n").trimEnd()
                    if (docNorm != oldLinesNorm) {
                        // skip this edit in preview if not matching
                        return@forEachIndexed
                    }
                }
                edits += Triple(range.startOffset, range.endOffset, h.newLines.joinToString("\n"))
            }
        }

        // Apply edits from the end to avoid shifting offsets
        val sb = StringBuilder(original)
        edits.sortedByDescending { it.first }.forEach { (start, end, repl) ->
            sb.replace(start, end, repl)
        }

        return BuildResult(sb.toString(), failures)
    }

    fun show(project: Project, oldText: String, newText: String, title: String = "Preview Changes") {
        val factory = DiffContentFactory.getInstance()
        val left = factory.create(project, oldText)
        val right = factory.create(project, newText)
        val request = SimpleDiffRequest(title, left, right, "Current", "Patched")
        DiffManager.getInstance().showDiff(project, request)
    }
}
