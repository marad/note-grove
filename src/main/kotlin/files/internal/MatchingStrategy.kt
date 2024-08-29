package files.internal

object MatchingStrategy {
    fun regex(entry: String, pattern: String): Boolean {
        return entry.matches(Regex(pattern))
    }

    fun contains(entry: String, pattern: String): Boolean {
        return entry.contains(pattern)
    }

    fun fuzzy(entry: String, pattern: String): Boolean {
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