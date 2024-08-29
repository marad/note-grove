package files

import files.internal.BasicSearch
import files.internal.MatchingStrategy

interface FilesFacade {
    fun search(pattern: String, path: String, matches: (entry: String, pattern: String) -> Boolean = MatchingStrategy::fuzzy): List<String>

    companion object {
        fun create() =
            BasicSearch()

    }
}