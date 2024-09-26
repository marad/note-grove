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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import androidx.lifecycle.ViewModel
import config.AppConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import window.NoteWindow
import window.NoteWindowViewModel
import java.nio.file.Path
import kotlin.system.exitProcess


data class AppState(
    val windows: List<NoteWindowViewModel>,
) {
    init {
        assert(windows.isNotEmpty()) { "At least one window must be provided" }
    }
}

data class RootState(
    val name: String,
    val root: Root,
    val workspace: WorkspaceViewModel = WorkspaceViewModel()
)

class AppViewModel(
    val appConfig: AppConfig,
    val actionLauncherViewModel: LauncherViewModel = LauncherViewModel(),
) : ViewModel() {
    private val _state = MutableStateFlow(AppState(
        windows = listOf(NoteWindowViewModel(appConfig.roots))
    ))
    val state = _state.asStateFlow()

    fun newWindow() {
        _state.value = _state.value.copy(
            windows = _state.value.windows + NoteWindowViewModel(appConfig.roots)
        )
    }

    fun closeWindow(windowViewModel: NoteWindowViewModel) {
        val windows = _state.value.windows.toMutableList()
        windows.remove(windowViewModel)
        _state.value = _state.value.copy(windows = windows)
    }

    fun hasWindows() = state.value.windows.isNotEmpty()
}

@Composable
@Preview
fun App(appVm: NoteWindowViewModel, onRequestCompletions: (tabViewModel: TabViewModel, query: String) -> List<String> = { _, _ -> emptyList() }) {

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

    var selected by remember { mutableStateOf(NoteName("")) }

    Column(
        Modifier.verticalScroll(scrollState)
            .padding(20.dp)
            .width(200.dp)
    ) {
        for (file in files) {
            Text(file.name, overflow = TextOverflow.Ellipsis, maxLines = 1,
                modifier = Modifier.selectable(selected == file, onClick = {
                    selected = file
                }))

        }
    }
}

fun main() = application {
    val appVm = remember {
        val currentDir = Path.of("")
        val config = AppConfig.load(currentDir.resolve("config.toml"))
        if (config == null) {
            println("Config not found at ${currentDir.resolve("config.toml")}")
            exitProcess(1)
        }
        AppViewModel(
            appConfig = config,
        )
    }

    val appState by appVm.state.collectAsState()

    appState.windows.forEach { windowVm ->
        NoteWindow(windowVm,
            onCloseRequest = {
                appVm.closeWindow(windowVm)
                if (!appVm.hasWindows()) {
                    exitApplication()
                }
            },
            newWindowRequested = {
                appVm.newWindow()
            }
        )
    }
}

