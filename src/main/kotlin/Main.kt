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
import kotlin.io.path.nameWithoutExtension
import kotlin.system.exitProcess


data class AppState(
    val roots: List<RootState>,
    val activeRootIndex: Int = 0,
)  {
    val currentRootName get() =  roots[activeRootIndex].name
    val workspace get() =  roots[activeRootIndex].workspace
    val root get() =  roots[activeRootIndex].root

    init {
        assert(roots.isNotEmpty()) { "At least one root must be provided" }
    }
}

data class RootState(
    val name: String,
    val root: Root,
    val workspace: WorkspaceViewModel = WorkspaceViewModel()
)

class AppViewModel(
    appConfig: AppConfig,
    val actionLauncherViewModel: LauncherViewModel = LauncherViewModel(),
) : ViewModel() {
    private val _state = MutableStateFlow(AppState(
        roots = appConfig.roots.map { RootState(it.name, Root(it.path)) }
    ))
    val state = _state.asStateFlow()
    val windowState = WindowState(size = DpSize(1000.dp, 800.dp))

    fun cycleRoots() {
        _state.value = _state.value.copy(activeRootIndex = (_state.value.activeRootIndex + 1) % _state.value.roots.size)
    }

    fun selectRoot(root: Root) {
        val index = _state.value.roots.indexOfFirst { it.root == root }
        if (index != -1) {
            _state.value = _state.value.copy(activeRootIndex = index)
        }
    }

    fun openFile(path: Path, templateName: String = "templates.note") {
        val root = state.value.root
        val title = path.nameWithoutExtension
        val defaultContent = Templates.newNote(root, title, templateName)
        state.value.workspace.addTab(path, defaultContent)
    }
}

@Composable
@Preview
fun App(appVm: AppViewModel, onRequestCompletions: (tabViewModel: TabViewModel, query: String) -> List<String> = { _, _ -> emptyList() }) {

    val state by appVm.state.collectAsState()

    Row(Modifier.fillMaxSize()) {
        ToolBar(
            onFilesClicked = {
                state.workspace.addTab(Path.of("/tmp/testfile.md"))
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
        )
    }
    val shortcuts = Shortcuts()
    val appActions = mutableListOf<Action>()

    val appState by appVm.state.collectAsState()

    val saveAction = createSaveAction(appVm)
    val closeTabAction = createCloseTabAction(appVm)
    val newNoteAction = newNoteAction(appVm)
    val deleteNoteAction = createDeleteAction(appVm)
    val renameNoteAction = createRenameNoteAction(appVm)
    val selectRootAction = createSelectRootAction(appVm)
    val cycleRootAction = createCycleRootAction(appVm)
    val followLinkAction = createFollowLinkAction(appVm)
    val showNoteSearchDialog = createSearchNoteAction(appVm, appActions)
    val showActionSearchDialog = createSearchActionsAction(appVm, appActions)
    val openDailyNote = createOpenDailyNoteAction(appVm)
    val previousDailyNote = createPreviousDailyNoteAction(appVm)
    val nextDailyNote = createNextDailyNoteAction(appVm)
    val openWeeklyNote = createOpenWeeklyNoteAction(appVm)
    val previousWeeklyNote = createPreviousWeeklyNoteAction(appVm)
    val nextWeeklyNote = createNextWeeklyNoteAction(appVm)

    appActions.addAll(listOf(
        saveAction, closeTabAction, newNoteAction, deleteNoteAction, renameNoteAction, selectRootAction,
        cycleRootAction, createRefactorHierarchyAction(appVm), followLinkAction, showNoteSearchDialog,
        showActionSearchDialog, openDailyNote, previousDailyNote, nextDailyNote,
        openWeeklyNote, previousWeeklyNote, nextWeeklyNote
    ))

    appActions.sortBy { it.name }

    shortcuts.add(Shortcut(Key.S, KeyModifier.Ctrl), saveAction)
    shortcuts.add(Shortcut(Key.W, KeyModifier.Ctrl), closeTabAction)
    shortcuts.add(Shortcut(Key.N, KeyModifier.Ctrl), newNoteAction)
    shortcuts.add(Shortcut(Key.R, KeyModifier.Ctrl, KeyModifier.Shift), selectRootAction)
    shortcuts.add(Shortcut(Key.R, KeyModifier.Ctrl), cycleRootAction)
    shortcuts.add(Shortcut(Key.G, KeyModifier.Ctrl), followLinkAction)
    shortcuts.add(Shortcut(Key.P, KeyModifier.Ctrl), showNoteSearchDialog)
    shortcuts.add(Shortcut(Key.P, KeyModifier.Ctrl, KeyModifier.Shift), showActionSearchDialog)
    shortcuts.add(Shortcut(Key.D, KeyModifier.Ctrl), openDailyNote)
    shortcuts.add(Shortcut(Key.U, KeyModifier.Ctrl), previousDailyNote)
    shortcuts.add(Shortcut(Key.I, KeyModifier.Ctrl), nextDailyNote)


    Window(
        title = "Note Grove - ${appState.currentRootName}",
        state = appVm.windowState,
        onPreviewKeyEvent = shortcuts::handle,
        onCloseRequest = ::exitApplication) {

        MaterialTheme(colors = lightColors(primary = Color(0.2f, 0.6f, 0.2f))) {
            Surface {
                App(appVm, onRequestCompletions = { tab, query ->
                    val root = appVm.state.value.root
                    root.searchFiles(query)
                })
            }

            ActionLauncherDialog(appVm.actionLauncherViewModel)
        }
    }
}
