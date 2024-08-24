package files

import files.internal.BasicSearch

interface FilesFacade {
    fun search(pattern: String, path: String): List<String>

    companion object {
        fun create() =
            BasicSearch()

    }
}