package dev.namdev.aspicetrace.model

/** A position in the scanned tree. [path] is relative to the scan root and always uses '/'. */
data class Location(val path: String, val line: Int) : Comparable<Location> {
    override fun compareTo(other: Location): Int = compareValuesBy(this, other, { it.path }, { it.line })

    override fun toString(): String = "$path:$line"
}

/** The ASPICE work product a file belongs to. */
enum class ArtifactKind(val label: String) {
    SPEC("Specification (SWE.1)"),
    CODE("Implementation (SWE.3)"),
    TEST("Verification (SWE.4–SWE.6)"),
}

/** A requirement as defined in a Markdown spec. */
data class Requirement(val id: String, val title: String, val location: Location)

/** A requirement ID cited by code or a test through the trace tag. */
data class Reference(val id: String, val kind: ArtifactKind, val location: Location)

data class ScanResult(
    val definitions: List<Requirement>,
    val references: List<Reference>,
    val filesScanned: Int,
    val filesSkipped: List<String> = emptyList(),
)
