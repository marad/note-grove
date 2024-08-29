import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import editor.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.notExists

class WorkspaceState {
    val tabIndex = mutableIntStateOf(0)
    val tabs = mutableStateListOf<TabState>()

    fun addTab(title: String, file: Path, activateNewTab: Boolean = true, defaultContent: String = "") {
        val existingTabIndex = tabIndex(file)
        if (existingTabIndex != null) {
            if (activateNewTab) {
                setActiveTab(existingTabIndex)
            }
        } else {
            tabs.add(TabState(title, file, defaultContent = defaultContent))
            if (activateNewTab) {
                setActiveTab(tabs.size - 1)
            }
        }
    }

    fun nextTab() { setActiveTab(tabIndex.value+1) }
    fun prevTab() { setActiveTab(tabIndex.value-1) }
    fun closeTab(index: Int) {
        tabs.removeAt(index)
        prevTab()
    }
    fun setActiveTab(index: Int) {
        tabIndex.value = index.coerceIn(0, (tabs.size-1).coerceAtLeast(0))
    }
    fun isTabActive(index: Int) = tabIndex.value == index

    fun closeActiveTab() {
        if (tabIndex.value in 0..<tabs.size) {
            closeTab(tabIndex.value)
        }
    }
    fun activeTabState() = tabs.getOrNull(tabIndex.value)
    fun findTab(file: Path): TabState? = tabs.firstOrNull { it.file == file }
    fun tabIndex(file: Path): Int? = tabs.indexOfFirst { it.file == file }?.let {
        if (it == -1) null else it
    }

}

@Composable
@Preview
fun Workspace(state: WorkspaceState, onRequestCompletions: (state: TabState, query: String) -> List<String> = { _,_ -> emptyList() }) {
    if (state.tabs.isNotEmpty()) {
        Column {
            ScrollableTabRow(
                state.tabIndex.value,
                Modifier.height(40.dp)
            ) {
                state.tabs.forEachIndexed { index, tabState ->
                    WorkspaceTab(tabState, state.isTabActive(index),
                        onClick = { state.setActiveTab(index) },
                        onClose = { state.closeTab(index) })
                }
            }

            val tabState = state.activeTabState()!!


            if (tabState.vimMode) {
                VimMode(tabState.vimModeViewModel,
                    Modifier.padding(10.dp),
                    onRequestCompletions = { onRequestCompletions(tabState, it) })
            } else {
                Editor(tabState.editorViewModel,
                    Modifier.padding(10.dp),
                    onRequestCompletions = { onRequestCompletions(tabState, it) })
            }
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxHeight()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Open or create a file with Ctrl+P",
                    color = Color.Gray,
                    fontSize = 18.sp
                )
            }
        }
    }
}

class TabState(title: String = "<unknown>", val file: Path, val vimMode: Boolean = false, defaultContent: String = "") {
    val title = mutableStateOf(title)
    val editorViewModel = EditorViewModel(readContentIfExists(defaultContent), file.notExists())
    val vimModeViewModel = VimModeViewModel(editorViewModel)

    fun updateTitle(newTitle: String) {
        title.value = newTitle
    }

    private fun readContentIfExists(defaultContent: String): String =
        if (file.exists()) {
            Files.readString(file)
        } else {
            defaultContent
        }
}


@Composable
fun WorkspaceTab(state: TabState,
                 selected: Boolean,
                 onClick: () -> Unit = {},
                 onClose: () -> Unit = {}) {

    val editorState by state.editorViewModel.state.collectAsState()

    Tab(
        text = {
            Row {
                Text(state.title.value, Modifier
                    .align(Alignment.CenterVertically)
                )
                if (editorState.dirty) {
                    Text("*", Modifier.align(Alignment.CenterVertically))
                }
                Spacer(Modifier.width(10.dp))
                if (selected) {
                    Icon(
                        Icons.Default.Close, "",
                        Modifier.align(Alignment.CenterVertically)
                            .clickable(onClick = onClose)
                    )
                }
            }
        },
        selected = selected,
        onClick = onClick,
    )
}

