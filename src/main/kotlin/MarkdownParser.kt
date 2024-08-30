import com.vladsch.flexmark.ext.wikilink.WikiLink
import com.vladsch.flexmark.ext.wikilink.WikiLinkExtension
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterNode
import com.vladsch.flexmark.formatter.Formatter
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.ast.NodeVisitor
import com.vladsch.flexmark.util.ast.VisitHandler
import com.vladsch.flexmark.util.data.MutableDataSet
import com.vladsch.flexmark.util.sequence.BasedSequence

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
        val builder = StringBuilder()
        var lastOffset = 0
        fun appendUntil(until: Int) {
            val endIndex = until.coerceIn(lastOffset, document.chars.length)
            builder.append(document.chars.substring(lastOffset, endIndex))
            lastOffset = endIndex
        }

        val queue = mutableListOf<Node>(document)
        while (queue.isNotEmpty()) {
            val node = queue.removeAt(0)
            if (node.hasChildren()) {
                node.reversedChildren.forEach {
                    queue.add(0, it)
                }
            } else {
                appendUntil(node.startOffset)
                builder.append(node.chars)
                lastOffset = node.endOffset
            }
            println(builder.toString())
        }

        appendUntil(document.endOffset)
        return builder.toString()
    }

    fun updateYamlFrontmatterVisitHandler(key: String, value: String): VisitHandler<YamlFrontMatterNode> {
        return VisitHandler(YamlFrontMatterNode::class.java) { node ->
            if (node.key == key) {
                val range = node.children.first().sourceRange
                node.children.first().chars = BasedSequence.of(value)
                node.document.chars = node.document.chars.replace(range.start, range.end, value)
            }
        }
    }

    fun findLink(content: String, cursor: Int): String {
        val document = parse(content)
        var result = ""
        val visitor = NodeVisitor(
            VisitHandler(WikiLink::class.java) {
                val link = it.link
                if (link.startOffset <= cursor && link.endOffset >= cursor) {
                    result = link.substring(0)
                }
            }
        )
        visitor.visit(document)
        return result
    }
}
