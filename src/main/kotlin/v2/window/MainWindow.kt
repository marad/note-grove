package v2.window

import ActionLauncherDialog
import NoteName
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import v2.notestream.NoteStream
import v2.notestream.NoteStreamState
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp

data class MainWindowState(
    val noteStreamState: NoteStreamState = NoteStreamState(),
    val windowState: WindowState = WindowState(),
    private val roots: List<Root>,
    private val activeRoot: Int = 0,
    val lastSelectedNote: Int = -1
) {
    init {
        assert(roots.isNotEmpty()) { "At least one root should be provided!" }
    }

    val root get() = roots[activeRoot]
}

@Composable
fun MainWindow(controller: MainWindowController,
               onCloseRequest: () -> Unit = {}) {

    val state by controller.state.collectAsState()
    println(state.lastSelectedNote)

    Window(
        title = "Note Grove - ${state.root.name}",
        state = state.windowState,
        onCloseRequest = onCloseRequest,
        onPreviewKeyEvent = { event ->
            controller.shortcuts.handle(event)
        }
    ) {
        MaterialTheme {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    Modifier.padding(10.dp)
                ) {
                    Button(
                        onClick = {
                            controller.launcher.showInput {
                                controller.openNote(NoteName(it))
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, "")
                    }

                    NoteStream(
                        state.noteStreamState,
                        lazyListState = controller.streamLazyListState,
                        modifier = Modifier.weight(1f),
                        onUpdate = { controller.updateState(state, state.copy(noteStreamState = it)) },
                        outlineNote = state.lastSelectedNote,
                        onItemFocused = { idx ->
                            println(idx)
                            controller.updateState(
                                state,
                                state.copy(lastSelectedNote = idx)
                            )
                        }
                    )
                }

                ActionLauncherDialog(controller.launcher)
            }
        }
    }
}
