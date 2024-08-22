import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ConfirmDialogState(
    val question: String,
    val confirmAction: Action,
    val cancelAction: Action
)

class ConfirmDialogViewModel : ViewModel() {
    private val _state: MutableStateFlow<ConfirmDialogState?> = MutableStateFlow(null)
    val state = _state.asStateFlow()
    val searchVm = SearchDialogViewModel()

    init {
        searchVm.onSearchActions = { text ->
            val state = state.value
            if (state != null) {
                listOf(state.confirmAction, state.cancelAction).filter {
                    it.name.contains(text, ignoreCase = true) || it.description?.contains(
                        text,
                        ignoreCase = true
                    ) == true
                }
            } else {
                emptyList()
            }
        }
        searchVm.show()
    }

    fun show(question: String, onConfirm: () -> Unit, onCancel: () -> Unit) {
        _state.value = ConfirmDialogState(
            question = question,
            confirmAction = Action("Confirm", "Confirm the action", onConfirm),
            cancelAction = Action("Cancel", "Cancel the action", onCancel)
        )
        searchVm.search("")
        searchVm.state.value.actionLauncherState.selectedItem.value = 0
    }

    fun hide() {
        _state.value = null
    }
}

@Composable
fun ConfirmDialog(vm: ConfirmDialogViewModel) {
    val state by vm.state.collectAsState()
    if (state != null) {
        SearchDialog(vm.searchVm,
            onCompletion = { action ->
                vm.hide()
                action.call()
            },
            onDismissRequest = { vm.hide() },
        )
    }
}
