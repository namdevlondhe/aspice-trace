package dev.namdev.aspicetrace.scan

import dev.namdev.aspicetrace.cli.DEFAULT_ID_PATTERN
import dev.namdev.aspicetrace.model.ArtifactKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReferenceScannerTest {

    private val idPattern = Regex(DEFAULT_ID_PATTERN)

    // @Requirement TRC-SCAN-001
    @Test
    fun `only IDs after the tag on the same line are references`() {
        val lines = listOf(
            """@Requirement("SW-HMI-001", "SW-HMI-002")""",
            "// SW-HMI-003 mentioned without the tag",
            "/* @Requirement SW-HMI-004 */ int32_t fan;",
            "val x = 1 // SW-HMI-005 before the tag, @Requirement SW-HMI-006 after it",
            "# @Requirement SW-HMI-007 SW-HMI-007",
        )

        val references = ReferenceScanner(idPattern).scan("src/A.kt", ArtifactKind.CODE, lines)

        assertEquals(
            listOf("SW-HMI-001" to 1, "SW-HMI-002" to 1, "SW-HMI-004" to 3, "SW-HMI-006" to 4, "SW-HMI-007" to 5),
            references.map { it.id to it.location.line },
        )
        assertTrue(references.all { it.kind == ArtifactKind.CODE && it.location.path == "src/A.kt" })
    }

    // @Requirement TRC-SCAN-001
    @Test
    fun `the tag can be changed`() {
        val references = ReferenceScanner(idPattern, tag = "Verifies:")
            .scan("t.py", ArtifactKind.TEST, listOf("# Verifies: SW-HMI-010", """@Requirement("SW-HMI-011")"""))

        assertEquals(listOf("SW-HMI-010"), references.map { it.id })
    }
}
