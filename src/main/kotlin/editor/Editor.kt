package editor

import Markdown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.getTextBeforeSelection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel


@OptIn(ExperimentalTextApi::class)
val editorFont = FontFamily("JetBrainsMono Nerd Font")


fun calculateCompletionsOffset(
    layout: TextLayoutResult,
    content: TextFieldValue,
    density: Float
): IntOffset {
    val offset = (content.selection.start - 1).coerceAtLeast(0)
    val left = layout.getHorizontalPosition(offset, true)
    val top = layout.getLineBottom(layout.getLineForOffset(offset))

    return IntOffset(
        (left / density).toInt(),
        (top / density).toInt()
    )
}

@Composable
fun Editor(vm: EditorViewModel = viewModel { EditorViewModel() },
           modifier: Modifier = Modifier,
           cursorBrush: Brush = SolidColor(Color.Black),
           onTextLayout: (TextLayoutResult) -> Unit = {},
           onRequestCompletions: (String) -> List<String> = { emptyList() }) {
    val state by vm.state.collectAsState()
    val density = LocalDensity.current.density

    Box {
        BasicTextField(
            value = state.content,
            cursorBrush = cursorBrush,
            onValueChange = {
                if (vm.completionsState.isVisible()) {
                    if (vm.completionsState.getStartOffset() > it.selection.end) {
                        vm.completionsState.hide()
                    } else {
                        val query = it.text.substring(
                            vm.completionsState.getStartOffset(), it.selection.end)
                        val items = onRequestCompletions(query)
                        vm.completionsState.setItems(items)
                    }
                }
                vm.updateContent(it)
            },
            modifier = modifier
                .onPreviewKeyEvent {
                    if (vm.completionsState.isVisible()) {
                        if (it.key == Key.Escape && it.type == KeyEventType.KeyDown) {
                            vm.completionsState.hide()
                            return@onPreviewKeyEvent true
                        }
                        if ((it.key == Key.Tab || it.key == Key.Enter) && it.type == KeyEventType.KeyDown) {
                            vm.completionsState.getSelected()?.let {
                                val startOffset = vm.completionsState.getStartOffset()
                                vm.replace(TextRange(startOffset, state.content.selection.end), "$it]]")
                            }
                            vm.completionsState.hide()
                            return@onPreviewKeyEvent true
                        }
                        if (it.type == KeyEventType.KeyDown &&
                            ((it.key == Key.J && it.isCtrlPressed) || (it.key == Key.DirectionDown))) {
                            vm.completionsState.next()
                            return@onPreviewKeyEvent true
                        }
                        if (it.type == KeyEventType.KeyDown &&
                            ((it.key == Key.K && it.isCtrlPressed) || (it.key == Key.DirectionUp))) {
                            vm.completionsState.previous()
                            return@onPreviewKeyEvent true
                        }
                    }

                    if (it.key == Key.LeftBracket && it.type == KeyEventType.KeyUp) {
                        val before = state.content.getTextBeforeSelection(2).text
                        if (before == "[[") {
                            val offset = state.content.selection.end.coerceAtLeast(0)
                            vm.completionsState.startCompletion(offset)
                            val items = onRequestCompletions("")
                            vm.completionsState.setItems(items)
                        }
                    }
                    return@onPreviewKeyEvent false
                }
                .focusRequester(vm.focusRequester),
            textStyle = TextStyle(
                fontFamily = editorFont,
                fontSize = 16.sp
            ),
            visualTransformation = { text ->
                val highlighted = Markdown.parse(text.text).highlighted()
                TransformedText(highlighted, OffsetMapping.Identity)
            },
            onTextLayout = {
                vm.layout = it
                vm.completionsState.offset.value = calculateCompletionsOffset(it, state.content, density)
                onTextLayout(it)
            }
        )

        if (vm.completionsState.isVisible()) {
            Completions(vm.completionsState)
        }
    }
    LaunchedEffect(state) {
        vm.focusRequester.requestFocus()
    }
}

