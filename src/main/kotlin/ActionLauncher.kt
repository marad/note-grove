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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication

data class Action(
    val name: String,
    val description: String? = null,
    private val operation: () -> Unit
) {
    fun call() { operation() }
}

class LauncherState {
    val text = mutableStateOf(TextFieldValue())
    val actions = mutableStateListOf<Action>()
    val selectedItem = mutableStateOf(0)

    fun selectNext() {
        selectedItem.value = (selectedItem.value+1).coerceIn(0, (actions.size-1).coerceAtLeast(0))
    }

    fun selectPrevious() {
        selectedItem.value = (selectedItem.value-1).coerceIn(0, (actions.size-1).coerceAtLeast(0))
    }
}

@Composable
fun ActionLauncher(state: LauncherState,
                   onSearchChange: (String) -> Unit = {},
                   onComplete: (Action) -> Unit = {},
                   onCancel: () -> Unit = {}
) {
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
                            state.selectPrevious()
                        }
                        return@onPreviewKeyEvent true
                    }
                    if (ev.key == Key.DirectionDown || (ev.key == Key.J && ev.isCtrlPressed)) {
                        if (ev.type == KeyEventType.KeyDown) {
                            state.selectNext()
                        }
                        return@onPreviewKeyEvent true
                    }
                    if (ev.key == Key.Enter) {
                        if (ev.type == KeyEventType.KeyDown) {
                            if (state.selectedItem.value in 0..<state.actions.size) {
                                val action = state.actions[state.selectedItem.value]
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
                value = state.text.value,
                onValueChange = {
                    state.text.value = it
                    onSearchChange(it.text)
                },
                singleLine = true,
                placeholder = {
                    Text("Search your notes...", color = Color.LightGray)
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
                            highlighted = state.selectedItem.value == index,
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
    val vault = Vault("/home/marad/dendron/notes/")
    val state = remember {
        LauncherState()
    }


    ActionLauncher(state,
        onSearchChange = { name ->
            state.actions.clear()
            val files = vault.searchFiles(name)
            println(files)
            state.actions.addAll(files.map { Action(it) { println(it) } })
        },
        onComplete = { action ->
            println("Got action: $action")
        },
        onCancel = { println("Cancelled!") }
    )
}

fun main() {
    singleWindowApplication(
        WindowState(size = DpSize(1000.dp, 800.dp))
    ) {
        test()
    }
}
