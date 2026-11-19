package dev.namdev.aspicetrace.scan

import dev.namdev.aspicetrace.cli.DEFAULT_ID_PATTERN
import dev.namdev.aspicetrace.model.Location
import dev.namdev.aspicetrace.model.Requirement
import org.junit.Assert.assertEquals
import org.junit.Test

class SpecParserTest {

    private val parser = SpecParser(Regex(DEFAULT_ID_PATTERN))

    private fun parse(text: String) = parser.parse("spec.md", text.trimIndent().lines())

    // @Requirement TRC-SPEC-001
    @Test
    fun `headings that start with an ID define requirements`() {
        val found = parse(
            """
            # Climate HMI
            ## SW-HMI-001: Show the set temperature
            ### **SW-HMI-002** — Fan levels ###
            #### [SW-HMI-003] Defrost
            ## SW-HMI-004
            #SW-HMI-005 is a hashtag, not a heading
            ## Overview of SW-HMI-006
            """,
        )

        assertEquals(
            listOf(
                Requirement("SW-HMI-001", "Show the set temperature", Location("spec.md", 2)),
                Requirement("SW-HMI-002", "Fan levels", Location("spec.md", 3)),
                Requirement("SW-HMI-003", "Defrost", Location("spec.md", 4)),
                Requirement("SW-HMI-004", "", Location("spec.md", 5)),
            ),
            found,
        )
    }

    // @Requirement TRC-SPEC-002
    @Test
    fun `table rows whose first cell is an ID define requirements`() {
        val found = parse(
            """
            | ID | Title | Rationale |
            |---|---|---|
            | SW-HMI-007 | Day and night theme | Glare |
            | `SW-HMI-008` | Block text entry while driving | Distraction |
            | Note | SW-HMI-009 in another column is not a definition | |
            """,
        )

        assertEquals(
            listOf(
                Requirement("SW-HMI-007", "Day and night theme", Location("spec.md", 3)),
                Requirement("SW-HMI-008", "Block text entry while driving", Location("spec.md", 4)),
            ),
            found,
        )
    }

    // @Requirement TRC-SPEC-003
    @Test
    fun `code blocks and prose never define requirements`() {
        val found = parse(
            """
            ```markdown
            ## SW-HMI-001 Example inside a code block
            ```
            ## SW-HMI-002 Real requirement
            See SW-HMI-003 for details.
            ~~~~
            | SW-HMI-004 | Inside a tilde fence |
            ```
            ## SW-HMI-005 Still inside: a backtick line cannot close a tilde fence
            ~~~~
            """,
        )

        assertEquals(listOf("SW-HMI-002"), found.map { it.id })
    }
}
