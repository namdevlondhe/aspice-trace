package dev.namdev.aspicetrace

import org.junit.After
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText

/** Base class for tests that need a throwaway source tree on disk. */
abstract class WorkspaceTest {

    private val roots = mutableListOf<Path>()

    protected fun workspace(vararg files: Pair<String, String>): Path {
        val root = createTempDirectory("aspice-trace-test").also { roots.add(it) }
        files.forEach { (path, content) ->
            val file = root.resolve(path)
            file.parent.createDirectories()
            file.writeText(content)
        }
        return root
    }

    @After
    fun deleteWorkspaces() {
        roots.forEach { it.toFile().deleteRecursively() }
    }
}
