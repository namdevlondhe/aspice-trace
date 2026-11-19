package dev.namdev.aspicetrace.report

import dev.namdev.aspicetrace.analysis.TraceAnalyzer
import dev.namdev.aspicetrace.model.ArtifactKind
import dev.namdev.aspicetrace.model.Location
import dev.namdev.aspicetrace.model.Reference
import dev.namdev.aspicetrace.model.Requirement
import dev.namdev.aspicetrace.model.ScanResult
import dev.namdev.aspicetrace.scan.WorkspaceScanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Path

class ReportsTest {

    private val report = TraceAnalyzer.analyze(
        ScanResult(
            definitions = listOf(
                Requirement("SW-HMI-001", "Show <b>temp</b> & \"zones\", fast", Location("docs/spec.md", 3)),
                Requirement("SW-HMI-002", "Fan | levels", Location("docs/spec.md", 7)),
            ),
            references = listOf(
                Reference("SW-HMI-001", ArtifactKind.CODE, Location("src/A.kt", 10)),
                Reference("SW-HMI-001", ArtifactKind.TEST, Location("src/test/ATest.kt", 5)),
                Reference("SW-HMI-404", ArtifactKind.CODE, Location("src/B.kt", 2)),
            ),
            filesScanned = 4,
        ),
    )

    // @Requirement TRC-RPT-001
    @Test
    fun `html escapes text and links to the repository`() {
        val html = HtmlReport("demo", Linker.repository("https://example.com/repo/blob/main")).render(report)

        assertTrue(html.contains("Show &lt;b&gt;temp&lt;/b&gt; &amp; &quot;zones&quot;, fast"))
        assertFalse(html.contains("<b>temp"))
        assertTrue(html.contains("href=\"https://example.com/repo/blob/main/src/A.kt#L10\""))
        assertTrue(html.contains("href=\"https://example.com/repo/blob/main/docs/spec.md?plain=1#L3\""))
        assertTrue(html.contains("<tr data-status=\"covered\">"))
        assertTrue(html.contains("<tr data-status=\"untraced\">"))
        assertTrue(html.contains("Unknown references"))
        assertTrue(html.contains("SW-HMI-404"))
    }

    // @Requirement TRC-RPT-001
    @Test
    fun `relative links point from the report directory to the sources`() {
        val linker = Linker.relative(Path.of("/work/repo/trace-report"), Path.of("/work/repo"))

        assertEquals("../src/A.kt", linker.href(Location("src/A.kt", 1)))
    }

    // @Requirement TRC-RPT-002
    @Test
    fun `gap report lists every gap and is marked as generated`() {
        val markdown = GapReport.render(report, "demo")

        assertTrue(markdown.startsWith(WorkspaceScanner.GENERATED_MARKER))
        assertTrue(markdown.contains("| 2 | 1 (50%) | 1 (50%) | 2 |"))
        assertTrue(markdown.contains("## Not implemented (SWE.3)"))
        assertTrue(markdown.contains("| SW-HMI-002 | Fan \\| levels | `docs/spec.md:7` |"))
        assertTrue(markdown.contains("| SW-HMI-404 | `src/B.kt:2` |"))
        assertFalse(markdown.contains("Duplicate definitions"))
    }

    // @Requirement TRC-RPT-002
    @Test
    fun `csv quotes fields that need it`() {
        val lines = CsvReport.render(report).lines()

        assertEquals("id,title,status,specification,implemented_by,verified_by", lines[0])
        assertEquals(
            "SW-HMI-001,\"Show <b>temp</b> & \"\"zones\"\", fast\",Covered," +
                "docs/spec.md:3,src/A.kt:10,src/test/ATest.kt:5",
            lines[1],
        )
        assertEquals("SW-HMI-002,Fan | levels,\"Not implemented, not verified\",docs/spec.md:7,,", lines[2])
    }
}
