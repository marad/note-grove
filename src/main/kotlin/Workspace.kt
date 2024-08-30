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
import androidx.lifecycle.ViewModel
import editor.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.notExists

data class WorkspaceState(val tabIndex: Int = 0, val vimMode: Boolean = false)

class WorkspaceViewModel : ViewModel() {
    private val _state = MutableStateFlow(WorkspaceState())
    val state = _state.asStateFlow()
    val tabs = mutableStateListOf<TabViewModel>()

    fun addTab(file: Path, content: String = "", activateNewTab: Boolean = true) {
        val existingTabIndex = tabIndex(file)
        if (existingTabIndex != null) {
            if (activateNewTab) {
                setActiveTab(existingTabIndex)
            }
        } else {
            tabs.add(TabViewModel(file, defaultContent = content))
            if (activateNewTab) {
                setActiveTab(tabs.size - 1)
            }
        }
    }

    fun updateTabFile(oldFile: Path, newFile: Path) {
        findTab(oldFile)?.updateFile(newFile)
    }

    fun nextTab() { setActiveTab(state.value.tabIndex+1) }
    fun prevTab() { setActiveTab(state.value.tabIndex-1) }
    fun closeTab(index: Int) {
        tabs.removeAt(index)
        prevTab()
    }
    fun setActiveTab(index: Int) {
        _state.value = _state.value.copy(
            tabIndex = index.coerceIn(0, (tabs.size-1).coerceAtLeast(0))
        )
    }
    fun isTabActive(index: Int) = state.value.tabIndex == index

    fun closeActiveTab() {
        if (state.value.tabIndex in 0..<tabs.size) {
            closeTab(state.value.tabIndex)
        }
    }
    fun activeTab(): TabViewModel? = tabs.getOrNull(state.value.tabIndex)
    fun findTab(file: Path): TabViewModel? = tabs.firstOrNull { it.state.value.file == file }
    fun tabIndex(file: Path): Int? = tabs.indexOfFirst { it.state.value.file == file }.let {
        if (it == -1) null else it
    }


}

@Composable
@Preview
fun Workspace(vm: WorkspaceViewModel, onRequestCompletions: (state: TabViewModel, query: String) -> List<String> = { _,_ -> emptyList() }) {
    val state by vm.state.collectAsState()

    if (vm.tabs.isNotEmpty()) {
        Column {
            ScrollableTabRow(
                state.tabIndex,
                Modifier.height(40.dp)
            ) {
                vm.tabs.forEachIndexed { index, tabState ->
                    WorkspaceTab(tabState, vm.isTabActive(index),
                        onClick = { vm.setActiveTab(index) },
                        onClose = { vm.closeTab(index) })
                }
            }

            val tabState = vm.activeTab()!!


            if (state.vimMode) {
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

class TabState(val file: Path, val vimMode: Boolean = false) {
    val title = file.nameWithoutExtension
}

class TabViewModel(path: Path, vimMode: Boolean = false, defaultContent: String = "") : ViewModel() {
    private val _state = MutableStateFlow(TabState(path, vimMode))
    val state = _state.asStateFlow()
    val editorViewModel = EditorViewModel(readContentIfExists(defaultContent), _state.value.file.notExists())
    val vimModeViewModel = VimModeViewModel(editorViewModel)

    val title get() = state.value.title
    val path get() = state.value.file

    fun updateFile(newFile: Path) {
        _state.value = TabState(newFile, _state.value.vimMode)
    }

    private fun readContentIfExists(defaultContent: String): String =
        if (_state.value.file.exists()) {
            Files.readString(_state.value.file)
        } else {
            defaultContent
        }
}


@Composable
fun WorkspaceTab(tabVm: TabViewModel,
                 selected: Boolean,
                 onClick: () -> Unit = {},
                 onClose: () -> Unit = {}) {

    val state by tabVm.state.collectAsState()
    val editorState by tabVm.editorViewModel.state.collectAsState()

    Tab(
        text = {
            Row {
                Text(state.title, Modifier
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

