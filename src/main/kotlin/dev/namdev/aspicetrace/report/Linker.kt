package dev.namdev.aspicetrace.report

import dev.namdev.aspicetrace.model.Location
import java.nio.file.Path
import kotlin.io.path.invariantSeparatorsPathString

/** Turns a [Location] into a link for the HTML report. */
fun interface Linker {
    fun href(location: Location): String?

    companion object {
        /**
         * Links into a web repository view, e.g. `https://github.com/user/repo/blob/main/`.
         * Markdown gets `?plain=1` so GitHub honours the line anchor instead of rendering the file.
         */
        fun repository(base: String): Linker {
            val prefix = if (base.endsWith("/")) base else "$base/"
            return Linker { location ->
                val plain = if (location.path.endsWith(".md")) "?plain=1" else ""
                "$prefix${location.path}$plain#L${location.line}"
            }
        }

        /** Relative file links, so a report opened from disk can open the sources next to it. */
        fun relative(reportDir: Path, root: Path): Linker {
            val from = reportDir.toAbsolutePath().normalize()
            val to = root.toAbsolutePath().normalize()
            return Linker { location -> from.relativize(to.resolve(location.path)).invariantSeparatorsPathString }
        }
    }
}
