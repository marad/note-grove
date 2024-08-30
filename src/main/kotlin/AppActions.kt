import com.vladsch.flexmark.util.ast.NodeVisitor
import files.internal.MatchingStrategy
import java.nio.file.Files
import java.time.LocalDate
import kotlin.io.path.nameWithoutExtension

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

fun createCloseTabAction(appVm: AppViewModel): Action =
    Action("Close tab", "Closes current editor tab") {
        appVm.state.value.workspace.closeActiveTab()
    }

fun createSaveAction(appVm: AppViewModel): Action =
    Action("Save", "Saves current file") {
        appVm.state.value.workspace.activeTab()?.let { tab ->
            val content = tab.editorViewModel.content.text

            val md = Markdown.parse(content)
            val visitor = NodeVisitor(
                Markdown.updateYamlFrontmatterVisitHandler("updated",
                    System.currentTimeMillis().toString())
            )
            visitor.visit(md)

            val updatedContent = md.chars.toString()
            tab.editorViewModel.updateContent(updatedContent)

            Files.write(tab.path, updatedContent.toByteArray())
            tab.editorViewModel.clearDirty()
        }
    }


fun newNoteAction(appVm: AppViewModel): Action =
    Action("New note", "Creates a new note") {
        val activeTab = appVm.state.value.workspace.activeTab()
        val title = activeTab?.title
        appVm.actionLauncherViewModel.showInput(initialQuery = title ?: "") { fileName ->
            val root = appVm.state.value.root

            val path = root.pathToFile(fileName)
            val title = fileName.split(".").last()
            val content = Templates.newNote(root, title, "templates.note")
            appVm.state.value.workspace.addTab(path, content)
        }

    }


fun createDeleteAction(appVm: AppViewModel): Action =
    Action("Delete", "Deletes current file") {
        // get current note name
        val activeTab = appVm.state.value.workspace.activeTab()
        val question = activeTab?.title?.let {
            "Are you sure that you want to delete $it?"
        } ?: "Are you sure that you want to delete this note?"

        appVm.actionLauncherViewModel.showConfirm(
            onConfirm = {
                activeTab?.path?.let {
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
        val activeTab = appVm.state.value.workspace.activeTab()
        if (activeTab != null) {
            val title = activeTab.title
            appVm.actionLauncherViewModel.showInput(initialQuery = title) { newTitle ->
                val root = appVm.state.value.root
                val path = root.pathToFile(newTitle)
                val content = activeTab.editorViewModel.content.text
                Files.write(path, content.toByteArray())
                activeTab.path.let { Files.delete(it) }
                appVm.state.value.workspace.updateTabFile(activeTab.path, path)
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
        val tabState = appVm.state.value.workspace.activeTab()
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
                                    appVm.state.value.workspace.updateTabFile(oldPath, newPath)
                                }
                            }
                        }
                    }
                }
            }
        }
    }


fun createFollowLinkAction(appVm: AppViewModel): Action {
    return Action("Follow link", "Follows link under cursor") {
        val activeTab = appVm.state.value.workspace.activeTab()
        val editorViewModel = activeTab?.editorViewModel
        val content = editorViewModel?.content?.text ?: ""
        val cursor = editorViewModel?.state?.value?.content?.selection?.start ?: 0

        val link = Markdown.findLink(content, cursor)
        val path = appVm.state.value.root.pathToFile(link)
        if (Files.exists(path)) {
            appVm.state.value.workspace.addTab(path)
        }
    }
}

fun createOpenDailyNoteAction(appVm: AppViewModel): Action =
    Action("Open daily journal note") {
        val root = appVm.state.value.root
        val path = root.pathToFile(Journal.todaysDailyNote())
        appVm.openFile(path, "templates.daily")
    }

fun createPreviousDailyNoteAction(appVm: AppViewModel): Action =
    Action("Open previous daily journal note") {
        val root = appVm.state.value.root
        val activeTab = appVm.state.value.workspace.activeTab()
        val currentTitle = activeTab?.path?.nameWithoutExtension
        val date = currentTitle?.let(Journal::getJournalDate) ?: LocalDate.now()
        val previousNote = Journal.previousDailyNote(root, date)
        if (previousNote != null) {
            appVm.openFile(previousNote, "templates.daily")
        }
    }

fun createNextDailyNoteAction(appVm: AppViewModel): Action =
    Action("Open next daily journal note") {
        val root = appVm.state.value.root
        val activeTab = appVm.state.value.workspace.activeTab()
        val currentTitle = activeTab?.path?.nameWithoutExtension
        val date = currentTitle?.let(Journal::getJournalDate) ?: LocalDate.now()
        val nextNote = Journal.nextDailyNote(root, date)
        if (nextNote != null) {
            appVm.openFile(nextNote, "templates.daily")
        } else {
            val nextDay = date.plusDays(1)
            val noteName = Journal.formatJournalNoteName(nextDay)
            val path = root.pathToFile(noteName)
            appVm.openFile(path, "templates.daily")
        }
    }


fun createOpenWeeklyNoteAction(appVm: AppViewModel): Action =
    Action("Open weekly note") {
        val root = appVm.state.value.root
        val path = root.pathToFile(Weekly.getCurrentWeeklyNote())
        appVm.openFile(path, "templates.weekly")
    }

fun createPreviousWeeklyNoteAction(appVm: AppViewModel): Action =
    Action("Open previous weekly note") {
        val root = appVm.state.value.root
        val activeTab = appVm.state.value.workspace.activeTab()
        val currentTitle = activeTab?.path?.nameWithoutExtension
        currentTitle?.let { Weekly.getWeekAndYear(it) }?.let {
            val (week, year) = it
            val previousNote = Weekly.getPreviousWeeklyNote(root, week, year)
            if (previousNote != null) {
                appVm.openFile(previousNote, "templates.weekly")
            }
        }
    }

fun createNextWeeklyNoteAction(appVm: AppViewModel): Action =
    Action("Open next weekly note") {
        val root = appVm.state.value.root
        val activeTab = appVm.state.value.workspace.activeTab()
        val currentTitle = activeTab?.path?.nameWithoutExtension
        currentTitle?.let { Weekly.getWeekAndYear(it) }?.let {
            val (week, year) = it
            val nextNote = Weekly.getNextWeeklyNote(root, week, year)
            if (nextNote != null) {
                appVm.openFile(nextNote, "templates.weekly")
            } else {
                val nextWeekFirstDay = Weekly.getFirstDayOfWeek(week, year).plusWeeks(1)
                val noteName = Weekly.formatWeeklyNoteName(Weekly.getWeek(nextWeekFirstDay), nextWeekFirstDay.year)
                val path = root.pathToFile(noteName)
                appVm.openFile(path, "templates.weekly")
            }
        }
    }