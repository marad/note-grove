package editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.window.singleWindowApplication
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class VimModeState(
    val mode: Mode = Mode.Normal
)

class VimModeViewModel : ViewModel() {
    private val _state = MutableStateFlow(VimModeState())
    val state = _state.asStateFlow()

    fun switchMode(mode: Mode) {
        _state.update { it.copy(mode = mode) }
    }
}

enum class Mode {
    Normal, Insert, Visual
}

interface Motion {
    fun find(cursorPosition: Int, text: String): Int
}

interface TextObject {
    //InnerWord, AroundWord, InnerSentence, OuterSentence, InnerParagraph, OuterParagraph
    fun inner(cursorPosition: Int, text: String): TextRange
    fun around(cursorPosition: Int, text: String): TextRange
}

interface VimAction {
    // "Change" action would also require to change the state so that it'll land in insert mode
    // maybe perform should return text, cursor position and mode?
    fun perform(range: TextRange, text: String): String
}

class VimCommandHandler(private val editor: EditorState) {
    fun onKey(event: KeyEvent): Boolean {
        if (event.key == Key.J && event.type == KeyEventType.KeyDown) {
            // TODO: move cursor down
            return true
        }
        return false
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VimMode(editorState: EditorState,
            vm: VimModeViewModel = viewModel { VimModeViewModel() },
            modifier: Modifier = Modifier,
            onRequestCompletions: (String) -> List<String> = { emptyList() }) {

    val state by vm.state.collectAsState()
    val handler = remember { VimCommandHandler(editorState) }
    val layout = remember { mutableStateOf<TextLayoutResult?>(null) }
    val color = MaterialTheme.colors.secondary
    val lay = layout.value

    Box(modifier
        .onPreviewKeyEvent(handler::onKey) ) {
        if (lay != null && state.mode == Mode.Normal) {
            val offset = editorState.getContent().selection.start.coerceAtMost(editorState.getContent().text.length-1)
            val rect = lay.getBoundingBox(offset)
            val top = rect.top - editorState.scrollState.offset
            val left = rect.left
            var width = rect.width
            val height = rect.height

            if (width.toInt() == 0 && offset > 1) {
                val rect = lay.getBoundingBox(offset - 1)
                width = rect.width
            }
            if (width.toInt() == 0) {
                width = 20f
            }
            androidx.compose.foundation.Canvas(Modifier) {
                drawRect(
                    color,
                    Offset(left, top),
                    Size(width, height)
                )
            }
        }
        Row(Modifier.fillMaxSize()) {
            Editor(
                editorState,
                onTextLayout = { layout.value = it },
                onRequestCompletions = onRequestCompletions,
                cursorBrush = if (state.mode == Mode.Insert) SolidColor(Color.Black) else SolidColor(Color.Transparent),
                //modifier = Modifier.fillMaxSize()
            )
            Button(onClick = {
                if (state.mode == Mode.Normal) {
                    vm.switchMode(Mode.Insert)
                } else {
                    vm.switchMode(Mode.Normal)
                }
            }) {
                Text("Toggle mode")
            }
        }
    }
}

fun main() = singleWindowApplication {
    val editorState = EditorState("Hello World\nThis *is* a test")
    VimMode(editorState)
}