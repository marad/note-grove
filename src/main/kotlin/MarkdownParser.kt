import com.vladsch.flexmark.ext.wikilink.WikiLinkExtension
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterBlock
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterNode
import com.vladsch.flexmark.formatter.Formatter
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.ast.VisitHandler
import com.vladsch.flexmark.util.data.MutableDataSet

object Markdown {
    private val options = MutableDataSet().also {
        it.set(Parser.EXTENSIONS, listOf(
            WikiLinkExtension.create(),
            YamlFrontMatterExtension.create()
        ))
    }
    private val parser: Parser = createParser()
    private val renderer: Formatter = createRenderer()

    private fun createParser(): Parser {
        return Parser.builder(options).build()
    }

    private fun createRenderer(): Formatter =
        Formatter.builder(options).build()

    fun parse(text: String): Document {
        return parser.parse(text)
    }

    fun render(document: Document): String {
        return renderer.render(document)
    }

    fun updateYamlFrontmatterVisitHandler(key: String, value: String): VisitHandler<YamlFrontMatterNode> {
        return VisitHandler(YamlFrontMatterNode::class.java) { node ->
            if (node.key == key) {
                val block = node.parent as YamlFrontMatterBlock
                val range = node.children.first().sourceRange
                block.chars = block.chars.replace(range.start, range.end, value)
            }
        }
    }
}
