package editor

import Markdown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
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
import androidx.lifecycle.viewmodel.compose.viewModel


@OptIn(ExperimentalTextApi::class)
val editorFont = FontFamily("JetBrainsMono Nerd Font")


@Composable
fun Editor(vm: EditorViewModel = viewModel { EditorViewModel() },
           modifier: Modifier = Modifier,
           cursorBrush: Brush = SolidColor(Color.Black),
           onTextLayout: (TextLayoutResult) -> Unit = {},
           onRequestCompletions: (String) -> List<String> = { emptyList() }) {
    val state by vm.state.collectAsState()
    val layout = remember { mutableStateOf<TextLayoutResult?>(null) }
    val lay = layout.value
    Box(modifier) {
        BasicTextField(
            value = state.content,
            cursorBrush = cursorBrush,
            onValueChange = {
                val before = it.getTextBeforeSelection(2).text
                if (before == "[[") {
                    vm.completionsState.startCompletion(
                        it.selection.end.coerceAtLeast(0)
                    )
                }
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
            modifier = Modifier
                .onPreviewKeyEvent {
                    if (vm.completionsState.isVisible()) {
                        if (it.key == Key.Escape && it.type == KeyEventType.KeyDown) {
                            vm.completionsState.hide()
                            return@onPreviewKeyEvent true
                        }
                        if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                            vm.completionsState.getSelected()?.let {
                                val startOffset = vm.completionsState.getStartOffset()
                                vm.replace(startOffset, state.content.selection.end, it)
                            }
                            vm.completionsState.hide()
                            return@onPreviewKeyEvent true
                        }
                        if (it.type == KeyEventType.KeyDown &&
                            (it.key == Key.J && it.isCtrlPressed) || (it.key == Key.DirectionDown)) {
                            vm.completionsState.next()
                            return@onPreviewKeyEvent true
                        }
                        if (it.type == KeyEventType.KeyDown &&
                            (it.key == Key.K && it.isCtrlPressed) || (it.key == Key.DirectionUp)) {
                            vm.completionsState.previous()
                            return@onPreviewKeyEvent true
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
                val highlighted = Markdown.parse(text.text).toAnnotatedString()
                TransformedText(highlighted, OffsetMapping.Identity)
            },
            onTextLayout = {
                layout.value = it
                onTextLayout(it)
            }
        )


        if (vm.completionsState.isVisible()) {
            if (lay != null) {
                val offset = state.content.selection.start
                val left = lay.getHorizontalPosition(offset, true)
                val top = lay.getLineBottom(lay.getLineForOffset(offset))
                Completions(
                    vm.completionsState,
                    Modifier.offset((left / 2).dp, (top / 2).dp)
                )
            }
        }
    }
    LaunchedEffect(state) {
        vm.focusRequester.requestFocus()
    }
}

