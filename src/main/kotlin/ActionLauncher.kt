import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication
import androidx.lifecycle.ViewModel
import config.AppConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.file.Paths

data class Action(
    val name: String,
    val description: String? = null,
    private val operation: () -> Unit
) {
    fun call() { operation() }
}

data class LauncherState(
    val visible: Boolean = false,
    val text: TextFieldValue = TextFieldValue(),
    val actions: List<Action> = listOf(),
    val selectedItem: Int = 0,
    val searchActions: (String) -> List<Action> = { emptyList() }
) {
    fun selectNext() = copy(selectedItem = (selectedItem+1)
        .coerceIn(0, (actions.size-1).coerceAtLeast(0)))

    fun selectPrevious() = copy(selectedItem = (selectedItem-1)
        .coerceIn(0, (actions.size-1).coerceAtLeast(0)))

    fun isSelected(index: Int) = selectedItem == index

    fun show() = copy(visible = true)
    fun hide() = copy(visible = false)
}

class LauncherViewModel : ViewModel() {
    private val _state = MutableStateFlow(LauncherState())
    val state = _state.asStateFlow()

    fun selectNext() {
        _state.value = _state.value.selectNext()
    }

    fun selectPrevious() {
        _state.value = _state.value.selectPrevious()
    }

    fun textFieldChanged(textFieldValue: TextFieldValue) {
        _state.value = _state.value.copy(
            text = textFieldValue,
            actions = state.value.searchActions(textFieldValue.text)
        )
    }

    fun showInput(initialQuery: String? = "", onAccept: (String) -> Unit) {
        show(initialQuery) {
            listOf(
                Action("Accept", "Accept the input", { onAccept(it) }),
                Action("Cancel", "Cancel the input", { })
            )
        }
    }

    fun showConfirm(onConfirm: () -> Unit, onCancel: () -> Unit) {
        val confirmAction = Action("Confirm", "Confirm the action", onConfirm)
        val cancelAction = Action("Cancel", "Cancel the action", onCancel)
        show(listOf(confirmAction, cancelAction), initialQuery = "")
    }

    fun show(actions: List<Action>, initialQuery: String? = null) {
        show(initialQuery) { query ->
            actions.filter {
                it.name.contains(query, ignoreCase = true) ||
                        (it.description?.contains(query, ignoreCase = true) ?: false)
            }
        }
    }

    fun show(initialQuery: String? = null, searchActions: (String) -> List<Action>) {
        val finalQuery = initialQuery ?: _state.value.text.text
        _state.value = LauncherState(
            visible = true,
            text = _state.value.text.copy(text = finalQuery, selection = TextRange(finalQuery.length)),
            selectedItem = 0,
            actions = searchActions(finalQuery),
            searchActions = searchActions)
    }

    fun hide() {
        _state.value = _state.value.hide()
    }
}

@Composable
fun ActionLauncherDialog(vm: LauncherViewModel,
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
        ActionLauncher(vm,
            onComplete = onCompletion,
            onCancel = onDismissRequest
        )
    }

}


@Composable
fun ActionLauncher(vm: LauncherViewModel,
                   placeholder: String = "Type to select...",
                   onComplete: (Action) -> Unit = {},
                   onCancel: () -> Unit = {}
) {
    val state by vm.state.collectAsState()

    val searchFieldFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        searchFieldFocusRequester.requestFocus()
    }

    Card {
        Column(
            modifier = Modifier.padding(10.dp)
                .onPreviewKeyEvent { ev ->
                    if (ev.key == Key.DirectionUp || (ev.key == Key.K && ev.isCtrlPressed)) {
                        if (ev.type == KeyEventType.KeyDown) {
                            vm.selectPrevious()
                        }
                        return@onPreviewKeyEvent true
                    }
                    if (ev.key == Key.DirectionDown || (ev.key == Key.J && ev.isCtrlPressed)) {
                        if (ev.type == KeyEventType.KeyDown) {
                            vm.selectNext()
                        }
                        return@onPreviewKeyEvent true
                    }
                    if (ev.key == Key.Enter) {
                        if (ev.type == KeyEventType.KeyDown) {
                            if (state.selectedItem in 0..<state.actions.size) {
                                val action = state.actions[state.selectedItem]
                                onComplete(action)
                            }
                        }
                        return@onPreviewKeyEvent true
                    }
                    if (ev.key == Key.Escape) {
                        if (ev.type == KeyEventType.KeyDown) {
                            onCancel()
                        }
                        return@onPreviewKeyEvent true
                    }
                    false
                }
        ) {
            OutlinedTextField(
                value = state.text,
                onValueChange = vm::textFieldChanged,
                singleLine = true,
                placeholder = {
                    Text(placeholder, color = Color.LightGray)
                },
                modifier = Modifier.fillMaxWidth()
                    .focusRequester(searchFieldFocusRequester)
            )

            LazyColumn(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                state.actions.forEachIndexed { index, it ->
                    item {
                        ActionItem(
                            it,
                            highlighted = state.isSelected(index),
                            modifier = Modifier.fillMaxWidth()
                                .padding(5.dp)
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun ActionItem(action: Action,
               highlighted: Boolean = false,
               backgroundColor: Color = MaterialTheme.colors.surface,
               highlightedBackground: Color = MaterialTheme.colors.secondary,
               modifier: Modifier = Modifier) {
    Card(modifier,
        backgroundColor = if (highlighted) highlightedBackground else backgroundColor,
        ) {
        Column(Modifier.padding(5.dp)) {
            Text(action.name, fontSize = 18.sp)
            if (action.description != null) {
                Text(action.description, fontSize = 12.sp)
            }
        }
    }
}


@Composable
@Preview
fun test() {
    val rootConfig = AppConfig.load(Paths.get("config.toml"))?.roots?.first() ?: throw IllegalStateException("No roots found")
    val root = Root(rootConfig.path)
    val vm = remember { LauncherViewModel() }

    vm.show { name ->
        val files = root.searchFiles(name)
        files.map { Action(it) { println(it) } }
    }

    ActionLauncherDialog(vm)
}

fun main() {
    singleWindowApplication(
        WindowState(size = DpSize(1000.dp, 800.dp))
    ) {
        test()
    }
}
