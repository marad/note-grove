import files.FilesFacade
import files.internal.MatchingStrategy
import tools.rg.Begin
import tools.rg.Entry
import tools.rg.RgFacade
import tools.sed.SedFacade
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.nameWithoutExtension

fun main() {
    val path = "/home/marad/dendron/notes/"
    val root = Root("test", path)

//    vault.searchFiles("gcp").forEach(::println)
//    vault.search("sql").forEach(::println)

//    vault.listHierarchy("gtd").forEach(::println)
    val h = root.getHierarchy()
}

@JvmInline
value class NoteName(val value: String) {
    override fun toString(): String = value
}

class Root(val name: String, private val path: String) {
    private val files = FilesFacade.create()
    private val rg = RgFacade.create()
    private val sed = SedFacade.create()

    fun getHierarchy(): Hierarchy = Hierarchy(path)

    fun pathToFile(file: NoteName): Path = Paths.get(path, "$file.md")
    fun getNoteName(file: Path): NoteName = NoteName(file.nameWithoutExtension)

    fun searchFiles(pattern: String, strategy: (String,String)->Boolean = MatchingStrategy::fuzzy): List<NoteName> =
        files.search(pattern, path, strategy).map { NoteName(it) }


    fun searchInFiles(pattern: String): List<Entry> =
        rg.search(pattern, path)

    fun searchBacklinks(noteName: NoteName): List<Entry> =
        searchInFiles("\\[\\[${noteName.value}[^\\]]*\\]\\]")

    fun renameNote(oldName: NoteName, newName: NoteName): List<NoteName> {
        val updatedNotes = mutableListOf<NoteName>()
        searchBacklinks(oldName).filterIsInstance<Begin>().forEach {
            sed.replace(oldName.value, newName.value, it.path)
            updatedNotes.add(getNoteName(Paths.get(it.path)))
        }

        val oldPath = pathToFile(oldName)
        val newPath = pathToFile(newName)
        Files.move(oldPath, newPath)
        return updatedNotes
    }
}

class Hierarchy(path: String) {
    private val root = Node("Root", "")

    init {
        Files.list(Paths.get(path)).forEach {
            it.nameWithoutExtension.split(".")
                .fold(root) { node, name ->
                    node.getOrCreateChild(name)
                }
        }
        println(root.children.map { it.name })
    }

    fun get(prefix: String = ""): Node {
        if (prefix.isEmpty()) return root
        return prefix.split(".").fold(root) { node, name ->
            node.getOrCreateChild(name)}
    }

    data class Node(val name: String, val path: String, val children: MutableList<Node> = mutableListOf()) {
        fun getOrCreateChild(name: String): Node =
            children.firstOrNull { it.name == name } ?: Node(name, if(path.isNotBlank()) "$path.$name" else name).also { children.add(it) }

        override fun toString(): String = name.capitalizeWords()
    }
}

fun String.capitalizeWords() = split(" ").joinToString(" ") { word ->
    word.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
}
