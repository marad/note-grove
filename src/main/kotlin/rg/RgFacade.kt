package gh.marad.grove.rg

import gh.marad.grove.rg.internal.RgImpl

interface RgFacade {
    fun search(pattern: String, path: String): List<Entry>

    companion object {
        fun create(rgBinaryPath: String = "rg"): RgFacade = RgImpl(rgBinaryPath)
    }
}