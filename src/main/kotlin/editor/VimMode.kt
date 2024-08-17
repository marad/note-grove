package editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.isTypedEvent
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

@FunctionalInterface
interface Reader {
    fun onKeyEvent(keyEvent: KeyEvent): Boolean
}

typealias Action = () -> Unit
typealias ActionReady = (Action) -> Unit
typealias ChangeReader = (Reader) -> Unit


data class VimModeState(
    val mode: Mode = Mode.Normal
)

class VimModeViewModel : ViewModel() {
    private val _state = MutableStateFlow(VimModeState())
    val state = _state.asStateFlow()
    val editorViewModel = EditorViewModel("Hello World\nThis *is* sparta!")
    private var consumeNextKeyTyped = false

    fun switchMode(mode: Mode) {
        _state.update { it.copy(mode = mode) }
    }

    fun handleKey(event: KeyEvent): Boolean {
        fun switchMode(mode: Mode) {
            this.switchMode(mode)
            consumeNextKeyTyped = true
        }
        if (consumeNextKeyTyped && event.isTypedEvent) {
            consumeNextKeyTyped = false
            return true
        }

        if (state.value.mode == Mode.Normal) {
            if (event.key == Key.J && event.type == KeyEventType.KeyDown) {
                editorViewModel.moveCaretDown()
            }
            else if (event.key == Key.K && event.type == KeyEventType.KeyDown) {
                editorViewModel.moveCaretUp()
            }
            else if (event.key == Key.H && event.type == KeyEventType.KeyDown) {
                editorViewModel.moveCaretLeft()
            }
            else if (event.key == Key.L && event.type == KeyEventType.KeyDown) {
                editorViewModel.moveCaretRight()
            }
            else if (event.key == Key.I && event.isShiftPressed && event.type == KeyEventType.KeyDown) {
                editorViewModel.moveCaretToStartOfCurrentLine()
                switchMode(Mode.Insert)
            }
            else if (event.key == Key.I && event.type == KeyEventType.KeyDown) {
                switchMode(Mode.Insert)
            }
            else if (event.key == Key.A && event.isShiftPressed && event.type == KeyEventType.KeyDown) {
                editorViewModel.moveCaretToEndOfCurrentLine()
                switchMode(Mode.Insert)
            }
            else if (event.key == Key.A && event.type == KeyEventType.KeyDown) {
                editorViewModel.moveCaretRight()
                switchMode(Mode.Insert)
            }
            else if (event.key == Key.Four && event.isShiftPressed && event.type == KeyEventType.KeyDown) {
                editorViewModel.moveCaretToEndOfCurrentLine()
            }
            else if (event.key == Key.Zero && event.type == KeyEventType.KeyDown) {
                editorViewModel.moveCaretToStartOfCurrentLine()
            }
            else if (event.key == Key.W && event.type == KeyEventType.KeyDown) {
                editorViewModel.moveCaretToEndOfNextWord()
            }
            else if (event.key == Key.B && event.type == KeyEventType.KeyDown) {
                editorViewModel.moveCaretToBeginingOfPreviousWord()
            }
            else if (event.key == Key.O && event.isShiftPressed && event.type == KeyEventType.KeyDown) {
                editorViewModel.insertLineAbove()
                switchMode(Mode.Insert)
            }
            else if (event.key == Key.O && event.type == KeyEventType.KeyDown) {
                editorViewModel.insertLineBelow()
                switchMode(Mode.Insert)
            }
            return true
        }
        else if (state.value.mode == Mode.Insert) {
            if ((event.key == Key.Escape || (event.key == Key.LeftBracket && event.isCtrlPressed)) && event.type == KeyEventType.KeyDown) {
                editorViewModel.moveCaretLeft()
                switchMode(Mode.Normal)
                return true
            }
        }
        return false
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
    fun perform(range: TextRange, vm: VimModeViewModel)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VimMode(vm: VimModeViewModel = viewModel { VimModeViewModel() },
            modifier: Modifier = Modifier,
            onRequestCompletions: (String) -> List<String> = { emptyList() }) {

    val editorViewModel = vm.editorViewModel
    val state by vm.state.collectAsState()
    val layout = remember { mutableStateOf<TextLayoutResult?>(null) }
    val color = MaterialTheme.colors.secondary
    val lay = layout.value

    val editorState by editorViewModel.state.collectAsState()

    Box(modifier
        .onPreviewKeyEvent(vm::handleKey) ) {
        if (lay != null && state.mode == Mode.Normal) {
            val offset = editorState.content.selection.start.coerceAtMost(editorState.content.text.length-1)
            val rect = lay.getBoundingBox(offset)
            val top = rect.top - editorViewModel.scrollState.offset
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
                editorViewModel,
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
            Button(onClick = { vm.editorViewModel.moveCaretUp() }) {
                Text("Up")
            }
            Button(onClick = { vm.editorViewModel.moveCaretDown() }) {
                Text("Down")
            }
            Button(onClick = { vm.editorViewModel.moveCaretLeft() }) {
                Text("Left")
            }
            Button(onClick = { vm.editorViewModel.moveCaretRight() }) {
                Text("Right")
            }
        }
    }
}

fun main() = singleWindowApplication {
    VimMode()
}