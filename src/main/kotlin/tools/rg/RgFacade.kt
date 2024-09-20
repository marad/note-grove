package tools.rg

import NoteName
import Root
import tools.rg.internal.RgImpl

interface RgFacade {
    fun search(pattern: String, path: String): List<Entry>

    companion object {
        fun create(rgBinaryPath: String = "rg"): RgFacade = RgImpl(rgBinaryPath)
    }
}