import java.nio.file.Files

fun createSaveAction(appState: AppState): Action =
    Action("Save", "Saves current file") {
        appState.workspaceState.activeTabState()?.let { tab ->
            val content = tab.editorState.content.value.text
            Files.write(tab.file, content.toByteArray())
            tab.editorState.clearDirty()
        }
    }