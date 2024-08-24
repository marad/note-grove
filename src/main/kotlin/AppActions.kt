import com.vladsch.flexmark.util.ast.NodeVisitor
import java.nio.file.Files

fun createSearchNoteAction(appVm: AppViewModel): Action =
    Action("Search note", "Shows search note dialog") {
        appVm.searchDialogViewModel.showWithPrefix("")
    }

fun createSearchActionsAction(appVm: AppViewModel): Action =
    Action("Search actions", "Shows search actions dialog") {
        appVm.searchDialogViewModel.showWithPrefix("> ")
    }

fun createCloseTabAction(appState: AppState): Action =
    Action("Close tab", "Closes current editor tab") {
        appState.workspaceState.closeActiveTab()
    }

fun createSaveAction(appState: AppState): Action =
    Action("Save", "Saves current file") {
        appState.workspaceState.activeTabState()?.let { tab ->
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
        val activeTab = appVm.state.value.workspaceState.activeTabState()
        val title = activeTab?.title?.value
        appVm.inputDialogViewModel.show(title ?: "") { title ->
            val path = appVm.vault.pathToFile(title)
            val content = """
                |---
                |title: $title
                |description: ''
                |created: ${System.currentTimeMillis()}
                |updated: ${System.currentTimeMillis()}
                |---
                |
                |
            """.trimMargin()
            appVm.state.value.workspaceState.addTab(title, path, defaultContent = content)
        }

    }


fun createDeleteAction(appVm: AppViewModel): Action =
    Action("Delete", "Deletes current file") {
        // get current note name
        val activeTab = appVm.state.value.workspaceState.activeTabState()
        val question = activeTab?.title?.value?.let {
            "Are you sure that you want to delete $it?"
        } ?: "Are you sure that you want to delete this note?"

        appVm.confirmDialogViewModel.show(
            question = question,
            onConfirm = {
                activeTab?.file?.let {
                    Files.deleteIfExists(it)
                    appVm.state.value.workspaceState.closeActiveTab()
                }
            },
            onCancel = {
                // do nothing
            }
        )

    }