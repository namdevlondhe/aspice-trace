# aspice-trace: requirements

The tool's own requirements. `./gradlew traceSelf` traces them against this repository and fails CI on any gap,
so every feature below is linked to the code that implements it and the tests that verify it.

## Specifications (input)

### TRC-SPEC-001: Requirements defined by Markdown headings
A heading whose text starts with a requirement ID defines that requirement; the rest of the heading is its title.

### TRC-SPEC-002: Requirements defined by Markdown table rows
A table row whose first cell is a requirement ID defines that requirement; the second cell is its title.

### TRC-SPEC-003: Examples and prose are not definitions
IDs inside fenced code blocks or in running text do not define requirements.

## Scanning

### TRC-SCAN-001: Tagged references in any language
An ID cited after the trace tag (`@Requirement` by default) on the same line of a source file is a reference.

### TRC-SCAN-002: Implementation and verification told apart by path
Source files under a test directory, or named like a test, are verification; other source files are implementation.

### TRC-SCAN-003: Exclusions
Build output, VCS and IDE folders, the report directory, user globs and previously generated reports are not scanned.

## Analysis

### TRC-ANA-001: Forward traceability gaps
Each requirement is reported as covered, not implemented, not verified, or neither.

### TRC-ANA-002: Backward traceability gaps
IDs cited by code or tests but defined in no spec, and IDs defined more than once, are reported.

## Reports and CLI

### TRC-RPT-001: HTML traceability matrix
A single self-contained HTML page shows the summary and a filterable matrix with links to the sources.

### TRC-RPT-002: Gap report and CSV
A Markdown gap report (for pull requests and CI summaries) and a CSV of the full matrix are written.

### TRC-CLI-001: Quality gates
`--fail-on-gaps` and `--min-verified` make the command exit with status 1, so CI can block a merge.

### TRC-CLI-002: Deterministic output
The same input always produces byte-identical reports (sorted, no timestamps), so reports diff cleanly.
