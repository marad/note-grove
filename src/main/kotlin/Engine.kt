import gh.marad.grove.files.FilesFacade
import gh.marad.grove.rg.Entry
import gh.marad.grove.rg.RgFacade
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.nameWithoutExtension

fun main() {
    val path = "/home/marad/dendron/notes/"
    val vault = Vault(path)

//    vault.searchFiles("gcp").forEach(::println)
//    vault.search("sql").forEach(::println)

//    vault.listHierarchy("gtd").forEach(::println)
    val h = vault.getHierarchy()
}


class Vault(private val path: String) {
    private val files = FilesFacade.create()
    private val rg = RgFacade.create()

    fun getHierarchy(): Hierarchy = Hierarchy(path)

    fun pathToFile(file: String): Path = Paths.get(path, "$file.md")

    fun searchFiles(pattern: String): List<String> =
        files.search(pattern, path)

    fun search(pattern: String): List<Entry> =
        rg.search(pattern, path)

}

class Hierarchy(path: String) {
    private val root = Node("Vault", "")

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

fun String.capitalizeWords() = split(" ").map { word ->
    word.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
}.joinToString(" ")
