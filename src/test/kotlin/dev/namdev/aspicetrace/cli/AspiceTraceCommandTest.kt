package dev.namdev.aspicetrace.cli

import com.github.ajalt.clikt.testing.test
import dev.namdev.aspicetrace.WorkspaceTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

class AspiceTraceCommandTest : WorkspaceTest() {

    /** One requirement covered, one implemented but not verified. */
    private fun project(): Path = workspace(
        "docs/spec.md" to "## SW-HMI-001 Covered\n## SW-HMI-002 Not verified\n",
        "src/main/kotlin/A.kt" to "@Requirement(\"SW-HMI-001\", \"SW-HMI-002\")\nclass A\n",
        "src/test/kotlin/ATest.kt" to "@Requirement(\"SW-HMI-001\")\nclass ATest\n",
    )

    private fun run(vararg args: String) = AspiceTraceCommand().test(args.toList())

    // @Requirement TRC-CLI-001
    @Test
    fun `writes every report and succeeds without a gate`() {
        val root = project()
        val out = root.resolve("trace-report")

        val result = run(root.toString(), "--out", out.toString())

        assertEquals(result.output, 0, result.statusCode)
        assertTrue(out.resolve("traceability.html").exists())
        assertTrue(out.resolve("traceability.csv").exists())
        assertTrue(out.resolve("gaps.md").exists())
        assertTrue(result.stdout.contains("Verified      1/2 (50%)"))
        assertTrue(result.stdout.contains("SW-HMI-002: not verified"))
    }

    // @Requirement TRC-CLI-001
    @Test
    fun `fail-on-gaps exits with status 1`() {
        val root = project()

        val result = run(root.toString(), "--out", root.resolve("trace-report").toString(), "--fail-on-gaps")

        assertEquals(1, result.statusCode)
        assertTrue(result.stderr.contains("1 traceability gap(s)"))
    }

    // @Requirement TRC-CLI-001
    @Test
    fun `min-verified gates on the verified percentage`() {
        val root = project()
        val out = root.resolve("trace-report").toString()

        assertEquals(0, run(root.toString(), "--out", out, "--min-verified", "50").statusCode)
        assertEquals(1, run(root.toString(), "--out", out, "--min-verified", "60").statusCode)
    }

    @Test
    fun `finding no requirements is an error`() {
        val root = workspace("README.md" to "# Nothing to trace here\n")

        val result = run(root.toString(), "--out", root.resolve("trace-report").toString())

        assertEquals(1, result.statusCode)
        assertTrue(result.stderr.contains("No requirements found"))
    }

    @Test
    fun `an invalid pattern is rejected`() {
        val root = project()

        val result = run(root.toString(), "--pattern", "[unclosed")

        assertNotEquals(0, result.statusCode)
        assertTrue(result.stderr.contains("not a valid regular expression"))
    }

    // @Requirement TRC-CLI-002
    @Test
    fun `reports are identical across runs`() {
        val root = project()
        val out = root.resolve("trace-report")
        val names = listOf("traceability.html", "traceability.csv", "gaps.md")

        run(root.toString(), "--out", out.toString())
        val first = names.map { out.resolve(it).readText() }
        run(root.toString(), "--out", out.toString())
        val second = names.map { out.resolve(it).readText() }

        assertEquals(first, second)
    }
}
