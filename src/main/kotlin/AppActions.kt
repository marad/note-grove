import com.vladsch.flexmark.util.ast.NodeVisitor
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
            val content = tab.editorState.getContent().text

            val md = Markdown.parse(content)
            val visitor = NodeVisitor(
                Markdown.updateYamlFrontmatterVisitHandler("updated",
                    System.currentTimeMillis().toString())
            )
            visitor.visit(md)

            val updatedContent = Markdown.render(md)
            tab.editorState.updateContent(updatedContent)

            Files.write(tab.file, updatedContent.toByteArray())
            tab.editorState.clearDirty()
        }
    }
