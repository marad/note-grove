package gh.marad.grove.rg

sealed interface Entry

data class Begin(val path: String) : Entry

data class Match(
    val path: String,
    val lines: String,
    val lineNumber: Int,
    val absoluteOffset: Int,
    val submatches: List<Submatch>
) : Entry

data class Submatch(
    val text: String,
    val start: Int,
    val end: Int,
)

data class End(val path: String, val stats: Stats) : Entry

data class Summary(
    val elapsedTotal: Elapsed,
    val stats: Stats
) : Entry

data class Stats(
    val elapsed: Elapsed,
    val searches: Int,
    val searchesWithMatch: Int,
    val bytesSearched: Int,
    val bytesPrinted: Int,
    val matchedLines: Int,
    val matches: Int
)

data class Elapsed(
    val nanos: Long,
    val secs: Int,
    val seconds: String
)

