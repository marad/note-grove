package editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.sp
import com.vladsch.flexmark.ast.*
import com.vladsch.flexmark.ext.wikilink.WikiLink
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterBlock
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.ast.NodeVisitor
import com.vladsch.flexmark.util.ast.VisitHandler
import kotlin.math.max

private val grayedOut = SpanStyle(color = Color.LightGray)


fun Document.highlighted(): AnnotatedString {
    val ann = AnnotatedString.Builder()
    var lastOffset = 0
    fun appendUntil(until: Int) {
        val endIndex = until.coerceAtMost(document.chars.length)
        ann.append(document.chars.substring(lastOffset, endIndex))
        lastOffset = endIndex
    }

    val visitor = NodeVisitor()

    visitor.addHandlers(listOf(
        VisitHandler(YamlFrontMatterBlock::class.java) {
            appendUntil(it.startOffset)
            ann.pushStyle(SpanStyle(fontSize = 12.sp))
            ann.pushStyle(grayedOut)
            appendUntil(it.chars.endOffset)
            ann.pop()
            ann.pop()
        },
        VisitHandler(Emphasis::class.java) {
            appendUntil(it.startOffset)
            ann.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
            visitor.visitChildren(it)
            appendUntil(it.endOffset)
            ann.pop()
        },
        VisitHandler(StrongEmphasis::class.java) {
            appendUntil(it.startOffset)
            ann.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            visitor.visitChildren(it)
            appendUntil(it.endOffset)
            ann.pop()
        },
        VisitHandler(Heading::class.java) {
            appendUntil(it.startOffset)
            ann.pushStyle(SpanStyle(fontSize = (22 - 2 * it.level).coerceAtLeast(16).sp))
            visitor.visitChildren(it)
            appendUntil(it.endOffset+1)
            ann.pop()
        },
        VisitHandler(FencedCodeBlock::class.java) {
            appendUntil(it.startOffset)
            ann.pushStyle(grayedOut)
            appendUntil(max(it.info.endOffset, it.openingMarker.endOffset))
            ann.pop()

            ann.pushStyle(SpanStyle(color = Color.Gray))
            visitor.visitChildren(it)
            ann.pop()

            ann.pushStyle(grayedOut)
            appendUntil(it.closingMarker.endOffset)
            ann.pop()
        },
        VisitHandler(Code::class.java) {
            appendUntil(it.startOffset)
            ann.pushStyle(SpanStyle(color = Color.Gray))
            visitor.visitChildren(it)
            appendUntil(it.endOffset)
            ann.pop()
        },
        VisitHandler(LinkRef::class.java) {
            appendUntil(it.startOffset)

            ann.pushStyle(SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 12.sp))
            ann.pushStyle(grayedOut)
            appendUntil(max(it.childChars.startOffset, it.referenceOpeningMarker.endOffset))
            ann.pop()

            ann.pushStyle(SpanStyle(color = Color.Blue))
            visitor.visitChildren(it)
            ann.pop()

            ann.pushStyle(grayedOut)
            appendUntil(it.endOffset)
            ann.pop()
            ann.pop()
        },
        VisitHandler(Link::class.java) {
            appendUntil(it.startOffset)
            ann.pushStyle(grayedOut)
            appendUntil(it.textOpeningMarker.endOffset)
            ann.pop()

            ann.pushStyle(SpanStyle(color = Color.Blue))
            appendUntil(max(it.childChars.endOffset, it.textClosingMarker.startOffset))
            ann.pop()

            ann.pushStyle(grayedOut)
            appendUntil(it.endOffset)
            ann.pop()
        },
        VisitHandler(WikiLink::class.java) {
            appendUntil(it.startOffset)

            ann.pushStyle(grayedOut)
            ann.append(it.openingMarker)
            ann.pop()
            lastOffset += it.openingMarker.length

            ann.pushStyle(SpanStyle(color = Color.Blue))
            visitor.visitChildren(it)
            ann.pop()

            ann.pushStyle(grayedOut)
            ann.append(it.closingMarker)
            ann.pop()
            lastOffset = it.endOffset
        },
        VisitHandler(Text::class.java) {
            appendUntil(it.endOffset)
        }
    ))


    visitor.visit(document)

    appendUntil(document.endOffset)
    return ann.toAnnotatedString()
}
