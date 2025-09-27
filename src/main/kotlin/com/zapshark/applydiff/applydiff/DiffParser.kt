package com.zapshark.applydiff

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
    private val hunkHeader = Regex("""^@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@""")
    private val fencedDiffOpen = Regex("""^\s*```+\s*diff\b.*$""", RegexOption.IGNORE_CASE)
    private val fencedAnyClose = Regex("""^\s*```+\s*$""")

    fun parse(raw: String): ParsedDiff {
        val trimmed = raw.trim()
        val allLines = trimmed.lines()

        // 1) GitHub suggestion block — accept with or without closing fence
        if (allLines.firstOrNull()?.trim()?.startsWith("```suggestion") == true) {
            val bodyLines = allLines.drop(1).dropLastWhile { fencedAnyClose.matches(it.trim()) }
            val body = bodyLines.joinToString("\n").trim()
            return ParsedDiff(emptyList(), suggestionReplacement = body)
        }

        // 2) Fenced diff without headers — be forgiving about closing ```
        if (allLines.firstOrNull()?.let { fencedDiffOpen.matches(it) } == true) {
            val body = allLines.drop(1).dropLastWhile { fencedAnyClose.matches(it.trim()) }
            val plus  = body.filter { it.startsWith("+") }.map { it.removePrefix("+") }
            val minus = body.filter { it.startsWith("-") }.map { it.removePrefix("-") }
            if (plus.isNotEmpty() || minus.isNotEmpty()) {
                return ParsedDiff(listOf(Hunk(-1, minus.size, plus, minus)))
            }
        }

        // 3) Unified diff with @@ headers (assume current file)
        val hunks = parseUnifiedHunks(allLines)
        if (hunks.isNotEmpty()) return ParsedDiff(hunks)

        // 4) Fallback: plain +/- lines (no fences). Useful for quick ChatGPT snippets.
        val plus  = allLines.filter { it.startsWith("+") }.map { it.removePrefix("+") }
        val minus = allLines.filter { it.startsWith("-") }.map { it.removePrefix("-") }
        if ((plus.isNotEmpty() || minus.isNotEmpty()) && !containsHunkHeader(allLines)) {
            return ParsedDiff(listOf(Hunk(-1, minus.size, plus, minus)))
        }

        // Nothing recognized
        return ParsedDiff(emptyList(), suggestionReplacement = null)
    }

    private fun parseUnifiedHunks(lines: List<String>): List<Hunk> {
        val hunks = mutableListOf<Hunk>()
        var i = 0
        while (i < lines.size) {
            val m = hunkHeader.matchEntire(lines[i])
            if (m != null) {
                val startNew = m.groupValues[3].toInt()
                val oldCount = m.groupValues.getOrNull(2)?.takeIf { it.isNotEmpty() }?.toInt() ?: 1
                val newCount = m.groupValues.getOrNull(4)?.takeIf { it.isNotEmpty() }?.toInt() ?: 1
                i++

                val plus = mutableListOf<String>()
                val minus = mutableListOf<String>()
                var seen = 0

                while (i < lines.size && seen < oldCount + newCount + 200) {
                    val line = lines[i]
                    when {
                        line.startsWith("+") -> plus += line.drop(1)
                        line.startsWith("-") -> minus += line.drop(1)
                        line.startsWith(" ") -> { /* context */ }
                        line.startsWith("@@") -> break
                        fencedAnyClose.matches(line.trim()) -> break
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

    private fun containsHunkHeader(lines: List<String>): Boolean =
        lines.any { hunkHeader.matches(it) }
}
