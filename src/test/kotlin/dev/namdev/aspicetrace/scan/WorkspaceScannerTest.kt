package dev.namdev.aspicetrace.scan

import dev.namdev.aspicetrace.WorkspaceTest
import dev.namdev.aspicetrace.cli.DEFAULT_ID_PATTERN
import dev.namdev.aspicetrace.model.ArtifactKind
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.io.path.writeBytes

class WorkspaceScannerTest : WorkspaceTest() {

    private val idPattern = Regex(DEFAULT_ID_PATTERN)

    // @Requirement TRC-SCAN-003
    @Test
    fun `skips build output, ignored paths, excluded globs and generated reports`() {
        val root = workspace(
            "docs/spec.md" to "## SW-HMI-001 Real requirement",
            "build/generated/spec.md" to "## SW-HMI-002 Build output",
            ".git/spec.md" to "## SW-HMI-003 VCS internals",
            "third_party/lib/spec.md" to "## SW-HMI-004 Vendored",
            "samples/demo/spec.md" to "## SW-HMI-005 Sample",
            "out/report/spec.md" to "## SW-HMI-006 Report directory",
            "notes/gaps.md" to "${WorkspaceScanner.GENERATED_MARKER}\n| SW-HMI-007 | A previous report |",
            "src/main/kotlin/A.kt" to "@Requirement(\"SW-HMI-001\") fun a() = 1",
            "src/test/kotlin/ATest.kt" to "@Requirement(\"SW-HMI-001\") fun t() = 1",
        )
        val config = ScanConfig(
            idPattern = idPattern,
            excludes = listOf("third_party", "samples/**"),
            ignoredPaths = setOf(root.resolve("out")),
        )

        val result = WorkspaceScanner(config).scan(root)

        assertEquals(listOf("SW-HMI-001"), result.definitions.map { it.id })
        assertEquals(
            listOf(ArtifactKind.CODE to "src/main/kotlin/A.kt", ArtifactKind.TEST to "src/test/kotlin/ATest.kt"),
            result.references.map { it.kind to it.location.path },
        )
        assertEquals(3, result.filesScanned)
    }

    // @Requirement TRC-SCAN-003
    @Test
    fun `files that are not UTF-8 are reported as skipped`() {
        val root = workspace("docs/spec.md" to "## SW-HMI-001 Title")
        root.resolve("docs/blob.c").writeBytes(byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x00))

        val result = WorkspaceScanner(ScanConfig(idPattern)).scan(root)

        assertEquals(listOf("docs/blob.c"), result.filesSkipped)
        assertEquals(1, result.filesScanned)
    }
}
