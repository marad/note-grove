package editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.vladsch.flexmark.ast.*
import com.vladsch.flexmark.ext.wikilink.WikiLink
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterBlock
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.ast.NodeVisitor
import com.vladsch.flexmark.util.ast.VisitHandler

private val grayedOut = SpanStyle(color = Color.LightGray)
fun Document.toAnnotatedString(): AnnotatedString {
    val ann = AnnotatedString.Builder()
    var lastOffset = 0
    fun appendPrevious(startOffset: Int) {
        ann.append(document.chars.substring(lastOffset, startOffset))
    }


    val visitor = NodeVisitor()
    visitor.addHandlers(listOf(
        VisitHandler(YamlFrontMatterBlock::class.java) {
            appendPrevious(it.startOffset)
            ann.pushStyle(grayedOut)
            ann.append(it.chars)
            ann.pop()
            lastOffset = it.endOffset
        },
        VisitHandler(Emphasis::class.java) {
            appendPrevious(it.startOffset)
            ann.pushStyle(grayedOut)
            ann.append(it.openingMarker)
            ann.pop()
            ann.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
            ann.append(it.childChars)
            ann.pop()
            ann.pushStyle(grayedOut)
            ann.append(it.closingMarker)
            ann.pop()
            lastOffset = it.endOffset
        },
        VisitHandler(StrongEmphasis::class.java) {
            appendPrevious(it.startOffset)
            ann.pushStyle(grayedOut)
            ann.append(it.openingMarker)
            ann.pop()
            ann.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            ann.append(it.childChars)
            ann.pop()
            ann.pushStyle(grayedOut)
            ann.append(it.closingMarker)
            ann.pop()
            lastOffset = it.endOffset
        },
        VisitHandler(Heading::class.java) {
            appendPrevious(it.startOffset)
            ann.pushStyle(SpanStyle(fontSize = (22 - 2 * it.level).coerceAtLeast(16).sp))
            if (it.openingMarker.isNotBlank()) {
                ann.pushStyle(grayedOut)
                ann.append(it.openingMarker)
                ann.append(" ")
                ann.pop()
            }
            ann.append(it.childChars)
            if (it.closingMarker.isNotBlank()) {
                ann.pushStyle(grayedOut)
                ann.append("\n")
                ann.append(it.closingMarker)
                ann.pop()
            }
            ann.pop()
            lastOffset = it.endOffset
        },
        VisitHandler(FencedCodeBlock::class.java) {
            appendPrevious(it.startOffset)
            ann.pushStyle(grayedOut)
            ann.append(it.openingMarker)
            ann.append(it.info)
            ann.pop()

            ann.pushStyle(SpanStyle(color = Color.Gray))
            ann.append("\n")
            ann.append(it.childChars)
            ann.pop()

            ann.pushStyle(grayedOut)
            ann.append(it.closingMarker)
            ann.pop()
            lastOffset = it.endOffset
        },
        VisitHandler(Code::class.java) {
            appendPrevious(it.startOffset)
            ann.pushStyle(grayedOut)
            ann.append(it.openingMarker)
            ann.pop()

            ann.pushStyle(SpanStyle(color = Color.Gray))
            ann.append(it.childChars)
            ann.pop()

            ann.pushStyle(grayedOut)
            ann.append(it.closingMarker)
            ann.pop()

            lastOffset = it.endOffset
        },
        VisitHandler(LinkRef::class.java) {
            appendPrevious(it.startOffset)
            ann.pushStyle(grayedOut)
            ann.append(it.baseSequence.substring(it.startOffset, it.firstChild?.startOffset ?: it.startOffset))
            ann.pop()

            ann.pushStyle(SpanStyle(color = Color.Blue))
            //it.children.forEach { ann.append(it, false) }
            ann.append(it.childChars)
            ann.pop()

            ann.pushStyle(grayedOut)
            ann.append(it.baseSequence.substring(it.lastChild?.endOffset ?: it.endOffset, it.endOffset))
            ann.pop()

            lastOffset = it.endOffset
        },
        VisitHandler(Link::class.java) {
            appendPrevious(it.startOffset)
            ann.pushStyle(grayedOut)
            ann.append(it.baseSequence.substring(it.startOffset, it.firstChild?.startOffset ?: it.startOffset))
            ann.pop()

            ann.pushStyle(SpanStyle(color = Color.Blue))
            //it.children.forEach { ann.append(it, false) }
            ann.append(it.childChars)
            ann.pop()

            ann.pushStyle(grayedOut)
            ann.append(it.baseSequence.substring(it.lastChild?.endOffset ?: it.endOffset, it.endOffset))
            ann.pop()

            lastOffset = it.endOffset
        },
        VisitHandler(WikiLink::class.java) {
            appendPrevious(it.startOffset)
            ann.pushStyle(grayedOut)
            ann.append(it.baseSequence.substring(it.startOffset, it.firstChild?.startOffset ?: it.startOffset))
            ann.pop()

            ann.pushStyle(SpanStyle(color = Color.Blue))
            ann.append(it.childChars)
            ann.pop()

            ann.pushStyle(grayedOut)
            ann.append(it.baseSequence.substring(it.lastChild?.endOffset ?: it.endOffset, it.endOffset))
            ann.pop()

            lastOffset = it.endOffset
        }
    ))

    visitor.visit(document)

    ann.append(document.chars.substring(lastOffset, document.endOffset))
    return ann.toAnnotatedString()
}