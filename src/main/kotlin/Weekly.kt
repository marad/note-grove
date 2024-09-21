import java.nio.file.Path
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.io.path.exists

object Weekly {
    val namePattern = Regex("""weekly\.(\d{4})\.(\d{2})""")

    fun getWeek(date: LocalDate = LocalDate.now()): Int {
        return date.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear())
    }

    fun getWeekAndYear(name: String): Pair<Int, Int>? {
        val match = namePattern.find(name) ?: return null
        val (year, week) = match.destructured
        return week.toInt() to year.toInt()
    }

    fun getFirstDayOfWeek(week: Int, year: Int = LocalDate.now().year): LocalDate {
        return LocalDate.of(year, 1, 1).with(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear(), week.toLong())
    }

    fun getLastDayOfWeek(week: Int, year: Int = LocalDate.now().year): LocalDate {
        return getFirstDayOfWeek(week, year).plusDays(6)
    }

    fun formatWeeklyNoteName(week: Int, year: Int): NoteName {
        return NoteName("weekly.${year}.${week.toString().padStart(2, '0')}")
    }

    fun getCurrentWeeklyNote(): NoteName {
        val currentWeek = getWeek()
        return formatWeeklyNoteName(currentWeek, LocalDate.now().year)
    }

    fun getPreviousWeeklyNote(root: Root, week: Int, year: Int, maxWeeksBack: Int = 10): NoteName? {
        var currentDate = getFirstDayOfWeek(week, year)
        var maxWeeksBack = maxWeeksBack
        while (maxWeeksBack-- > 0) {
            currentDate = currentDate.minusWeeks(1)
            val week = getWeek(currentDate)
            val noteName = formatWeeklyNoteName(week, currentDate.year)
            val path = root.pathToFile(noteName)
            if (path.exists()) {
                return noteName
            }
        }
        return null
    }

    fun getNextWeeklyNote(root: Root, week: Int, year: Int, maxWeeksForward: Int = 10): NoteName? {
        var currentDate = getFirstDayOfWeek(week, year)
        var maxWeeksForward = maxWeeksForward
        while (maxWeeksForward-- > 0) {
            currentDate = currentDate.plusWeeks(1)
            val week = getWeek(currentDate)
            val noteName = formatWeeklyNoteName(week, currentDate.year)
            val path = root.pathToFile(noteName)
            if (path.exists()) {
                return noteName
            }
        }
        return null
    }
}
