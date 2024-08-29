import com.vladsch.flexmark.util.ast.NodeVisitor
import files.internal.MatchingStrategy
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
                appVm.state.value.workspace.addTab(root.pathToFile(it))
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
        val title = activeTab?.title
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
            appVm.state.value.workspace.addTab(path, defaultContent = content)
        }

    }


fun createDeleteAction(appVm: AppViewModel): Action =
    Action("Delete", "Deletes current file") {
        // get current note name
        val activeTab = appVm.state.value.workspace.activeTabState()
        val question = activeTab?.title?.let {
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
            val title = activeTab.title
            appVm.actionLauncherViewModel.showInput(initialQuery = title) { newTitle ->
                val root = appVm.state.value.root
                val path = root.pathToFile(newTitle)
                val content = activeTab.editorViewModel.content.text
                Files.write(path, content.toByteArray())
                activeTab.file.let { Files.delete(it) }
                appVm.state.value.workspace.closeTab(appVm.state.value.workspace.tabIndex.value)
                appVm.state.value.workspace.addTab(path, defaultContent = content)
            }
        }
    }

fun createSelectRootAction(appVm: AppViewModel): Action =
    Action("Select root", "Shows dialog to switch active root") {
        val state = appVm.state.value
        appVm.actionLauncherViewModel.show("") { query ->
            val roots = state.roots
            roots.filter { it.name.contains(query, ignoreCase = true) }
                .map { rootInfo ->
                    Action(rootInfo.name, "Switch to ${rootInfo.name}") {
                        appVm.selectRoot(rootInfo.root)
                    }
                }
        }
    }

fun createCycleRootAction(appVm: AppViewModel): Action =
    Action("Cycle roots", "Cycles through all opened roots") {
        appVm.cycleRoots()
    }

fun createRefactorHierarchyAction(appVm: AppViewModel): Action =
    Action("Refactor hierarchy", "Changes name for multiple files at once") {
        val tabState = appVm.state.value.workspace.activeTabState()
        val name = tabState?.title ?: ""
        val root = appVm.state.value.root
        // show input dialog to get the source pattern
        appVm.actionLauncherViewModel.show(initialQuery = name) { srcPattern ->
            val files = root.searchFiles(srcPattern, MatchingStrategy::contains)
            files.map { file ->
                Action(file) {
                    // show input dialog to get the destination pattern
                    appVm.actionLauncherViewModel.show { dstPattern ->
                        files.map {
                            Action("$it > ${it.replace(srcPattern, dstPattern)}") {
                                // rename selected files
                                files.forEach { oldFileName ->
                                    val newFileName = oldFileName.replace(srcPattern, dstPattern)
                                    val oldPath = root.pathToFile(oldFileName)
                                    val newPath = root.pathToFile(newFileName)
                                    Files.move(oldPath, newPath)
                                    // TODO: update tab title and path
                                }
                            }
                        }
                    }
                }
            }
        }
    }