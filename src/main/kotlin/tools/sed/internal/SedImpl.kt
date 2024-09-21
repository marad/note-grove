package tools.sed.internal

import tools.sed.SedFacade
import java.io.BufferedReader
import java.io.InputStreamReader

class SedImpl(private val sedBinaryPath: String = "sed") : SedFacade {
    override fun replace(pattern: String, replacement: String, path: String, global: Boolean) {
        val flags = if (global) "g" else ""
        val process = ProcessBuilder(
            sedBinaryPath, "-i", "", "-E", "s/$pattern/$replacement/$flags", path
        ).start()
        val status = process.waitFor()

        if (status != 0) {
            val sb = StringBuilder()
            val reader = BufferedReader(InputStreamReader(process.errorStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.appendLine(line)
            }
            throw RuntimeException("Failed to replace $pattern to $replacement in $path: $sb")
        }
    }
}