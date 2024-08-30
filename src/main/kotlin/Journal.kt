import java.nio.file.Path
import java.time.LocalDate
import kotlin.io.path.exists

object Journal {
    val filePattern = Regex("""daily\.journal\.(\d{4})\.(\d{1,2})\.(\d{1,2})""")

    fun getJournalDate(path: String): LocalDate? {
        val match = filePattern.find(path) ?: return null
        val (year, month, day) = match.destructured
        return LocalDate.of(year.toInt(), month.toInt(), day.toInt())
    }

    fun formatJournalNoteName(date: LocalDate): String =
        "daily.journal.${date.year}.${date.monthValue.toString().padStart(2, '0')}.${date.dayOfMonth.toString().padStart(2, '0')}"

    fun todaysDailyNote(): String = formatJournalNoteName(LocalDate.now())

    fun previousDailyNote(root: Root, date: LocalDate, maxDaysBack: Int = 60): Path? {
        var maxDaysBack = maxDaysBack
        var currentDate = date
        while (maxDaysBack-- > 0) {
            currentDate = currentDate.minusDays(1)
            val path = root.pathToFile(formatJournalNoteName(currentDate))
            if (path.exists()) {
                return path
            }
        }
        return null
    }

    fun nextDailyNote(root: Root, date: LocalDate, maxDaysForward: Int = 60): Path? {
        var maxDaysForward = maxDaysForward
        var currentDate = date
        while (maxDaysForward-- > 0) {
            currentDate = currentDate.plusDays(1)
            val path = root.pathToFile(formatJournalNoteName(currentDate))
            if (path.exists()) {
                return path
            }
        }
        return null
    }
}