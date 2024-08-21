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
import java.nio.file.Path


class AppState {
    val workspaceState = WorkspaceState()
    val searchDialogState = SearchDialogState()
}

@Composable
@Preview
fun App(state: AppState, onRequestCompletions: (tabState: TabState, query: String) -> List<String> = { _,_ -> emptyList() }) {

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

    val vault = Vault(vaultPath.toString())
    val appState = remember { AppState() }
    val shortcuts = Shortcuts()

    val saveAction = createSaveAction(appState)
    val showSearchDialog = createSearchNoteAction(appState)
    val showActionSearchDialog = createSearchActionsAction(appState)
    val closeTabAction = createCloseTabAction(appState)

    shortcuts.add(Shortcut(Key.P, KeyModifier.Ctrl), showSearchDialog)
    shortcuts.add(Shortcut(Key.P, KeyModifier.Ctrl, KeyModifier.Shift), showActionSearchDialog)
    shortcuts.add(Shortcut(Key.S, KeyModifier.Ctrl), saveAction)
    shortcuts.add(Shortcut(Key.W, KeyModifier.Ctrl), closeTabAction)

    val appActions = listOf(saveAction, showSearchDialog, closeTabAction)

    Window(
        title = "Note Grove",
        state = WindowState(size = DpSize(1000.dp, 800.dp)),
        onPreviewKeyEvent = shortcuts::handle,
        onCloseRequest = ::exitApplication) {

        MaterialTheme {
            App(appState, onRequestCompletions = { tab, query ->
                vault.searchFiles(query)
            })

            if (appState.searchDialogState.isVisible()) {
                SearchDialog(appState.searchDialogState,
                    onSearchActions = { name ->
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
                    })
            }
        }
    }
}
