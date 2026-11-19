package dev.namdev.aspicetrace.scan

import dev.namdev.aspicetrace.model.ArtifactKind.CODE
import dev.namdev.aspicetrace.model.ArtifactKind.SPEC
import dev.namdev.aspicetrace.model.ArtifactKind.TEST
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.file.Path

class FileClassifierTest {

    private fun classify(path: String) = FileClassifier.classify(Path.of(path))

    // @Requirement TRC-SCAN-002
    @Test
    fun `markdown is specification`() {
        assertEquals(SPEC, classify("docs/requirements.md"))
        assertEquals(SPEC, classify("README.markdown"))
    }

    // @Requirement TRC-SCAN-002
    @Test
    fun `sources under test folders or named like tests are verification`() {
        assertEquals(TEST, classify("app/src/test/kotlin/dev/Foo.kt"))
        assertEquals(TEST, classify("app/src/androidTest/java/dev/Foo.java"))
        assertEquals(TEST, classify("lib/FooTests.swift"))
        assertEquals(TEST, classify("tools/test_parser.py"))
        assertEquals(TEST, classify("hal/fan_test.cpp"))
        assertEquals(TEST, classify("web/src/button.spec.ts"))
    }

    // @Requirement TRC-SCAN-002
    @Test
    fun `other sources are implementation and unknown files are ignored`() {
        assertEquals(CODE, classify("app/src/main/kotlin/dev/Foo.kt"))
        assertEquals(CODE, classify("src/Latest.kt"))
        assertEquals(CODE, classify("src/contest/Main.kt"))
        assertEquals(CODE, classify("hal/vhal/fan_control.c"))
        assertNull(classify("app/build.gradle"))
        assertNull(classify("docs/logo.png"))
        assertNull(classify("Makefile"))
    }
}
