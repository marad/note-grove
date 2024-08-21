import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.window.Dialog

class SearchDialogState {
    private val visible = mutableStateOf(false)
    val actionLauncherState = LauncherState()

    fun showWithPrefix(prefix: String) {
        show()
        actionLauncherState.text.value = TextFieldValue(prefix, TextRange(prefix.length))
    }
    fun show() { visible.value = true}
    fun hide() { visible.value = false }
    fun isVisible() = visible.value

}

@Composable
fun SearchDialog(state: SearchDialogState, onSearchActions: (String) -> List<Action>) {
    Dialog(
        onDismissRequest = { state.hide() },
    ) {
        ActionLauncher(state.actionLauncherState,
            onSearchChange = { name ->
                state.actionLauncherState.actions.clear()
                state.actionLauncherState.actions.addAll(onSearchActions(name))
            },
            onComplete = { action ->
                state.hide()
                action.call()
            },
            onCancel = { state.hide() }
        )
    }

}

