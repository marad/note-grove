package window

import ActionLauncherDialog
import App
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.lightColors
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.window.Window

@Composable
fun NoteWindow(vm: NoteWindowViewModel,
               newWindowRequested: () -> Unit = {},
               onCloseRequest: () -> Unit = {}
               ) {
    val state by vm.state.collectAsState()

    Window(
        title = "Note Grove - ${state.currentRootName}",
        state = state.windowState,
        onPreviewKeyEvent = { event ->
            if (event.isShiftPressed && event.isCtrlPressed && event.key == Key.N && event.type == KeyEventType.KeyDown) {
                newWindowRequested()
                false
            } else {
                vm.shortcuts.handle(event)
            }
        },
        onCloseRequest = onCloseRequest) {

        val primaryColor = Color(0.21f, 0.4f, 0.32f)
        MaterialTheme(colors = lightColors(primary = primaryColor)) {
            Surface {
                App(vm, onRequestCompletions = { tab, query ->
                    val root = vm.state.value.root
                    root.searchFiles(query).map { it.name }
                })
            }

            ActionLauncherDialog(vm.actionLauncherViewModel)
        }
    }
}
