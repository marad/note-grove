package v2.window

import NoteName
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import v2.BufferManager
import v2.notestream.NoteCardState
import v2.notestream.NoteStream
import v2.notestream.NoteStreamState

data class MainWindowState(
    val noteStreamState: NoteStreamState = NoteStreamState(),
    val windowState: WindowState = WindowState(),
    private val roots: List<Root>,
    private val activeRoot: Int = 0,
    val bufferManager: BufferManager
) {
    init {
        assert(roots.isNotEmpty()) { "At least one root should be provided!" }
    }

    val root get() = roots[activeRoot]
}

fun openNote(state: MainWindowState, noteName: NoteName) =
    state.copy(noteStreamState = state.noteStreamState.prependCard(
        NoteCardState(state.bufferManager.openBuffer(state.root.pathToFile(noteName)))))

@Composable
fun MainWindow(state: MainWindowState,
               onUpdate: (MainWindowState) -> Unit = {},
               onCloseRequest: () -> Unit = {}) {
    Window(
        title = "Note Grove - ${state.root.name}",
        state = state.windowState,
        onCloseRequest = onCloseRequest
    ) {
        Column {
            Button(
                onClick = {
                    onUpdate(openNote(state, NoteName("test.note")))
                }
            ) {
                Icon(Icons.Default.Add, "")
            }

            NoteStream(
                state.noteStreamState,
                modifier = Modifier.weight(1f),
                onUpdate = { onUpdate(state.copy(noteStreamState = it)) },
            )
        }
    }
}
