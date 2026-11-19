package dev.namdev.aspicetrace.analysis

import dev.namdev.aspicetrace.model.ArtifactKind
import dev.namdev.aspicetrace.model.ArtifactKind.CODE
import dev.namdev.aspicetrace.model.ArtifactKind.TEST
import dev.namdev.aspicetrace.model.Location
import dev.namdev.aspicetrace.model.Reference
import dev.namdev.aspicetrace.model.Requirement
import dev.namdev.aspicetrace.model.ScanResult
import org.junit.Assert.assertEquals
import org.junit.Test

class TraceAnalyzerTest {

    private fun requirement(id: String, path: String = "spec.md", line: Int = 1) =
        Requirement(id, "Title of $id", Location(path, line))

    private fun reference(id: String, kind: ArtifactKind, path: String, line: Int = 1) =
        Reference(id, kind, Location(path, line))

    // @Requirement TRC-ANA-001
    @Test
    fun `status reflects implementation and verification`() {
        val scan = ScanResult(
            definitions = listOf(
                requirement("SW-HMI-004"),
                requirement("SW-HMI-001"),
                requirement("SW-HMI-002"),
                requirement("SW-HMI-003"),
            ),
            references = listOf(
                reference("SW-HMI-001", CODE, "a.kt"),
                reference("SW-HMI-001", CODE, "a.kt"),
                reference("SW-HMI-001", TEST, "ATest.kt"),
                reference("SW-HMI-002", CODE, "b.kt"),
                reference("SW-HMI-003", TEST, "CTest.kt"),
            ),
            filesScanned = 5,
        )

        val report = TraceAnalyzer.analyze(scan)

        assertEquals(
            listOf(
                "SW-HMI-001" to TraceStatus.COVERED,
                "SW-HMI-002" to TraceStatus.NOT_VERIFIED,
                "SW-HMI-003" to TraceStatus.NOT_IMPLEMENTED,
                "SW-HMI-004" to TraceStatus.UNTRACED,
            ),
            report.rows.map { it.requirement.id to it.status },
        )
        assertEquals(listOf(Location("a.kt", 1)), report.rows.first().implementedBy)
        assertEquals(50, report.implementedPercent)
        assertEquals(50, report.verifiedPercent)
        assertEquals(3, report.gapCount)
    }

    // @Requirement TRC-ANA-002
    @Test
    fun `unknown references and duplicate definitions are gaps`() {
        val scan = ScanResult(
            definitions = listOf(requirement("SW-HMI-001", "b.md", 9), requirement("SW-HMI-001", "a.md", 3)),
            references = listOf(
                reference("SW-HMI-001", CODE, "a.kt"),
                reference("SW-HMI-001", TEST, "ATest.kt"),
                reference("SW-HMI-777", TEST, "xTest.kt", 2),
                reference("SW-HMI-777", CODE, "x.kt", 4),
            ),
            filesScanned = 5,
        )

        val report = TraceAnalyzer.analyze(scan)

        assertEquals(Location("a.md", 3), report.rows.single().requirement.location)
        assertEquals(
            mapOf("SW-HMI-777" to listOf(Location("x.kt", 4), Location("xTest.kt", 2))),
            report.unknownReferences,
        )
        assertEquals(
            mapOf("SW-HMI-001" to listOf(Location("a.md", 3), Location("b.md", 9))),
            report.duplicateDefinitions,
        )
        assertEquals(2, report.gapCount)
    }
}
