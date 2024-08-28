import com.vladsch.flexmark.util.ast.NodeVisitor
import java.nio.file.Files

private fun searchActions(appVm: AppViewModel, appActions: List<Action>, name: String): List<Action> {
    return if (name.startsWith(">")) {
        val searchTerm = name.drop(1).trim()
        appActions.filter {
            it.name.contains(searchTerm, ignoreCase = true) ||
                    (it.description?.contains(searchTerm, ignoreCase = true) ?: false)
        }
    } else {
        val root = appVm.state.value.root
        root.searchFiles(name).map {
            Action(it) {
                appVm.state.value.workspace.addTab(it, root.pathToFile(it))
            }
        }
    }
}

fun createSearchNoteAction(appVm: AppViewModel, appActions: List<Action>): Action =
    Action("Search note", "Shows search note dialog") {
        appVm.actionLauncherViewModel.show("") {
            searchActions(appVm, appActions, it)
        }
    }

fun createSearchActionsAction(appVm: AppViewModel, appActions: List<Action>): Action =
    Action("Search actions", "Shows search actions dialog") {
        appVm.actionLauncherViewModel.show("> ") {
            searchActions(appVm, appActions, it)
        }
    }

fun createCloseTabAction(appState: AppState): Action =
    Action("Close tab", "Closes current editor tab") {
        appState.workspace.closeActiveTab()
    }

fun createSaveAction(appState: AppState): Action =
    Action("Save", "Saves current file") {
        appState.workspace.activeTabState()?.let { tab ->
            val content = tab.editorViewModel.content.text

            val md = Markdown.parse(content)
            val visitor = NodeVisitor(
                Markdown.updateYamlFrontmatterVisitHandler("updated",
                    System.currentTimeMillis().toString())
            )
            visitor.visit(md)

            val updatedContent = md.chars.toString()
            tab.editorViewModel.updateContent(updatedContent)

            Files.write(tab.file, updatedContent.toByteArray())
            tab.editorViewModel.clearDirty()
        }
    }


fun newNoteAction(appVm: AppViewModel): Action =
    Action("New note", "Creates a new note") {
        val activeTab = appVm.state.value.workspace.activeTabState()
        val title = activeTab?.title?.value
        appVm.actionLauncherViewModel.showInput(initialQuery = title ?: "") { fileName ->
            val root = appVm.state.value.root

            val path = root.pathToFile(fileName)
            val content = """
                |---
                |title: ${fileName.split(".").last()}
                |description: ''
                |created: ${System.currentTimeMillis()}
                |updated: ${System.currentTimeMillis()}
                |---
                |
                |
            """.trimMargin()
            appVm.state.value.workspace.addTab(fileName, path, defaultContent = content)
        }

    }


fun createDeleteAction(appVm: AppViewModel): Action =
    Action("Delete", "Deletes current file") {
        // get current note name
        val activeTab = appVm.state.value.workspace.activeTabState()
        val question = activeTab?.title?.value?.let {
            "Are you sure that you want to delete $it?"
        } ?: "Are you sure that you want to delete this note?"

        appVm.actionLauncherViewModel.showConfirm(
            onConfirm = {
                activeTab?.file?.let {
                    Files.deleteIfExists(it)
                    appVm.state.value.workspace.closeActiveTab()
                }
            },
            onCancel = {
                // do nothing
            }
        )

    }

fun createRenameNoteAction(appVm: AppViewModel): Action =
    Action("Rename", "Renames current file") {
        val activeTab = appVm.state.value.workspace.activeTabState()
        if (activeTab != null) {
            val title = activeTab.title.value
            appVm.actionLauncherViewModel.showInput(initialQuery = title) { newTitle ->
                val root = appVm.state.value.root
                val path = root.pathToFile(newTitle)
                val content = activeTab.editorViewModel?.content?.text ?: ""
                Files.write(path, content.toByteArray())
                activeTab.file.let { Files.delete(it) }
                appVm.state.value.workspace.closeTab(appVm.state.value.workspace.tabIndex.value)
                appVm.state.value.workspace.addTab(newTitle, path, defaultContent = content)
            }
        }
    }