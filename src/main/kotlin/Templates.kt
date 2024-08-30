import org.stringtemplate.v4.ST
import java.nio.file.Files
import kotlin.io.path.exists

object Templates {

    fun newNote(root: Root, title: String, templateName: String): String {
        val template = loadTemplateOrDefault(root, templateName)
        return ST(template).apply {
            add("title", title)
            add("timestamp", System.currentTimeMillis())
        }.render()
    }

    fun loadTemplateOrDefault(root: Root, name: String): String {
        val path = root.pathToFile(name)
        return if (path.exists()) {
            defaultFrontMatter + "\n" + Files.readString(path).let(Markdown::removeFrontMatter)
        } else {
            defaultFrontMatter + "\n"
        }
    }


    private val defaultFrontMatter = """
        ---
        created: <timestamp>
        updated: <timestamp>
        ---
        
    """.trimIndent()
}
