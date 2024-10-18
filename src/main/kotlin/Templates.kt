import org.stringtemplate.v4.ST
import java.nio.file.Files
import java.time.LocalDateTime
import kotlin.io.path.exists

object Templates {

    fun newNote(root: Root, title: String, templateName: NoteName): String {
        val template = defaultFrontMatter + "\n" + loadTemplateOrDefault(root, templateName)
        return applyTemplate(template, mapOf("title" to title))
    }

    fun loadTemplate(root: Root, name: NoteName): String {
        val template = loadTemplateOrDefault(root, name)
        return applyTemplate(template, mapOf("title" to name.value))
    }

    private fun applyTemplate(templateContent: String, vars: Map<String, String>): String {
        val dt = LocalDateTime.now()
        return ST(templateContent).apply {
            vars.forEach { (k, v) -> add(k, v) }
            add("timestamp", System.currentTimeMillis())
            add("year", dt.year)
            add("month", dt.monthValue.toString().padStart(2, '0'))
            add("day", dt.dayOfMonth.toString().padStart(2, '0'))
            add("hour", dt.hour.toString().padStart(2, '0'))
            add("minute", dt.minute.toString().padStart(2, '0'))
            add("second", dt.second.toString().padStart(2, '0'))
        }.render()
    }

    private fun loadTemplateOrDefault(root: Root, name: NoteName, default: String = ""): String {
        val path = root.pathToFile(name)
        return if (path.exists()) {
            Files.readString(path).let(Markdown::removeFrontMatter)
        } else {
            default
        }
    }


    private val defaultFrontMatter = """
        ---
        created: <timestamp>
        updated: <timestamp>
        ---
        
    """.trimIndent()
}
