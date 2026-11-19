package dev.namdev.aspicetrace

import dev.namdev.aspicetrace.analysis.TraceAnalyzer
import dev.namdev.aspicetrace.analysis.TraceStatus.COVERED
import dev.namdev.aspicetrace.analysis.TraceStatus.NOT_IMPLEMENTED
import dev.namdev.aspicetrace.analysis.TraceStatus.NOT_VERIFIED
import dev.namdev.aspicetrace.analysis.TraceStatus.UNTRACED
import dev.namdev.aspicetrace.cli.DEFAULT_ID_PATTERN
import dev.namdev.aspicetrace.report.CsvReport
import dev.namdev.aspicetrace.report.GapReport
import dev.namdev.aspicetrace.report.HtmlReport
import dev.namdev.aspicetrace.report.Linker
import dev.namdev.aspicetrace.scan.ScanConfig
import dev.namdev.aspicetrace.scan.WorkspaceScanner
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Path
import kotlin.io.path.readText

/** Keeps samples/hmi-demo, its README and the committed docs/sample-report honest. */
class SampleProjectTest {

    private val sample = Path.of("samples/hmi-demo")
    private val report = TraceAnalyzer.analyze(WorkspaceScanner(ScanConfig(Regex(DEFAULT_ID_PATTERN))).scan(sample))

    // @Requirement TRC-ANA-001 TRC-ANA-002
    @Test
    fun `sample project has exactly the gaps its README describes`() {
        assertEquals(
            mapOf(
                "SW-HMI-001" to COVERED,
                "SW-HMI-002" to COVERED,
                "SW-HMI-003" to COVERED,
                "SW-HMI-004" to COVERED,
                "SW-HMI-005" to NOT_IMPLEMENTED,
                "SW-HMI-006" to NOT_VERIFIED,
                "SW-HMI-007" to NOT_VERIFIED,
                "SW-HMI-008" to UNTRACED,
            ),
            report.rows.associate { it.requirement.id to it.status },
        )
        assertEquals(setOf("SW-HMI-099"), report.unknownReferences.keys)
        assertEquals(emptyMap<String, Any>(), report.duplicateDefinitions)
        assertEquals(5, report.gapCount)
    }

    // @Requirement TRC-CLI-002
    @Test
    fun `committed sample report is up to date`() {
        val committed = Path.of("docs/sample-report")
        val linker = Linker.repository("https://github.com/namdevlondhe/aspice-trace/blob/main/samples/hmi-demo/")
        val expected = mapOf(
            "traceability.html" to HtmlReport("hmi-demo", linker).render(report),
            "traceability.csv" to CsvReport.render(report),
            "gaps.md" to GapReport.render(report, "hmi-demo"),
        )

        expected.forEach { (name, content) ->
            assertEquals("$name is stale: run ./gradlew sampleReport", content, committed.resolve(name).readText())
        }
    }
}
