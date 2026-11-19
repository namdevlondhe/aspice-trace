package dev.namdev.aspicetrace.report

import dev.namdev.aspicetrace.analysis.TraceReport
import dev.namdev.aspicetrace.analysis.TraceRow
import dev.namdev.aspicetrace.model.Location
import dev.namdev.aspicetrace.scan.WorkspaceScanner

/**
 * The coverage-gap report as Markdown: short enough to read in a pull request or a CI job summary
 * (`cat gaps.md >> $GITHUB_STEP_SUMMARY`).
 */
// @Requirement TRC-RPT-002
object GapReport {

    fun render(report: TraceReport, projectName: String): String = buildString {
        appendLine(WorkspaceScanner.GENERATED_MARKER)
        appendLine("# Traceability gaps: $projectName")
        appendLine()
        appendLine("| Requirements | Implemented | Verified | Gaps |")
        appendLine("|---:|---:|---:|---:|")
        appendLine(
            "| ${report.requirementCount} " +
                "| ${report.implementedCount} (${report.implementedPercent}%) " +
                "| ${report.verifiedCount} (${report.verifiedPercent}%) " +
                "| ${report.gapCount} |",
        )
        appendLine()
        if (report.gapCount == 0) {
            appendLine("No gaps: every requirement is implemented and verified, and every reference resolves.")
            return@buildString
        }
        requirementSection("Not implemented (SWE.3)", report.notImplemented)
        requirementSection("Not verified (SWE.4–SWE.6)", report.notVerified)
        locationSection(
            "Unknown references",
            "Cited by code or tests, but not defined in any spec.",
            "Referenced at",
            report.unknownReferences,
        )
        locationSection(
            "Duplicate definitions",
            "Defined in more than one place.",
            "Defined at",
            report.duplicateDefinitions,
        )
    }

    private fun StringBuilder.requirementSection(title: String, rows: List<TraceRow>) {
        if (rows.isEmpty()) return
        appendLine("## $title")
        appendLine()
        appendLine("| Requirement | Title | Defined at |")
        appendLine("|---|---|---|")
        rows.forEach { row ->
            appendLine("| ${row.requirement.id} | ${cell(row.requirement.title)} | `${row.requirement.location}` |")
        }
        appendLine()
    }

    private fun StringBuilder.locationSection(
        title: String,
        note: String,
        header: String,
        entries: Map<String, List<Location>>,
    ) {
        if (entries.isEmpty()) return
        appendLine("## $title")
        appendLine()
        appendLine(note)
        appendLine()
        appendLine("| Requirement | $header |")
        appendLine("|---|---|")
        entries.forEach { (id, where) -> appendLine("| $id | ${where.joinToString(", ") { "`$it`" }} |") }
        appendLine()
    }

    private fun cell(text: String): String = text.replace("|", "\\|")
}

/** The full matrix as CSV, for spreadsheets and assessment tooling. */
// @Requirement TRC-RPT-002
object CsvReport {

    fun render(report: TraceReport): String = buildString {
        appendLine("id,title,status,specification,implemented_by,verified_by")
        report.rows.forEach { row ->
            val fields = listOf(
                row.requirement.id,
                row.requirement.title,
                row.status.label,
                row.requirement.location.toString(),
                row.implementedBy.joinToString("; "),
                row.verifiedBy.joinToString("; "),
            )
            appendLine(fields.joinToString(",") { quote(it) })
        }
    }

    private fun quote(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' }) "\"${field.replace("\"", "\"\"")}\"" else field
}
