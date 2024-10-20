import androidx.compose.ui.input.key.Key
import com.vladsch.flexmark.util.ast.NodeVisitor
import files.internal.MatchingStrategy
import tools.rg.Match
import window.NoteWindowViewModel
import java.awt.Desktop
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import kotlin.io.path.nameWithoutExtension

fun prepareActionsAndShortcuts(windowVm: NoteWindowViewModel): Shortcuts {
    val shortcuts = Shortcuts()
    val appActions = mutableListOf<Action>()


    val saveAction = createSaveAction(windowVm)
    val closeTabAction = createCloseTabAction(windowVm)
    val newNoteAction = newNoteAction(windowVm)
    val deleteNoteAction = createDeleteAction(windowVm)
    val renameNoteAction = createRenameNoteAction(windowVm)
    val selectRootAction = createSelectRootAction(windowVm)
    val cycleRootAction = createCycleRootAction(windowVm)
    val followLinkAction = createFollowLinkAction(windowVm)
    val showNoteSearchDialog = createSearchNoteAction(windowVm, appActions)
    val showActionSearchDialog = createSearchActionsAction(windowVm, appActions)
    val openDailyNote = createOpenDailyNoteAction(windowVm)
    val previousDailyNote = createPreviousDailyNoteAction(windowVm)
    val nextDailyNote = createNextDailyNoteAction(windowVm)
    val openWeeklyNote = createOpenWeeklyNoteAction(windowVm)
    val previousWeeklyNote = createPreviousWeeklyNoteAction(windowVm)
    val nextWeeklyNote = createNextWeeklyNoteAction(windowVm)
    val insertTemplate = createInsertTemplateAction(windowVm)
    val jumpToBacklink = createJumpToBacklinkAction(windowVm)
    val searchPhrase = createSearchPhraseAction(windowVm)

    appActions.addAll(listOf(
        saveAction, closeTabAction, newNoteAction, deleteNoteAction, renameNoteAction, selectRootAction,
        cycleRootAction, createRefactorHierarchyAction(windowVm), followLinkAction, showNoteSearchDialog,
        showActionSearchDialog, openDailyNote, previousDailyNote, nextDailyNote,
        openWeeklyNote, previousWeeklyNote, nextWeeklyNote, insertTemplate, jumpToBacklink, searchPhrase
    ))

    appActions.sortBy { it.name }

    shortcuts.add(Shortcut(Key.S, KeyModifier.Ctrl), saveAction)
    shortcuts.add(Shortcut(Key.W, KeyModifier.Ctrl), closeTabAction)
    shortcuts.add(Shortcut(Key.N, KeyModifier.Ctrl), newNoteAction)
    shortcuts.add(Shortcut(Key.R, KeyModifier.Ctrl, KeyModifier.Shift), selectRootAction)
    shortcuts.add(Shortcut(Key.R, KeyModifier.Ctrl), cycleRootAction)
    shortcuts.add(Shortcut(Key.G, KeyModifier.Ctrl), followLinkAction)
    shortcuts.add(Shortcut(Key.P, KeyModifier.Ctrl), showNoteSearchDialog)
    shortcuts.add(Shortcut(Key.P, KeyModifier.Ctrl, KeyModifier.Shift), showActionSearchDialog)
    shortcuts.add(Shortcut(Key.D, KeyModifier.Ctrl), openDailyNote)
    shortcuts.add(Shortcut(Key.U, KeyModifier.Ctrl), previousDailyNote)
    shortcuts.add(Shortcut(Key.I, KeyModifier.Ctrl), nextDailyNote)
    shortcuts.add(Shortcut(Key.F, KeyModifier.Ctrl, KeyModifier.Shift), searchPhrase)

    return shortcuts
}

private fun searchActions(appVm: NoteWindowViewModel, appActions: List<Action>, name: String): List<Action> {
    return if (name.startsWith(">")) {
        val searchTerm = name.drop(1).trim()
        appActions.filter {
            MatchingStrategy.fuzzy(it.name, searchTerm) ||
                    (it.description?.let { MatchingStrategy.fuzzy(it, searchTerm) } == true)
        }
    } else {
        val root = appVm.state.value.root
        root.searchFiles(name, MatchingStrategy::fuzzy).map {
            Action(it.name) {
                appVm.openNote(it)
            }
        }
    }
}

fun createSearchNoteAction(appVm: NoteWindowViewModel, appActions: List<Action>): Action =
    Action("Search note", "Shows search note dialog") {
        val currentNoteTitle = appVm.state.value.workspace.activeTab()?.title
        appVm.actionLauncherViewModel.show(currentNoteTitle, selectText = currentNoteTitle != null, forceAccept = { name ->
            appVm.openNote(NoteName(name))
        }) {
            searchActions(appVm, appActions, it)
        }
    }

fun createSearchActionsAction(appVm: NoteWindowViewModel, appActions: List<Action>): Action =
    Action("Search actions", "Shows search actions dialog") {
        appVm.actionLauncherViewModel.show("> ") {
            searchActions(appVm, appActions, it)
        }
    }

fun createCloseTabAction(appVm: NoteWindowViewModel): Action =
    Action("Close tab", "Closes current editor tab") {
        appVm.state.value.workspace.closeActiveTab()
    }

fun createSaveAction(appVm: NoteWindowViewModel): Action =
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


fun newNoteAction(appVm: NoteWindowViewModel): Action =
    Action("New note", "Creates a new note") {
        val activeTab = appVm.state.value.workspace.activeTab()
        val title = activeTab?.title
        appVm.actionLauncherViewModel.showInput(initialQuery = title ?: "") { fileName ->
            val root = appVm.state.value.root

            val path = root.pathToFile(NoteName(fileName))
            val title = fileName.split(".").last()
            val content = Templates.newNote(root, title, NoteName("templates.note"))
            appVm.state.value.workspace.addTab(path, content)
        }

    }


fun createDeleteAction(appVm: NoteWindowViewModel): Action =
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

fun createRenameNoteAction(appVm: NoteWindowViewModel): Action =
    Action("Rename", "Renames current file") {
        val activeTab = appVm.state.value.workspace.activeTab()
        if (activeTab != null) {
            val title = activeTab.title
            appVm.actionLauncherViewModel.showInput(initialQuery = title) { newTitle ->
                val oldName = NoteName(title)
                val newName = NoteName(newTitle)
                appVm.renameNote(oldName, newName)
            }
        }
    }

fun createSelectRootAction(appVm: NoteWindowViewModel): Action =
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

fun createCycleRootAction(appVm: NoteWindowViewModel): Action =
    Action("Cycle roots", "Cycles through all opened roots") {
        appVm.cycleRoots()
    }

fun createRefactorHierarchyAction(appVm: NoteWindowViewModel): Action =
    Action("Refactor hierarchy", "Changes name for multiple files at once") {
        val tabState = appVm.state.value.workspace.activeTab()
        val name = tabState?.title ?: ""
        val root = appVm.state.value.root
        // show input dialog to get the source pattern
        appVm.actionLauncherViewModel.show(initialQuery = name) { srcPattern ->
            val files = root.searchFiles(srcPattern, MatchingStrategy::contains)
            files.map { file ->
                Action(file.name) {
                    // show input dialog to get the destination pattern
                    appVm.actionLauncherViewModel.show { dstPattern ->
                        files.map {
                            Action("${it.name} > ${it.name.replace(srcPattern, dstPattern)}") {
                                appVm.refactorHierarchy(srcPattern, dstPattern, files)
                            }
                        }
                    }
                }
            }
        }
    }


fun createFollowLinkAction(appVm: NoteWindowViewModel): Action {
    return Action("Follow link", "Follows link under cursor") {
        val activeTab = appVm.state.value.workspace.activeTab()
        val editorViewModel = activeTab?.editorViewModel
        val content = editorViewModel?.content?.text ?: ""
        val cursor = editorViewModel?.state?.value?.content?.selection?.start ?: 0

        val link = Markdown.findLink(content, cursor)
        if (link != null) {
            if (link.startsWith("http")) {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(java.net.URI(link))
                }
            } else {
                val noteName = NoteName(link)
                appVm.openNote(noteName)
            }
        }
    }
}

fun createOpenDailyNoteAction(appVm: NoteWindowViewModel): Action =
    Action("Open daily journal note") {
        appVm.openNote(Journal.todaysDailyNote(), "templates.daily")
    }

fun createPreviousDailyNoteAction(appVm: NoteWindowViewModel): Action =
    Action("Open previous daily journal note") {
        val root = appVm.state.value.root
        val activeTab = appVm.state.value.workspace.activeTab()
        val currentTitle = activeTab?.path?.nameWithoutExtension
        val date = currentTitle?.let(Journal::getJournalDate) ?: LocalDate.now()
        val previousNote = Journal.previousDailyNote(root, date)
        if (previousNote != null) {
            appVm.openNote(previousNote, "templates.daily")
        }
    }

fun createNextDailyNoteAction(appVm: NoteWindowViewModel): Action =
    Action("Open next daily journal note") {
        val root = appVm.state.value.root
        val activeTab = appVm.state.value.workspace.activeTab()
        val currentTitle = activeTab?.path?.nameWithoutExtension
        val date = currentTitle?.let(Journal::getJournalDate) ?: LocalDate.now()
        val nextNote = Journal.nextDailyNote(root, date)
        if (nextNote != null) {
            appVm.openNote(nextNote, "templates.daily")
        } else {
            val nextDay = date.plusDays(1)
            val noteName = Journal.formatJournalNoteName(nextDay)
            appVm.openNote(noteName, "templates.daily")
        }
    }


fun createOpenWeeklyNoteAction(appVm: NoteWindowViewModel): Action =
    Action("Open weekly note") {
        appVm.openNote(Weekly.getCurrentWeeklyNote(), "templates.weekly")
    }

fun createPreviousWeeklyNoteAction(appVm: NoteWindowViewModel): Action =
    Action("Open previous weekly note") {
        val root = appVm.state.value.root
        val activeTab = appVm.state.value.workspace.activeTab()
        val currentTitle = activeTab?.path?.nameWithoutExtension
        currentTitle?.let { Weekly.getWeekAndYear(it) }?.let {
            val (week, year) = it
            val previousNote = Weekly.getPreviousWeeklyNote(root, week, year)
            if (previousNote != null) {
                appVm.openNote(previousNote, "templates.weekly")
            }
        }
    }

fun createNextWeeklyNoteAction(appVm: NoteWindowViewModel): Action =
    Action("Open next weekly note") {
        val root = appVm.state.value.root
        val activeTab = appVm.state.value.workspace.activeTab()
        val currentTitle = activeTab?.path?.nameWithoutExtension
        currentTitle?.let { Weekly.getWeekAndYear(it) }?.let {
            val (week, year) = it
            val nextNote = Weekly.getNextWeeklyNote(root, week, year)
            if (nextNote != null) {
                appVm.openNote(nextNote, "templates.weekly")
            } else {
                val nextWeekFirstDay = Weekly.getFirstDayOfWeek(week, year).plusWeeks(1)
                val noteName = Weekly.formatWeeklyNoteName(Weekly.getWeek(nextWeekFirstDay), nextWeekFirstDay.year)
                appVm.openNote(noteName, "templates.weekly")
            }
        }
    }


fun createInsertTemplateAction(appVm: NoteWindowViewModel): Action =
    Action("Insert template", "Inserts template at cursor position") {

        // select template
        appVm.actionLauncherViewModel.show("", placeholder = "Select a template...") { query ->
            val root = appVm.state.value.root
            val templates = root.searchFiles("templates.", { entry, pattern -> entry.startsWith(pattern)} )
                .map { it.name.removePrefix("") }

            templates.filter { it.contains(query, ignoreCase = true) }
                .map { template ->
                    Action(template) {
                        val activeTab = appVm.state.value.workspace.activeTab()
                        val editorViewModel = activeTab?.editorViewModel
                        val cursor = editorViewModel?.state?.value?.content?.selection?.start ?: 0
                        val templateContent = Templates.loadTemplate(root, NoteName(template))
                        editorViewModel?.insert(templateContent, cursor)
                    }
                }
        }
    }

fun createJumpToBacklinkAction(appVm: NoteWindowViewModel): Action =
    Action("Jump to backlink", "Shows a list of backlinks to current note") {
        val matchingStrategy = MatchingStrategy::fuzzy
        val activeTab = appVm.state.value.workspace.activeTab()
        val noteName = activeTab?.title
        if (noteName != null) {
            val root = appVm.state.value.root
            val backlinks = root.searchBacklinks(NoteName(noteName)).filterIsInstance<Match>()
            appVm.actionLauncherViewModel.show("") { pattern ->
                backlinks
                    .filter { matchingStrategy(it.path, pattern) }
                    .groupBy { it.path }
                    .map { (_, backlinks) ->
                        val path = Path.of(backlinks.first().path)
                        val title = path.nameWithoutExtension
                        val description = backlinks.fold(StringBuilder()) { acc, entry ->
                            acc.appendLine(entry.lines.trim())
                        }.toString().trim()
                        Action(title, description) {
                            appVm.openNote(root.getNoteName(path))
                        }
                    }
            }
        }
    }


fun createSearchPhraseAction(appVm: NoteWindowViewModel): Action =
    Action("Search phrase", "Searches for a phrase in all notes") {
        val root = appVm.state.value.root
        appVm.actionLauncherViewModel.show(placeholder = "Enter a phrase...") { phrase ->
            val entries = root.searchInFiles(phrase).filterIsInstance<Match>()
            entries.map { entry ->
                val path = Path.of(entry.path)
                Action(path.nameWithoutExtension, entry.lines.trim()) {
                    appVm.openNote(NoteName(path.nameWithoutExtension))
                }
            }
        }
    }
