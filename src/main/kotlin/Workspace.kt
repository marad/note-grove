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

class WorkspaceState {
    val tabIndex = mutableIntStateOf(0)
    val tabs = mutableStateListOf<TabState>()

    fun addTab(title: String, content: String = "", activateNewTab: Boolean = true) {
        tabs.add(TabState(title, content))
        if (activateNewTab) {
            setActiveTab(tabs.size - 1)
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
    fun activeTabState() = tabs[tabIndex.value]

}

@Composable
@Preview
fun Workspace(state: WorkspaceState) {
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

            val tabState = state.activeTabState()
            Editor(tabState.editorState)
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

class TabState(title: String = "<unknown>", content: String = "") {
    val title = mutableStateOf(title)
    val editorState = EditorState(content)

    fun updateTitle(newTitle: String) {
        title.value = newTitle
    }
}


@Composable
fun WorkspaceTab(state: TabState,
                 selected: Boolean,
                 onClick: () -> Unit = {},
                 onClose: () -> Unit = {}) {
    Tab(
        text = {
            Row {
                Text(state.title.value, Modifier
                    .align(Alignment.CenterVertically)
                )
                if (state.editorState.isDirty()) {
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

