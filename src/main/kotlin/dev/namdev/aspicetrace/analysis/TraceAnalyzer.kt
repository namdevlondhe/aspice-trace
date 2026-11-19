package dev.namdev.aspicetrace.analysis

import dev.namdev.aspicetrace.model.ArtifactKind
import dev.namdev.aspicetrace.model.Location
import dev.namdev.aspicetrace.model.Reference
import dev.namdev.aspicetrace.model.Requirement
import dev.namdev.aspicetrace.model.ScanResult

enum class TraceStatus(val label: String) {
    COVERED("Covered"),
    NOT_VERIFIED("Not verified"),
    NOT_IMPLEMENTED("Not implemented"),
    UNTRACED("Not implemented, not verified"),
    ;

    val cssClass: String get() = name.lowercase().replace('_', '-')
}

/** One line of the matrix: a requirement and everything that traces back to it. */
data class TraceRow(
    val requirement: Requirement,
    val implementedBy: List<Location>,
    val verifiedBy: List<Location>,
) {
    val status: TraceStatus
        get() = when {
            implementedBy.isNotEmpty() && verifiedBy.isNotEmpty() -> TraceStatus.COVERED
            implementedBy.isNotEmpty() -> TraceStatus.NOT_VERIFIED
            verifiedBy.isNotEmpty() -> TraceStatus.NOT_IMPLEMENTED
            else -> TraceStatus.UNTRACED
        }
}

data class TraceReport(
    val rows: List<TraceRow>,
    /** IDs cited by code or tests that no spec defines: broken backward traceability. */
    val unknownReferences: Map<String, List<Location>>,
    /** IDs defined more than once across the specs. */
    val duplicateDefinitions: Map<String, List<Location>>,
    val filesScanned: Int,
) {
    val requirementCount: Int get() = rows.size
    val implementedCount: Int get() = rows.count { it.implementedBy.isNotEmpty() }
    val verifiedCount: Int get() = rows.count { it.verifiedBy.isNotEmpty() }
    val implementedPercent: Int get() = percent(implementedCount)
    val verifiedPercent: Int get() = percent(verifiedCount)

    val notImplemented: List<TraceRow> get() = rows.filter { it.implementedBy.isEmpty() }
    val notVerified: List<TraceRow> get() = rows.filter { it.verifiedBy.isEmpty() }

    /** Requirements that are not fully covered, plus unknown references and duplicate definitions. */
    val gapCount: Int
        get() = rows.count { it.status != TraceStatus.COVERED } + unknownReferences.size + duplicateDefinitions.size

    private fun percent(count: Int): Int = if (rows.isEmpty()) 0 else count * PERCENT / rows.size

    private companion object {
        const val PERCENT = 100
    }
}

/** Joins definitions and references into a bidirectional traceability matrix. Output is sorted and stable. */
// @Requirement TRC-ANA-001 TRC-ANA-002 TRC-CLI-002
object TraceAnalyzer {

    fun analyze(scan: ScanResult): TraceReport {
        val definitionsById = scan.definitions.groupBy { it.id }
        val referencesById = scan.references.groupBy { it.id }

        val rows = definitionsById.values
            .map { definitions ->
                val requirement = definitions.minBy { it.location }
                val references = referencesById[requirement.id].orEmpty()
                TraceRow(
                    requirement = requirement,
                    implementedBy = references.locationsOf(ArtifactKind.CODE),
                    verifiedBy = references.locationsOf(ArtifactKind.TEST),
                )
            }
            .sortedBy { it.requirement.id }

        val unknown = referencesById
            .filterKeys { it !in definitionsById }
            .mapValues { (_, references) -> references.map { it.location }.distinct().sorted() }
            .toSortedMap()

        val duplicates = definitionsById
            .filterValues { it.size > 1 }
            .mapValues { (_, definitions) -> definitions.map { it.location }.sorted() }
            .toSortedMap()

        return TraceReport(rows, unknown, duplicates, scan.filesScanned)
    }

    private fun List<Reference>.locationsOf(kind: ArtifactKind): List<Location> =
        filter { it.kind == kind }.map { it.location }.distinct().sorted()
}
