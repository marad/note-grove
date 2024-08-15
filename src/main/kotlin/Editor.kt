import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vladsch.flexmark.ast.*
import com.vladsch.flexmark.ast.Paragraph
import com.vladsch.flexmark.ext.wikilink.WikiLink
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterBlock
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.ast.NodeVisitor
import com.vladsch.flexmark.util.ast.VisitHandler
import kotlin.text.substring

private val grayedOut = SpanStyle(color = Color.LightGray)
@Suppress("IMPLICIT_CAST_TO_ANY")
fun AnnotatedString.Builder.append(it: Node, shouldAppendRest: Boolean = true) {
    fun appendRest() {
        if (shouldAppendRest) {
            val sb = StringBuilder()
            it.astExtraChars(sb)
            val extra = sb.toString()
            append(
                it.document.chars.substring(
                    it.endOffset,
                    it.next?.startOffset ?: it.parent?.endOffset ?: it.endOffset
                )
            )
        }
    }
    val ignored = when(it) {
        is Document -> { it.children.forEach { append(it) } }
        is YamlFrontMatterBlock -> {
            pushStyle(grayedOut)
            append(it.chars)
            pop()
            appendRest()
        }
        is Paragraph -> {
            it.children.forEach { append(it) }
            appendRest()
        }
        is Text -> {
            append(it.chars)
            appendRest()
        }
        is Heading -> {
            pushStyle(SpanStyle(fontSize = (22 - 2*it.level).coerceAtLeast(16).sp))
            if (it.openingMarker.isNotBlank()) {
                pushStyle(grayedOut)
                append(it.openingMarker)
                append(" ")
                pop()
            }
            it.children.forEach { append(it) }
            pop()
            appendRest()
        }

        is Emphasis -> {
            pushStyle(grayedOut)
            append(it.openingMarker)
            pop()

            pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
            it.children.forEach { append(it, false) }
            pop()

            pushStyle(grayedOut)
            append(it.closingMarker)
            pop()
            appendRest()
        }

        is StrongEmphasis -> {
            pushStyle(grayedOut)
            append(it.openingMarker)
            pop()

            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            it.children.forEach { append(it, false) }
            pop()

            pushStyle(grayedOut)
            append(it.closingMarker)
            pop()
            appendRest()
        }

        is BulletList -> {
            append(it.chars)
            appendRest()
        }

        is LinkRef, is WikiLink -> {
            pushStyle(grayedOut)
            append(it.baseSequence.substring(it.startOffset, it.firstChild?.startOffset ?: it.startOffset))
            pop()

            pushStyle(SpanStyle(color = Color.Blue))
            it.children.forEach { append(it, false) }
            pop()

            pushStyle(grayedOut)
            append(it.baseSequence.substring(it.lastChild?.endOffset ?: it.endOffset, it.endOffset))
            pop()
            appendRest()
        }

        is FencedCodeBlock -> {
            pushStyle(grayedOut)
            append(it.openingMarker)
            append(it.info)
            pop()

            append(it.contentChars)
            //it.children.forEach { append(it, true) }

            pushStyle(grayedOut)
            append(it.closingMarker)
            pop()
            appendRest()
        }

        is SoftLineBreak -> {
            append(it.chars)
        }

        else -> {
            println("unhandled: $it")
        }
    }
}


class EditorState(content: String = "") {
    val dirty = mutableStateOf(false)
    val content = mutableStateOf(TextFieldValue(content))
    val focusRequester = FocusRequester()

    fun markDirty() {
        dirty.value = true
    }

    fun clearDirty() {
        dirty.value = false
    }

    fun isDirty() = dirty.value

    fun updateContent(newContent: TextFieldValue, markDirty: Boolean = true) {
        if (markDirty && content.value.text != newContent.text) {
            markDirty()
        }

        val md = Markdown.parse(newContent.text)
        val ann = AnnotatedString.Builder()

//        md.children.forEach {
//            when(it) {
//                is YamlFrontMatterBlock -> {
//                    ann.pushStyle(SpanStyle(color = Color.LightGray))
//                    ann.append(md.chars.substring(it.startOffset, it.next?.startOffset ?: it.endOffset))
//                    ann.pop()
//                    lastPosition = it.endOffset
//                }
//                else -> {
//                    ann.append(md.chars.substring(it.startOffset, it.next?.startOffset ?: md.endOffset))
//                }
//            }
//        }



//        val visitor = NodeVisitor()
//        visitor.addHandlers(arrayOf(
//            VisitHandler(YamlFrontMatterBlock::class.java) {
//                ann.pushStyle(SpanStyle(color = Color.LightGray))
//                ann.append(it.chars)
//                ann.pop()
//                ann.append(md.chars.substring(it.endOffset, it.next?.startOffset ?: it.parent?.endOffset ?: it.endOffset))
//            },
//            VisitHandler(Paragraph::class.java) {
//                visitor.visitChildren(it)
//                ann.append(md.chars.substring(it.endOffset, it.next?.startOffset ?: it.parent?.endOffset ?: it.endOffset))
//            },
//            VisitHandler(Text::class.java) {
//                ann.append(it.chars)
//                ann.append(md.chars.substring(it.endOffset, it.next?.startOffset ?: it.parent?.endOffset ?: it.endOffset))
//            },
//            VisitHandler(Heading::class.java) {
//                ann.pushStyle(SpanStyle(fontSize = (22 - 2*it.level).coerceAtLeast(16).sp))
//                if (it.openingMarker.isNotBlank()) {
//                    ann.append(it.openingMarker)
//                    ann.append(" ")
//                }
//                visitor.visitChildren(it)
//                ann.pop()
//                ann.append(md.chars.substring(it.endOffset, it.next?.startOffset ?: it.parent?.endOffset ?: it.endOffset))
//            },
//
//            VisitHandler(Node::class.java) {
//                ann.append(md.chars.substring(it.endOffset, it.next?.startOffset ?: it.parent?.endOffset ?: it.endOffset))
//            }
//        ))
//        visitor.visit(md)

        ann.append(md)
        content.value = newContent.copy(ann.toAnnotatedString())
    }

    fun requestFocus() {
        focusRequester.requestFocus()
    }
}

@OptIn(ExperimentalTextApi::class)
val sourceCodePro = FontFamily("JetBrainsMono Nerd Font")

@Composable
fun BlockCursorOutlinedTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle.Default
) {
    val cursorBrush = SolidColor(Color.Black)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = textStyle.copy(textDecoration = TextDecoration.None),
        cursorBrush = cursorBrush,
        visualTransformation = VisualTransformation.None
    )
}

@Composable
fun Editor(state: EditorState) {
    BlockCursorOutlinedTextField(state.content.value, state::updateContent,
        textStyle = TextStyle(
            fontFamily = sourceCodePro,
            fontSize = 16.sp
        ),
        modifier = Modifier.fillMaxSize()
            .padding(5.0.dp)
            .focusRequester(state.focusRequester)
    )
    LaunchedEffect(state) {
        state.requestFocus()
    }
}

