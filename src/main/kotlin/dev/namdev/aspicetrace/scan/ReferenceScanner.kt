package dev.namdev.aspicetrace.scan

import dev.namdev.aspicetrace.model.ArtifactKind
import dev.namdev.aspicetrace.model.Location
import dev.namdev.aspicetrace.model.Reference

/**
 * Finds requirement IDs cited by code and tests.
 *
 * Only IDs that follow the trace tag on the same line count, so the same rule works in any language:
 * `@Requirement("SW-HMI-001")` as a Kotlin/Java annotation, or `@Requirement SW-HMI-001` inside any
 * comment style (C, C++, Python, shell).
 */
// @Requirement TRC-SCAN-001
class ReferenceScanner(
    private val idPattern: Regex,
    private val tag: String = DEFAULT_TAG,
) {

    fun scan(path: String, kind: ArtifactKind, lines: List<String>): List<Reference> =
        lines.withIndex().flatMap { (index, line) ->
            val tagAt = line.indexOf(tag)
            if (tagAt < 0) {
                emptyList()
            } else {
                idPattern.findAll(line, tagAt + tag.length)
                    .map { it.value }
                    .distinct()
                    .map { Reference(it, kind, Location(path, index + 1)) }
                    .toList()
            }
        }

    companion object {
        const val DEFAULT_TAG = "@Requirement"
    }
}
