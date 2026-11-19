package dev.namdev.aspicetrace.scan

import dev.namdev.aspicetrace.model.ArtifactKind
import java.nio.file.Path

/**
 * Decides which work product a file is from its path alone: Markdown is a spec, source files under a test
 * directory (or named like a test) are verification, other source files are implementation.
 * Everything else (images, binaries, build files) returns null and is not read.
 */
// @Requirement TRC-SCAN-002
object FileClassifier {

    private val specExtensions = setOf("md", "markdown")

    private val sourceExtensions = setOf(
        "kt", "kts", "java", "scala", "groovy",
        "c", "h", "cc", "cpp", "cxx", "hpp", "hh", "aidl",
        "py", "js", "jsx", "ts", "tsx", "swift", "go", "rs", "cs", "sh",
    )

    private val testDirectories = setOf("test", "tests", "androidTest", "testFixtures", "integrationTest", "__tests__")

    /** Matched against the file name without its last extension: FooTest, FooTests, foo_test, test_foo, foo.spec. */
    private val testFileName = Regex("""^test_.+|.+_test|.+Tests?|.+\.(test|spec)""")

    fun classify(relativePath: Path): ArtifactKind? {
        val name = relativePath.fileName?.toString() ?: return null
        val extension = name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        return when {
            extension in specExtensions -> ArtifactKind.SPEC
            extension !in sourceExtensions -> null
            isTest(relativePath, name.substringBeforeLast('.')) -> ArtifactKind.TEST
            else -> ArtifactKind.CODE
        }
    }

    private fun isTest(relativePath: Path, baseName: String): Boolean =
        testFileName.matches(baseName) ||
            relativePath.parent?.any { it.toString() in testDirectories } == true
}
