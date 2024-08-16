package editor

import Markdown
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.getTextBeforeSelection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@OptIn(ExperimentalTextApi::class)
val editorFont = FontFamily("JetBrainsMono Nerd Font")


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Editor(state: EditorState,
           modifier: Modifier = Modifier,
           cursorBrush: Brush = SolidColor(Color.Black),
           onTextLayout: (TextLayoutResult) -> Unit = {},
           onRequestCompletions: (String) -> List<String> = { emptyList() }) {
    val layout = remember { mutableStateOf<TextLayoutResult?>(null) }
    val lay = layout.value
    Box(modifier) {
        BasicTextField(
            scrollState = state.scrollState,
            value = state.getContent(),
            cursorBrush = cursorBrush,
            onValueChange = {
                val before = it.getTextBeforeSelection(2).text
                if (before == "[[") {
                    state.completionsState.startCompletion(
                        it.selection.end.coerceAtLeast(0)
                    )
                }
                if (state.completionsState.isVisible()) {
                    if (state.completionsState.getStartOffset() > it.selection.end) {
                        state.completionsState.hide()
                    } else {
                        val query = it.text.substring(
                            state.completionsState.getStartOffset(), it.selection.end)
                        val items = onRequestCompletions(query)
                        state.completionsState.setItems(items)
                    }
                }
                state.updateContent(it)
            },
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent {
                    if (state.completionsState.isVisible()) {
                        if (it.key == Key.Escape && it.type == KeyEventType.KeyDown) {
                            state.completionsState.hide()
                            return@onPreviewKeyEvent true
                        }
                        if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                            state.completionsState.getSelected()?.let {
                                val startOffset = state.completionsState.getStartOffset()
                                state.replace(startOffset, state.getContent().selection.end, it)
                            }
                            state.completionsState.hide()
                            return@onPreviewKeyEvent true
                        }
                        if (it.type == KeyEventType.KeyDown &&
                            (it.key == Key.J && it.isCtrlPressed) || (it.key == Key.DirectionDown)) {
                            state.completionsState.next()
                            return@onPreviewKeyEvent true
                        }
                        if (it.type == KeyEventType.KeyDown &&
                            (it.key == Key.K && it.isCtrlPressed) || (it.key == Key.DirectionUp)) {
                            state.completionsState.previous()
                            return@onPreviewKeyEvent true
                        }
                    }
                    return@onPreviewKeyEvent false
                }
                .focusRequester(state.focusRequester),
            textStyle = TextStyle(
                fontFamily = editorFont,
                fontSize = 16.sp
            ),
            visualTransformation = { text ->
                val highlighted = Markdown.parse(text.text).toAnnotatedString()
                TransformedText(highlighted, OffsetMapping.Identity)
            },
            onTextLayout = {
                layout.value = it
                onTextLayout(it)
            }
        )


        if (state.completionsState.isVisible()) {
            if (lay != null) {
                val offset = state.getContent().selection.start
                val left = lay.getHorizontalPosition(offset, true)
                val top = lay.getLineBottom(lay.getLineForOffset(offset))
                Completions(
                    state.completionsState,
                    Modifier.offset((left / 2).dp, (top / 2).dp)
                )
            }
        }
    }
    LaunchedEffect(state) {
        state.requestFocus()
    }
}

