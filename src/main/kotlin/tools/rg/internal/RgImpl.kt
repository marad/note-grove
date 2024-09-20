package tools.rg.internal

import com.google.gson.GsonBuilder
import tools.rg.Entry
import tools.rg.RgFacade
import java.io.BufferedReader
import java.io.InputStreamReader

class RgImpl(private val rgBinaryPath: String = "rg") : RgFacade {
    private val gson = GsonBuilder()
        .registerTypeAdapter(Entry::class.java, EntryDeserializer())
        .create()

    override fun search(pattern: String, path: String): List<Entry> {
        val process = ProcessBuilder(rgBinaryPath, "-i", "--json", pattern, path)
            .start()

        val reader = BufferedReader(InputStreamReader(process.inputStream))
        var line: String?
        val result = mutableListOf<Entry>()
        while (reader.readLine().also { line = it } != null) {
            val entry = gson.fromJson(line, Entry::class.java)
            result.add(entry)
        }
        return result
    }
}