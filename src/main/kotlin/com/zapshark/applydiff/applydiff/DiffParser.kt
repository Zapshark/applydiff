
package com.zapshark.applydiff
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

data class Hunk(
    val startLineNew: Int,      // 1-based; -1 means "use selection"
    val oldCount: Int,
    val newLines: List<String>,
    val oldLines: List<String>
)

data class ParsedDiff(
    val hunks: List<Hunk>,
    val suggestionReplacement: String? = null
)

object DiffParser {
    // @@ -<oldStart>[,<oldCount>] +<newStart>[,<newCount>] @@
    private val hunkHeader = Regex("""^@@\s+-(\d+)(?:,(\d+))?\s+\+(\d+)(?:,(\d+))?\s+@@""")
    // Matches a fence line like ```diff / ``` / ~~~
    private val fenceLine = Regex("""^\s*(```+|~~~+)\s*\w*\s*$""")
    // Fences: ```diff / ```patch / ```udiff / ```something-with-diff ...
    private val fencedDiffOpen = Regex("""^\s*(```+|~~~+)\s*(\w+)?[^\r\n]*$""", RegexOption.IGNORE_CASE)
    private val fencedAnyClose = Regex("""^\s*(```+|~~~+)\s*$""")
    private val looksLikeDiffLang = Regex("""(?i)\b(diff|patch|udiff|git-diff|unidiff)\b""")

    // Suggestion fences may appear anywhere
    private val fencedSuggestionOpen = Regex("""^\s*```+\s*suggestion\b.*$""", RegexOption.IGNORE_CASE)

    // Lines we should ignore when scanning unified diffs
    private val ignoreUnifiedHeader = Regex("""^(diff --git|index\s|---\s|[+]{3}\s|No newline at end of file)""")

    fun parse(raw: String): ParsedDiff {
        if (raw.isBlank()) return ParsedDiff(emptyList(), null)

        // Normalize newlines, strip BOMs just in case
        val text = raw.replace("\r\n", "\n").replace("\r", "\n").removePrefix("\uFEFF")
        val all = text.lines()

        // 1) Capture first suggestion block (if any), but keep scanning for diffs too
        val suggestion = extractFirstSuggestion(all)

        // 2) Collect hunks from all fenced diff-like blocks anywhere
        val hunksFromFences = extractAllFencedDiffHunks(all)

        // 3) Collect unified hunks from the whole text (outside or inside fences)
        val hunksUnified = parseUnifiedHunks(all)

        // 4) If nothing yet, try "selection" (+/-) fallback across the whole text
        val hunksSelection =
            if (hunksFromFences.isEmpty() && hunksUnified.isEmpty())
                parseSelectionPlusMinus(all)
            else emptyList()

        val allHunks = (hunksFromFences + hunksUnified + hunksSelection)
        return ParsedDiff(allHunks, suggestion)
    }

    // -------------------- fenced parsing --------------------

    private fun extractAllFencedDiffHunks(lines: List<String>): List<Hunk> {
        val hunks = mutableListOf<Hunk>()
        var i = 0
        while (i < lines.size) {
            val open = fencedDiffOpen.matchEntire(lines[i])
            if (open != null) {
                val fenceToken = open.groupValues[1] // ``` or ~~~
                val lang = open.groupValues.getOrNull(2)?.trim().orEmpty()
                val isDiffFence = lang.isBlank() || looksLikeDiffLang.containsMatchIn(lang)

                // Gather body until matching close fence OR until EOF
                val body = mutableListOf<String>()
                i++
                while (i < lines.size) {
                    val maybeClose = lines[i]
                    if (fencedAnyClose.matches(maybeClose.trim())) {
                        i++ // consume close
                        break
                    }
                    body += maybeClose
                    i++
                }

                if (isDiffFence && body.isNotEmpty()) {
                    val parsed =
                        if (containsHunkHeader(body)) parseUnifiedHunks(body)
                        else parseSelectionPlusMinus(body)
                    hunks += parsed
                    continue
                }
            }
            i++
        }
        return hunks
    }

    private fun extractFirstSuggestion(lines: List<String>): String? {
        var i = 0
        while (i < lines.size) {
            val open = fencedSuggestionOpen.matches(lines[i].trim())
            if (open) {
                val body = mutableListOf<String>()
                i++
                while (i < lines.size) {
                    val line = lines[i]
                    if (fencedAnyClose.matches(line.trim())) {
                        break
                    }
                    body += line
                    i++
                }
                val text = body.joinToString("\n").trim()
                if (text.isNotEmpty()) return text
                break
            }
            i++
        }
        return null
    }

    // -------------------- unified hunks parser --------------------

    private fun parseUnifiedHunks(lines: List<String>): List<Hunk> {
        val hunks = mutableListOf<Hunk>()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val m = hunkHeader.matchEntire(line)
            if (m != null) {
                val startNew = m.groupValues[3].toInt()
                val oldCount = m.groupValues.getOrNull(2)?.takeIf { it.isNotEmpty() }?.toInt() ?: 1
                val newCount = m.groupValues.getOrNull(4)?.takeIf { it.isNotEmpty() }?.toInt() ?: 1
                i++

                val plus = mutableListOf<String>()
                val minus = mutableListOf<String>()
                var seen = 0

                // Read body until we've plausibly consumed enough, or hit next hunk/close
                while (i < lines.size && seen < oldCount + newCount + 200) {
                    val l = lines[i]

                    // Stop conditions
                    if (l.startsWith("@@")) break
                    if (fencedAnyClose.matches(l.trim())) break

                    // Ignore git headers and "No newline..." markers
                    if (ignoreUnifiedHeader.containsMatchIn(l)) {
                        i++; continue
                    }

                    when {
                        l.startsWith("+") -> plus += l.drop(1)
                        l.startsWith("-") -> minus += l.drop(1)
                        l.startsWith(" ") -> { /* context; ignore */ }
                        else -> { /* stray line in diff; ignore */ }
                    }
                    seen++; i++
                }

                hunks += Hunk(startNew, oldCount, plus, minus)
            } else {
                i++
            }
        }
        return hunks
    }

    // -------------------- selection (+/- only) parser --------------------

    private fun parseSelectionPlusMinus(lines: List<String>): List<Hunk> {
        // Accept leading spaces before +/-
        val plus  = lines.filter { it.startsWith("+") || it.startsWith(" +") }.map { it.trimStart().removePrefix("+") }
        val minus = lines.filter { it.startsWith("-") || it.startsWith(" -") }.map { it.trimStart().removePrefix("-") }
        return if (plus.isNotEmpty() || minus.isNotEmpty())
            listOf(Hunk(-1, minus.size, plus, minus))
        else
            emptyList()
    }

    // -------------------- helpers --------------------

    private fun containsHunkHeader(lines: List<String>): Boolean =
        lines.any { hunkHeader.matches(it) }


    // ---- UI prompt + cleanup fallback ---------------------------------------

    /**
     * UI-friendly wrapper: try parse(raw). If it fails, ask the user whether
     * to auto-clean prefixes (fences / leading + / - / >) and retry.
     */
    fun parseWithPrompt(project: Project?, raw: String): ParsedDiff {
        val first = parse(raw)
        if (first.hunks.isNotEmpty() || first.suggestionReplacement != null) {
            return first
        }

        val yes = askYesNo(
            project,
            "Couldn't parse that diff.\n\nTry auto-cleaning the pasted text (strip ``` fences and leading + / - symbols) and apply as a selection?",
            "Apply Diff"
        )
        if (yes != Messages.YES) return first

        val cleaned = cleanLeadingSymbols(raw)
        val retry = parse(cleaned)
        if (retry.hunks.isNotEmpty() || retry.suggestionReplacement != null) {
            return retry
        }

        // Last-ditch: build a selection hunk from cleaned +/- lines
        val lines = cleaned.lines()
        val plus  = lines.filter { it.trimStart().startsWith("+") }.map { it.trimStart().removePrefix("+") }
        val minus = lines.filter { it.trimStart().startsWith("-") }.map { it.trimStart().removePrefix("-") }
        return if (plus.isNotEmpty() || minus.isNotEmpty())
            ParsedDiff(listOf(Hunk(-1, minus.size, plus, minus)))
        else
            ParsedDiff(emptyList(), null)
    }

    private fun askYesNo(project: Project?, message: String, title: String): Int {
        val app = ApplicationManager.getApplication()
        var result = Messages.NO
        val task = Runnable {
            result = Messages.showYesNoDialog(
                project, message, title,
                Messages.getYesButton(), Messages.getNoButton(), null
            )
        }
        if (app.isDispatchThread) task.run() else app.invokeAndWait(task)
        return result
    }

    /** Remove code fences and a single leading +/-/> marker while keeping indentation. */
    private fun cleanLeadingSymbols(raw: String): String {
        val normalized = raw.replace("\r\n", "\n").replace("\r", "\n")
        return normalized.lines()
            .filterNot { fenceLine.matches(it.trim()) }
            .map { stripLeadingMarkerKeepingIndent(it) }
            .joinToString("\n")
            .trim()
    }

    private fun stripLeadingMarkerKeepingIndent(line: String): String {
        val idx = line.indexOfFirst { !it.isWhitespace() }
        if (idx < 0) return line
        val c = line[idx]
        return if (c == '+' || c == '-' || c == '>') {
            line.removeRange(idx, idx + 1)
        } else {
            line
        }
    }
}
