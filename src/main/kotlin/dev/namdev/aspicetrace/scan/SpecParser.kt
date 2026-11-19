package dev.namdev.aspicetrace.scan

import dev.namdev.aspicetrace.model.Location
import dev.namdev.aspicetrace.model.Requirement

/**
 * Finds requirement definitions in a Markdown spec.
 *
 * An ID defines a requirement only when it starts a heading (`## SW-HMI-001: Title`) or is the first cell of a
 * table row (`| SW-HMI-001 | Title |`). Mentions in prose ("see SW-HMI-001") are not definitions, and fenced
 * code blocks are skipped so that examples in a README never create requirements.
 */
// @Requirement TRC-SPEC-001 TRC-SPEC-002 TRC-SPEC-003
class SpecParser(private val idPattern: Regex) {

    fun parse(path: String, lines: List<String>): List<Requirement> {
        val found = mutableListOf<Requirement>()
        var openFence: String? = null
        for ((index, raw) in lines.withIndex()) {
            val line = raw.trim()
            val fence = FENCE.find(line)?.value
            val insideCode = openFence != null || fence != null
            openFence = nextFence(openFence, fence)
            if (!insideCode) {
                definitionIn(line)?.let { (id, title) -> found += Requirement(id, title, Location(path, index + 1)) }
            }
        }
        return found
    }

    /** Opens a fence, closes it with a marker of the same character and at least the same length, or keeps it. */
    private fun nextFence(open: String?, marker: String?): String? = when {
        marker == null -> open
        open == null -> marker
        marker.first() == open.first() && marker.length >= open.length -> null
        else -> open
    }

    private fun definitionIn(line: String): Pair<String, String>? = when {
        line.startsWith("#") -> fromHeading(line)
        line.startsWith("|") -> fromTableRow(line)
        else -> null
    }

    private fun fromHeading(line: String): Pair<String, String>? {
        val text = line.trimStart('#')
        // "#tag" is not a heading; CommonMark needs a space after the hashes.
        if (text.isNotEmpty() && !text.first().isWhitespace()) return null
        return splitIdAndTitle(text.trim().trimEnd('#').trim())
    }

    private fun fromTableRow(line: String): Pair<String, String>? {
        val cells = line.trim().removePrefix("|").removeSuffix("|").split('|').map { it.trim() }
        val (id, rest) = splitIdAndTitle(cells.first()) ?: return null
        val title = rest.ifEmpty { cells.getOrNull(1)?.let(::stripDecoration).orEmpty() }
        return id to title
    }

    /** "**SW-HMI-001**: Title" -> ("SW-HMI-001", "Title"), or null when the text does not start with an ID. */
    private fun splitIdAndTitle(text: String): Pair<String, String>? {
        val start = text.indexOfFirst { it !in LEADING_DECORATION }
        val match = if (start < 0) null else idPattern.find(text, start)?.takeIf { it.range.first == start }
        return match?.let { it.value to stripDecoration(text.substring(it.range.last + 1)) }
    }

    private fun stripDecoration(text: String): String =
        text.trimStart { it in TRAILING_DECORATION || it in SEPARATORS || it.isWhitespace() }.trimEnd()

    private companion object {
        val FENCE = Regex("^(`{3,}|~{3,})")
        const val LEADING_DECORATION = "*_`["
        const val TRAILING_DECORATION = "*_`]"
        const val SEPARATORS = ":.-–—"
    }
}
