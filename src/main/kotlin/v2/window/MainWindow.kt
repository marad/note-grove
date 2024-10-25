package v2.window

import ActionLauncherDialog
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import v2.notestream.NoteStream
import v2.notestream.NoteStreamState

data class MainWindowState(
    val noteStreamState: NoteStreamState = NoteStreamState(),
    val windowState: WindowState = WindowState(),
    val roots: List<Root>,
    private val activeRoot: Int = 0,
) {
    init {
        assert(roots.isNotEmpty()) { "At least one root should be provided!" }
    }

    val root get() = roots[activeRoot]
    fun cycleRoots() =
            copy(activeRoot = (activeRoot+1)%roots.size)
}

@Composable
fun MainWindow(controller: MainWindowController,
               onCloseRequest: () -> Unit = {}) {

    val state by controller.state.collectAsState()
    var selectedNoteIndex by controller.selectedNoteIndex

    Window(
        title = "Note Grove - ${state.root.name}",
        state = state.windowState,
        onCloseRequest = onCloseRequest,
        onPreviewKeyEvent = { event ->
            if (!controller.launcher.state.value.visible) {
                controller.shortcuts.handle(event)
            } else {
                false
            }
        }
    ) {
        selectedNoteIndex = selectedNoteIndex.coerceIn(minOf(-1, state.noteStreamState.cards.size-1), state.noteStreamState.cards.size-1)
        
        MaterialTheme {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Surface(color = Color(0xFFEEEEEE), modifier = Modifier.fillMaxSize()) {}
                Column {
                    NoteStream(
                        state.noteStreamState,
                        lazyListState = controller.streamLazyListState,
                        modifier = Modifier.weight(1f),
                        onUpdate = { stream -> controller.updateState { state.copy(noteStreamState = stream) } },
                        outlineNote = controller.selectedNoteIndex.value,
                        onItemFocused = { idx ->
                            controller.selectNote(idx)
                        }
                    )
                }

                ActionLauncherDialog(controller.launcher)
            }
        }
    }
}
