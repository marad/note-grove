package editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.getTextBeforeSelection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.text.substring


@OptIn(ExperimentalTextApi::class)
val editorFont = FontFamily("JetBrainsMono Nerd Font")


@Composable
fun Editor(state: EditorState, onRequestCompletions: (String) -> List<String> = { emptyList() }) {
    Row {
        BasicTextField(
            value = state.getContent(),
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
            modifier = Modifier.weight(1f)
                .padding(5.0.dp)
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
            visualTransformation = VisualTransformation.None,
        )

        if (state.completionsState.isVisible()) {
            Completions(state.completionsState)
        }
    }
    LaunchedEffect(state) {
        state.requestFocus()
    }
}

