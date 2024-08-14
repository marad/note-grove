import java.nio.file.Files

fun createSearchNoteAction(appState: AppState): Action =
    Action("Search note", "Shows search note dialog") {
        appState.searchDialogState.showWithPrefix("")
    }

fun createSearchActionsAction(appState: AppState): Action =
    Action("Search actions", "Shows search actions dialog") {
        appState.searchDialogState.showWithPrefix("> ")
    }

fun createCloseTabAction(appState: AppState): Action =
    Action("Close tab", "Closes current editor tab") {
        appState.workspaceState.closeActiveTab()
    }

fun createSaveAction(appState: AppState): Action =
    Action("Save", "Saves current file") {
        appState.workspaceState.activeTabState()?.let { tab ->
            val content = tab.editorState.content.value.text
            Files.write(tab.file, content.toByteArray())
            tab.editorState.clearDirty()
        }
    }
