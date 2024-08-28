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
import config.AppConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.file.Path
import kotlin.system.exitProcess


data class AppState(
    val roots: List<RootState>,
    val activeRootIndex: Int = 0,
)  {
    val name = roots[activeRootIndex].name
    val workspace = roots[activeRootIndex].workspace
    val root = roots[activeRootIndex].root

    init {
        assert(roots.isNotEmpty()) { "At least one root must be provided" }
    }
}

data class RootState(
    val name: String,
    val root: Root,
    val workspace: WorkspaceState = WorkspaceState()
)

class AppViewModel(
    appConfig: AppConfig,
    val inputDialogViewModel: InputDialogViewModel = InputDialogViewModel(),
    val searchDialogViewModel: SearchDialogViewModel = SearchDialogViewModel(),
    val confirmDialogViewModel: ConfirmDialogViewModel = ConfirmDialogViewModel(),
) : ViewModel() {
    private val _state = MutableStateFlow(AppState(
        roots = appConfig.roots.map { RootState(it.name, Root(it.path)) }
    ))
    val state = _state.asStateFlow()

    fun toggleRoot() {
        _state.value = _state.value.copy(activeRootIndex = (_state.value.activeRootIndex + 1) % _state.value.roots.size)
    }
}

@Composable
@Preview
fun App(appVm: AppViewModel, onRequestCompletions: (tabState: TabState, query: String) -> List<String> = { _,_ -> emptyList() }) {

    val state by appVm.state.collectAsState()

    Row(Modifier.fillMaxSize()) {
        ToolBar(
            onFilesClicked = {
                println("Adding tab")
                state.workspace.addTab("testing", Path.of("/tmp/testfile.md"))
            }
        )
        //FileList()

        Divider(
            Modifier
                .fillMaxHeight()
                .width(2.dp),
            color = Color.LightGray,
        )

        Workspace(state.workspace, onRequestCompletions = onRequestCompletions)
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
    val root = Root("/home/marad/dendron/notes/")
    val files = root.searchFiles("")

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
    val currentDir = Path.of("")
    val config = AppConfig.load(currentDir.resolve("config.toml"))
    if (config == null) {
        println("Config not found at ${currentDir.resolve("config.toml")}")
        exitProcess(1)
    }

    val appVm = remember {
        AppViewModel(
            appConfig = config,
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
    val renameNoteAction = createRenameNoteAction(appVm)

    shortcuts.add(Shortcut(Key.P, KeyModifier.Ctrl), showSearchDialog)
    shortcuts.add(Shortcut(Key.P, KeyModifier.Ctrl, KeyModifier.Shift), showActionSearchDialog)
    shortcuts.add(Shortcut(Key.S, KeyModifier.Ctrl), saveAction)
    shortcuts.add(Shortcut(Key.W, KeyModifier.Ctrl), closeTabAction)
    shortcuts.add(Shortcut(Key.N, KeyModifier.Ctrl), newNoteAction)


    val appActions = listOf(
        saveAction, showSearchDialog, closeTabAction, newNoteAction, deleteNoteAction, renameNoteAction,
        Action("Toggle root", "") {
            appVm.toggleRoot()
        }
    )


    appVm.searchDialogViewModel.onSearchActions = { name ->
        if (name.startsWith(">")) {
            val searchTerm = name.drop(1).trim()
            appActions.filter {
                it.name.contains(searchTerm, ignoreCase = true) ||
                        (it.description?.contains(searchTerm, ignoreCase = true) ?: false)
            }
        } else {
            val root = appVm.state.value.root
            root.searchFiles(name).map {
                Action(it) {
                    appState.workspace.addTab(it, root.pathToFile(it))
                }
            }
        }
    }


    Window(
        title = "Note Grove - ${appState.name}",
        state = WindowState(size = DpSize(1000.dp, 800.dp)),
        onPreviewKeyEvent = shortcuts::handle,
        onCloseRequest = ::exitApplication) {

        MaterialTheme(colors = lightColors(primary = Color(0.2f, 0.6f, 0.2f))) {
            Surface {
                App(appVm, onRequestCompletions = { tab, query ->
                    val root = appVm.state.value.root
                    root.searchFiles(query)
                })
            }

            InputDialog(appVm.inputDialogViewModel)
            SearchDialog(appVm.searchDialogViewModel)
            ConfirmDialog(appVm.confirmDialogViewModel)
        }
    }
}
