package dev.namdev.aspicetrace.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.clikt.parameters.types.restrictTo
import dev.namdev.aspicetrace.Version
import dev.namdev.aspicetrace.analysis.TraceAnalyzer
import dev.namdev.aspicetrace.analysis.TraceReport
import dev.namdev.aspicetrace.analysis.TraceStatus
import dev.namdev.aspicetrace.report.CsvReport
import dev.namdev.aspicetrace.report.GapReport
import dev.namdev.aspicetrace.report.HtmlReport
import dev.namdev.aspicetrace.report.Linker
import dev.namdev.aspicetrace.scan.ScanConfig
import dev.namdev.aspicetrace.scan.WorkspaceScanner
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.name
import kotlin.io.path.writeText

/** Default ID shape: three dash-separated parts ending in a number, e.g. SW-HMI-001 or SYS-ADAS-0042. */
const val DEFAULT_ID_PATTERN = """\b[A-Z][A-Z0-9]*-[A-Z][A-Z0-9]*-\d{3,}\b"""

// @Requirement TRC-CLI-001
class AspiceTraceCommand : CliktCommand(
    name = "aspice-trace",
    help = """
        Builds a requirement -> code -> test traceability matrix (HTML, CSV) and a coverage-gap report (Markdown).

        Requirements are defined in Markdown specs, as a heading or the first cell of a table row starting with
        the ID. Code and tests cite them on a line with the trace tag, e.g. @Requirement("SW-HMI-001").
    """.trimIndent(),
    epilog = "Exit status: 0 on success, 1 when a quality gate (--fail-on-gaps, --min-verified) fails.",
) {
    private val root by argument(help = "Directory to scan (default: current directory)")
        .path(mustExist = true, canBeFile = false)
        .default(Path("."))

    private val out by option("-o", "--out", help = "Report directory")
        .path(canBeFile = false)
        .default(Path("trace-report"))

    private val pattern by option("-p", "--pattern", help = "Regular expression for requirement IDs")
        .convert("REGEX") { runCatching { Regex(it) }.getOrElse { fail("not a valid regular expression: $it") } }
        .default(Regex(DEFAULT_ID_PATTERN), defaultForHelp = DEFAULT_ID_PATTERN)

    private val excludes by option("-x", "--exclude", help = "Glob relative to the root to skip (repeatable)")
        .multiple()

    private val tag by option("--tag", help = "Marker that introduces IDs in code and tests").default("@Requirement")

    private val linkBase by option(
        "--link-base",
        help = "Repository URL prefix for source links, e.g. https://github.com/user/repo/blob/main/",
    )

    private val failOnGaps by option("--fail-on-gaps", help = "Exit with status 1 when any gap is found").flag()

    private val minVerified by option("--min-verified", help = "Minimum % of requirements verified by tests")
        .int()
        .restrictTo(0..PERCENT)

    override fun run() {
        val outDir = out.absolute().normalize()
        val scanConfig = ScanConfig(idPattern = pattern, excludes = excludes, ignoredPaths = setOf(outDir), tag = tag)
        val report = TraceAnalyzer.analyze(WorkspaceScanner(scanConfig).scan(root))
        val projectName = root.absolute().normalize().name.ifEmpty { "workspace" }

        if (report.requirementCount == 0) {
            echo(
                "No requirements found under $root. Define them in Markdown as a heading or table row " +
                    "that starts with an ID matching ${pattern.pattern}.",
                err = true,
            )
            throw ProgramResult(1)
        }

        writeReports(report, projectName, outDir)
        printSummary(report, outDir)
        enforceGates(report)
    }

    private fun writeReports(report: TraceReport, projectName: String, outDir: Path) {
        Files.createDirectories(outDir)
        val linker = linkBase?.let(Linker::repository) ?: Linker.relative(outDir, root)
        outDir.resolve("traceability.html").writeText(HtmlReport(projectName, linker).render(report))
        outDir.resolve("traceability.csv").writeText(CsvReport.render(report))
        outDir.resolve("gaps.md").writeText(GapReport.render(report, projectName))
    }

    private fun printSummary(report: TraceReport, outDir: Path) {
        val total = report.requirementCount
        echo("aspice-trace ${Version.NAME}: scanned ${report.filesScanned} files")
        echo("  Requirements  $total")
        echo("  Implemented   ${report.implementedCount}/$total (${report.implementedPercent}%)")
        echo("  Verified      ${report.verifiedCount}/$total (${report.verifiedPercent}%)")
        echo("  Gaps          ${report.gapCount}")
        report.rows.filter { it.status != TraceStatus.COVERED }.forEach {
            echo("    - ${it.requirement.id}: ${it.status.label.lowercase()}")
        }
        report.unknownReferences.forEach { (id, where) -> echo("    - $id: unknown reference at ${where.first()}") }
        report.duplicateDefinitions.forEach { (id, where) -> echo("    - $id: defined ${where.size} times") }
        echo("Report: ${outDir.resolve("traceability.html")}")
    }

    private fun enforceGates(report: TraceReport) {
        val failures = buildList {
            if (failOnGaps && report.gapCount > 0) add("${report.gapCount} traceability gap(s)")
            minVerified?.let { min ->
                if (report.verifiedPercent < min) add("${report.verifiedPercent}% verified, below the required $min%")
            }
        }
        if (failures.isNotEmpty()) {
            echo("FAILED: ${failures.joinToString("; ")}", err = true)
            throw ProgramResult(1)
        }
    }

    private companion object {
        const val PERCENT = 100
    }
}
