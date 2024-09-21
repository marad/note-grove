package tools.sed

import tools.sed.internal.SedImpl

interface SedFacade {
    fun replace(pattern: String, replacement: String, path: String, global: Boolean = true)

    companion object {
        fun create(): SedFacade = SedImpl()
    }
}
