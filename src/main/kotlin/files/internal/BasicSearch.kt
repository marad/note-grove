package files.internal

import files.FilesFacade
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

class BasicSearch : FilesFacade {
    override fun search(pattern: String, path: String): List<String> {
        return Files.list(Paths.get(path)).filter {
            it.name.endsWith(".md", ignoreCase = true) && (pattern.isEmpty() || matches(it.name, pattern))
        }.map {
            it.nameWithoutExtension
        }.toList().sorted()
    }

    private fun matches(entry: String, pattern: String): Boolean {
        var current = 0
        val patternlc = pattern.lowercase()
        entry.lowercase().forEach { char ->
            val patternChar = patternlc[current]
            if (char == patternChar) {
                current += 1
                if (current >= patternlc.length) {
                    return true
                }
            }
        }
        return false
    }

}