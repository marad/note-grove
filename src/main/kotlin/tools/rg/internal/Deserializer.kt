package tools.rg.internal

import com.google.gson.*
import tools.rg.Begin
import tools.rg.Elapsed
import tools.rg.End
import tools.rg.Entry
import tools.rg.Match
import tools.rg.Stats
import tools.rg.Submatch
import tools.rg.Summary
import java.lang.reflect.Type

class EntryDeserializer(private val gson: Gson = Gson()) : JsonDeserializer<Entry> {
    override fun deserialize(json: JsonElement, type: Type, ctx: JsonDeserializationContext): Entry {
        val obj = json.asJsonObject
        return when(val entryType = obj.get("type").asString) {
            "begin" -> readBegin(obj.getDataField())
            "match" -> readMatch(obj.getDataField())
            "end" -> readEnd(obj.getDataField())
            "summary" -> readSummary(obj.getDataField())
            else -> throw RuntimeException("Unhandled type: $entryType")
        }
    }

    fun readBegin(obj: JsonObject): Begin {
        return Begin(obj.getPathField())
    }

    fun readMatch(obj: JsonObject): Match {
        fun JsonObject.readSubmatches(): List<Submatch> {
            val arr = getAsJsonArray("submatches")
            val result = mutableListOf<Submatch>()
            arr.forEach {
                val submatch = it.asJsonObject
                result.add(
                    Submatch(
                        submatch.getAsJsonObject("match").getTextField(),
                        submatch.get("start").asInt,
                        submatch.get("end").asInt,
                    )
                )
            }
            return result
        }
        return Match(
            obj.getPathField(),
            obj.getAsJsonObject("lines").getTextField(),
            obj.get("line_number").asInt,
            obj.get("absolute_offset").asInt,
            obj.readSubmatches()
        )
    }

    fun readEnd(obj: JsonObject): End {
        val stats = obj.getAsJsonObject("stats")
        return End(
            obj.getPathField(),
            readStats(stats)
        )
    }

    fun readSummary(obj: JsonObject): Summary {
        return Summary(
            readElapsed(obj.getAsJsonObject("elapsed_total")),
            readStats(obj.getAsJsonObject("stats"))
        )
    }

    private fun readStats(stats: JsonObject): Stats =
        Stats(
            readElapsed(stats.getAsJsonObject("elapsed")),
            stats.get("searches").asInt,
            stats.get("searches_with_match").asInt,
            stats.get("bytes_searched").asInt,
            stats.get("bytes_printed").asInt,
            stats.get("matched_lines").asInt,
            stats.get("matches").asInt,
        )

    private fun readElapsed(obj: JsonObject): Elapsed =
        Elapsed(
            obj.get("nanos").asLong,
            obj.get("secs").asInt,
            obj.get("human").asString
        )

    private fun JsonObject.getDataField(): JsonObject = getAsJsonObject("data")
    private fun JsonObject.getPathField(): String =
        this.getAsJsonObject("path").getTextField()

    private fun JsonObject.getTextField(): String = get("text").asString
}
