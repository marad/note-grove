import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


data class SearchDialogState(
    val visible: Boolean = false,
    val actionLauncherState: LauncherState = LauncherState()
)

class SearchDialogViewModel : ViewModel() {
    private val _state = MutableStateFlow((SearchDialogState()))
    val state = _state.asStateFlow()
    var onSearchActions: (String) -> List<Action> = { emptyList() }

    fun showWithPrefix(prefix: String) {
        state.value.actionLauncherState.text.value = TextFieldValue(prefix, TextRange(prefix.length))
        search(prefix)
        show()
    }
    fun show() {
        search(state.value.actionLauncherState.text.value.text)
        _state.value = state.value.copy(visible = true)
    }
    fun hide() { _state.value = state.value.copy(visible = false) }
    fun isVisible() = _state.value.visible

    fun search(query: String) {
        state.value.actionLauncherState.actions.clear()
        state.value.actionLauncherState.actions.addAll(onSearchActions(query))
    }
}


@Composable
fun SearchDialog(vm: SearchDialogViewModel,
                 onCompletion: (Action) -> Unit = {
                     vm.hide()
                     it.call()
                 },
                 onDismissRequest: () -> Unit = { vm.hide() }) {
    val state by vm.state.collectAsState()

    if (!state.visible) return

    Dialog(
        onDismissRequest = onDismissRequest,
    ) {
        ActionLauncher(state.actionLauncherState,
            onSearchChange = vm::search,
            onComplete = onCompletion,
            onCancel = onDismissRequest
        )
    }

}

