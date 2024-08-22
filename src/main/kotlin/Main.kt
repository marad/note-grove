import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.sharp.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.file.Path


class AppState {
    val workspaceState = WorkspaceState()
}

class AppViewModel(
    appState: AppState = AppState(),
    val vault: Vault,
    val inputDialogViewModel: InputDialogViewModel = InputDialogViewModel(),
    val searchDialogViewModel: SearchDialogViewModel = SearchDialogViewModel(),
    val confirmDialogViewModel: ConfirmDialogViewModel = ConfirmDialogViewModel(),
) : ViewModel() {
    private val _state = MutableStateFlow(appState)
    val state = _state.asStateFlow()
}

@Composable
@Preview
fun App(appVm: AppViewModel, onRequestCompletions: (tabState: TabState, query: String) -> List<String> = { _,_ -> emptyList() }) {

    val state by appVm.state.collectAsState()

    Row(Modifier.fillMaxSize()) {
        ToolBar(
            onFilesClicked = {
                println("Adding tab")
                state.workspaceState.addTab("testing", Path.of("/tmp/testfile.md"))
            }
        )
        //FileList()

        Divider(
            Modifier
                .fillMaxHeight()
                .width(2.dp),
            color = Color.LightGray,
        )

        Workspace(state.workspaceState, onRequestCompletions = onRequestCompletions)
    }

}


@Composable
fun ToolBar(
    onFilesClicked: () -> Unit = {},
    onSearchClicked: () -> Unit = {},
) {

    Column(Modifier.fillMaxHeight()) {
        TextButton(
            onClick = onFilesClicked,
            Modifier.width(40.dp)
        ) {
            Icon(Icons.AutoMirrored.Default.List, "")
        }
        TextButton(
            onClick = onSearchClicked,
            Modifier.width(40.dp)
        ) {
            Icon(Icons.Sharp.Search, "")
        }
        TextButton(
            onClick = {},
            Modifier.width(40.dp)
        ) {
            Icon(Icons.Filled.Edit, "")
        }

        Spacer(Modifier.weight(1f))

        TextButton(
            onClick = {},
            Modifier.width(40.dp)
        ) {
            Icon(Icons.Default.Menu, "")
        }
        TextButton(
            onClick = {},
            Modifier.width(40.dp)
        ) {
            Icon(Icons.Filled.Settings, "")

        }
    }

}

@Composable
fun FileList() {
    val vault = Vault("/home/marad/dendron/notes/")
    val files = vault.searchFiles("")

    val scrollState = rememberScrollState()

    var selected by remember { mutableStateOf("") }

    Column(
        Modifier.verticalScroll(scrollState)
            .padding(20.dp)
            .width(200.dp)
    ) {
        for (file in files) {
            Text(file, overflow = TextOverflow.Ellipsis, maxLines = 1,
                modifier = Modifier.selectable(selected == file, onClick = {
                    println("Selected $file")
                    selected = file
                }))

        }
    }
}

fun main() = application {
    val vaultPath = Path.of("").resolve("test-vault").toAbsolutePath().toString()

    val vault = Vault(vaultPath)
    val appVm = remember {
        AppViewModel(
            vault = vault,
            searchDialogViewModel = SearchDialogViewModel()
        )
    }
    val shortcuts = Shortcuts()

    val appState by appVm.state.collectAsState()

    val saveAction = createSaveAction(appState)
    val showSearchDialog = createSearchNoteAction(appVm)
    val showActionSearchDialog = createSearchActionsAction(appVm)
    val closeTabAction = createCloseTabAction(appState)
    val newNoteAction = newNoteAction(appVm)
    val deleteNoteAction = createDeleteAction(appVm)

    shortcuts.add(Shortcut(Key.P, KeyModifier.Ctrl), showSearchDialog)
    shortcuts.add(Shortcut(Key.P, KeyModifier.Ctrl, KeyModifier.Shift), showActionSearchDialog)
    shortcuts.add(Shortcut(Key.S, KeyModifier.Ctrl), saveAction)
    shortcuts.add(Shortcut(Key.W, KeyModifier.Ctrl), closeTabAction)
    shortcuts.add(Shortcut(Key.N, KeyModifier.Ctrl), newNoteAction)


    val appActions = listOf(saveAction, showSearchDialog, closeTabAction, newNoteAction, deleteNoteAction)


    appVm.searchDialogViewModel.onSearchActions = { name ->
        if (name.startsWith(">")) {
            val searchTerm = name.drop(1).trim()
            appActions.filter {
                it.name.contains(searchTerm) ||
                        (it.description?.contains(searchTerm) ?: false)
            }
        } else {
            vault.searchFiles(name).map {
                Action(it) {
                    appState.workspaceState.addTab(it, vault.pathToFile(it))
                }
            }
        }
    }

    Window(
        title = "Note Grove",
        state = WindowState(size = DpSize(1000.dp, 800.dp)),
        onPreviewKeyEvent = shortcuts::handle,
        onCloseRequest = ::exitApplication) {

        MaterialTheme(colors = lightColors(primary = Color(0.2f, 0.6f, 0.2f))) {
            Surface {
                App(appVm, onRequestCompletions = { tab, query ->
                    vault.searchFiles(query)
                })
            }

            InputDialog(appVm.inputDialogViewModel)
            SearchDialog(appVm.searchDialogViewModel)
            ConfirmDialog(appVm.confirmDialogViewModel)
        }
    }
}
