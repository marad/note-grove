package v2

import Action
import KeyModifier.Ctrl
import KeyModifier.Shift
import NoteName
import Shortcut
import Shortcuts
import androidx.compose.ui.input.key.Key
import files.internal.MatchingStrategy
import tools.rg.Match
import v2.window.MainWindowController
import java.awt.Desktop
import java.net.URI
import java.nio.file.Path
import java.time.LocalDate
import kotlin.io.path.nameWithoutExtension

fun prepareActionsAndShortcuts(mainWindowController: MainWindowController): Shortcuts {
    val shortcuts = Shortcuts()
    val appActions = mutableListOf<Action>()

    val saveAction = createSaveAction(mainWindowController)
    val closeCurrentNoteAction = createCloseCurrentNoteAction(mainWindowController)
    val newNoteAction = newNoteAction(mainWindowController)
    val deleteNoteAction = createDeleteAction(mainWindowController)
    val renameNoteAction = createRenameNoteAction(mainWindowController)
    val selectRootAction = createSelectRootAction(mainWindowController)
    val cycleRootAction = createCycleRootAction(mainWindowController)
    val followLinkAction = createFollowLinkAction(mainWindowController)
    val showNoteSearchDialog = createSearchNoteAction(mainWindowController, appActions)
    val showActionSearchDialog = createSearchActionsAction(mainWindowController, appActions)
    val openDailyNote = createOpenDailyNoteAction(mainWindowController)
    val previousDailyNote = createPreviousDailyNoteAction(mainWindowController)
    val nextDailyNote = createNextDailyNoteAction(mainWindowController)
    val openWeeklyNote = createOpenWeeklyNoteAction(mainWindowController)
    val previousWeeklyNote = createPreviousWeeklyNoteAction(mainWindowController)
    val nextWeeklyNote = createNextWeeklyNoteAction(mainWindowController)
    val insertTemplate = createInsertTemplateAction(mainWindowController)
    val jumpToBacklink = createJumpToBacklinkAction(mainWindowController)
    val searchPhrase = createSearchPhraseAction(mainWindowController)
    val insertNoteLink = createInsertNoteLinkAction(mainWindowController)
    val moveNoteUp = createMoveNoteUpAction(mainWindowController)
    val moveNoteDown = createMoveNoteDownAction(mainWindowController)

    appActions.addAll(listOf(
        saveAction, newNoteAction, deleteNoteAction, renameNoteAction, selectRootAction,
        cycleRootAction, followLinkAction,
        createRefactorHierarchyAction(mainWindowController),
        closeCurrentNoteAction, showNoteSearchDialog, showActionSearchDialog,
        openDailyNote, previousDailyNote, nextDailyNote,
        openWeeklyNote, previousWeeklyNote, nextWeeklyNote,
        insertTemplate, jumpToBacklink, searchPhrase,
        insertNoteLink, moveNoteUp, moveNoteDown
    ))


    appActions.sortBy { it.name }

    shortcuts.add(Shortcut(Key.S, Ctrl), saveAction)
    shortcuts.add(Shortcut(Key.W, Ctrl), closeCurrentNoteAction)
    shortcuts.add(Shortcut(Key.N, Ctrl), newNoteAction)
    shortcuts.add(Shortcut(Key.R, Ctrl, Shift), selectRootAction)
    shortcuts.add(Shortcut(Key.R, Ctrl), cycleRootAction)
    shortcuts.add(Shortcut(Key.G, Ctrl), followLinkAction)
    shortcuts.add(Shortcut(Key.P, Ctrl), showNoteSearchDialog)
    shortcuts.add(Shortcut(Key.P, Ctrl, Shift), showActionSearchDialog)
    shortcuts.add(Shortcut(Key.D, Ctrl), openDailyNote)
    shortcuts.add(Shortcut(Key.U, Ctrl), previousDailyNote)
    shortcuts.add(Shortcut(Key.I, Ctrl), nextDailyNote)
    shortcuts.add(Shortcut(Key.F, Ctrl, Shift), searchPhrase)
    shortcuts.add(Shortcut(Key.K, Ctrl), createSelectPrevNoteAction(mainWindowController))
    shortcuts.add(Shortcut(Key.J, Ctrl), createSelectNextNoteAction(mainWindowController))
    shortcuts.add(Shortcut(Key.I, Ctrl, Shift), insertNoteLink)
    shortcuts.add(Shortcut(Key.K, Ctrl, Shift), moveNoteUp)
    shortcuts.add(Shortcut(Key.J, Ctrl, Shift), moveNoteDown)

    return shortcuts
}

private fun searchActions(ctl: MainWindowController, appActions: List<Action>, name: String): List<Action> {
    return if (name.startsWith(">")) {
        val searchTerm = name.drop(1).trim()
        appActions.filter {
            MatchingStrategy.fuzzy(it.name, searchTerm) ||
                    (it.description?.let { MatchingStrategy.fuzzy(it, searchTerm) } == true)
        }
    } else {
        ctl.root.searchFiles(name, MatchingStrategy::fuzzy).map {
            Action(it.value) {
                ctl.openNote(it)
            }
        }
    }
}

fun createSearchNoteAction(ctl: MainWindowController, appActions: List<Action>): Action =
    Action("Search note", "Shows search note dialog") {
        val currentNoteTitle = ctl.currentNote()?.buffer?.name?.value
        ctl.launcher.show(currentNoteTitle, selectText = currentNoteTitle != null, forceAccept = { name ->
            ctl.openNote(NoteName(name))
        }) {
            searchActions(ctl, appActions, it)
        }
    }

fun createSearchActionsAction(ctl: MainWindowController, appActions: List<Action>): Action =
    Action("Search actions", "Shows search actions dialog") {
        ctl.launcher.show("> ") {
            searchActions(ctl, appActions, it)
        }
    }

fun createInsertNoteLinkAction(ctl: MainWindowController): Action =
    Action("Insert note link", "Helps you find a note and insert a link to it") {
        ctl.launcher.show("") { name ->
            ctl.root.searchFiles(name, MatchingStrategy::fuzzy).map {
                Action(it.value) {
                    val note = ctl.currentNote()
                    if (note != null) {
                        ctl.updateCard(note.insertAtCursor("[[${it.value}]]"))
                    }
                }
            }
        }
    }


fun createCloseCurrentNoteAction(mainWindowController: MainWindowController): Action =
    Action("Close selected note", "Closes currently selected note") {
        mainWindowController.closeCurrentNote()
    }

fun createSaveAction(ctl: MainWindowController): Action =
    Action("Save", "Saves current file") {
        ctl.currentNote()?.let { card ->
            ctl.saveNote(card)
        }
    }


fun newNoteAction(ctl: MainWindowController): Action =
    Action("New note", "Creates a new note") {
        val card = ctl.currentNote()
        val title = card?.title ?: ""

        ctl.launcher.showInput(initialQuery = title) { fileName ->
            ctl.openNote(NoteName(fileName))
        }
    }


fun createDeleteAction(ctl: MainWindowController): Action =
    Action("Delete", "Deletes current file") {
        // get current note name
        val card = ctl.currentNote()
        val question = card?.title?.let {
            "Are you sure that you want to delete $it?"
        } ?: "Are you sure that you want to delete this note?"

        ctl.launcher.showConfirm(
            onConfirm = {
                card?.let(ctl::deleteNote)
            },
            onCancel = {
                // do nothing
            }
        )

    }

fun createRenameNoteAction(ctl: MainWindowController): Action =
    Action("Rename", "Renames current file") {
        val card = ctl.currentNote()
        if (card != null) {
            ctl.launcher.showInput(initialQuery = card.title) { newTitle ->
                val oldName = NoteName(card.title)
                val newName = NoteName(newTitle)
                ctl.renameNote(oldName, newName)
            }
        }
    }

fun createSelectRootAction(ctl: MainWindowController): Action =
    Action("Select root", "Shows dialog to switch active root") {
        val roots = ctl.state.value.roots
        ctl.launcher.show("") { query ->
            roots.filter { it.name.contains(query, ignoreCase = true) }
                .map { root ->
                    Action(root.name, "Switch to ${root.name}") {
                        ctl.selectRoot(root)
                    }
                }
        }
    }

fun createCycleRootAction(ctl: MainWindowController): Action =
    Action("Cycle roots", "Cycles through all opened roots") {
        ctl.cycleRoots()
    }

fun createFollowLinkAction(ctl: MainWindowController): Action {
    return Action("Follow link", "Follows link under cursor") {
        val card = ctl.currentNote()
        val buffer = card?.buffer
        val content = buffer?.content?.value?.text ?: ""
        val cursor = card?.selection?.start ?: 0

        val link = Markdown.findLink(content, cursor)
        if (link != null) {
            if (link.startsWith("http")) {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(URI(link))
                }
            } else {
                val noteName = NoteName(link)
                ctl.openNote(noteName)
            }
        }
    }
}


fun createRefactorHierarchyAction(ctl: MainWindowController): Action =
    Action("Refactor hierarchy", "Changes name for multiple files at once") {
        val buffer = ctl.currentNote()?.buffer
        val name = buffer?.name?.value ?: ""
        val root = buffer?.root ?: ctl.root

        // show input dialog to get the source pattern
        ctl.launcher.show(initialQuery = name) { srcPattern ->
            val files = root.searchFiles(srcPattern, MatchingStrategy::contains)
            files.map { file ->
                Action(file.value) {
                    // show input dialog to get the destination pattern
                    ctl.launcher.show { dstPattern ->
                        files.map {
                            Action("${it.value} > ${it.value.replace(srcPattern, dstPattern)}") {
                                ctl.refactorHierarchy(srcPattern, dstPattern, files)
                            }
                        }
                    }
                }
            }
        }
    }


fun createOpenDailyNoteAction(ctl: MainWindowController): Action =
    Action("Open daily journal note") {
        ctl.openNote(Journal.todaysDailyNote(), "templates.daily")
    }

fun createPreviousDailyNoteAction(ctl: MainWindowController): Action =
    Action("Open previous daily journal note") {
        val currentTitle = ctl.currentNote()?.title
        val date = currentTitle?.let(Journal::getJournalDate) ?: LocalDate.now()
        val previousNote = Journal.previousDailyNote(ctl.root, date)
        if (previousNote != null) {
            ctl.openNote(previousNote, "templates.daily")
        }
    }

fun createNextDailyNoteAction(ctl: MainWindowController): Action =
    Action("Open next daily journal note") {
        val currentTitle = ctl.currentNote()?.title
        val date = currentTitle?.let(Journal::getJournalDate) ?: LocalDate.now()
        val nextNote = Journal.nextDailyNote(ctl.root, date)
        if (nextNote != null) {
            ctl.openNote(nextNote, "templates.daily")
        } else {
            val nextDay = date.plusDays(1)
            val noteName = Journal.formatJournalNoteName(nextDay)
            ctl.openNote(noteName, "templates.daily")
        }
    }


fun createOpenWeeklyNoteAction(ctl: MainWindowController): Action =
    Action("Open weekly note") {
        ctl.openNote(Weekly.getCurrentWeeklyNote(), "templates.weekly")
    }

fun createPreviousWeeklyNoteAction(ctl: MainWindowController): Action =
    Action("Open previous weekly note") {
        val currentTitle = ctl.currentNote()?.title
        currentTitle?.let { Weekly.getWeekAndYear(it) }?.let {
            val (week, year) = it
            val previousNote = Weekly.getPreviousWeeklyNote(ctl.root, week, year)
            if (previousNote != null) {
                ctl.openNote(previousNote, "templates.weekly")
            }
        }
    }

fun createNextWeeklyNoteAction(ctl: MainWindowController): Action =
    Action("Open next weekly note") {
        val currentTitle = ctl.currentNote()?.title
        currentTitle?.let { Weekly.getWeekAndYear(it) }?.let {
            val (week, year) = it
            val nextNote = Weekly.getNextWeeklyNote(ctl.root, week, year)
            if (nextNote != null) {
                ctl.openNote(nextNote, "templates.weekly")
            } else {
                val nextWeekFirstDay = Weekly.getFirstDayOfWeek(week, year).plusWeeks(1)
                val noteName = Weekly.formatWeeklyNoteName(Weekly.getWeek(nextWeekFirstDay), nextWeekFirstDay.year)
                ctl.openNote(noteName, "templates.weekly")
            }
        }
    }


fun createInsertTemplateAction(ctl: MainWindowController): Action =
    Action("Insert template", "Inserts template at cursor position") {
        // select template
        ctl.launcher.show("", placeholder = "Select a template...") { query ->
            val templates = ctl.root.searchFiles("templates.", { entry, pattern -> entry.startsWith(pattern)} )
                .map { it.value.removePrefix("") }

            templates.filter { it.contains(query, ignoreCase = true) }
                .map { template ->
                    Action(template) {
                        val note = ctl.currentNote()
                        if (note != null) {
                            val templateContent = Templates.loadTemplate(ctl.root, NoteName(template))
                            ctl.updateCard(note.insertAtCursor(templateContent))
                        }
                    }
                }
        }
    }

fun createJumpToBacklinkAction(ctl: MainWindowController): Action =
    Action("Jump to backlink", "Shows a list of backlinks to current note") {
        val matchingStrategy = MatchingStrategy::fuzzy
        val note = ctl.currentNote()
        if (note != null) {
            val root = note.buffer.root
            val backlinks = root.searchBacklinks(note.buffer.name).filterIsInstance<Match>()
            ctl.launcher.show("") { pattern ->
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
                            ctl.openNote(root.getNoteName(path))
                        }
                    }
            }
        }
    }


fun createSearchPhraseAction(ctl: MainWindowController): Action =
    Action("Search phrase", "Searches for a phrase in all notes") {
        val root = ctl.root
        ctl.launcher.show(placeholder = "Enter a phrase...") { phrase ->
            val entries = root.searchInFiles(phrase).filterIsInstance<Match>()
            entries.map { entry ->
                val path = Path.of(entry.path)
                Action(path.nameWithoutExtension, entry.lines.trim()) {
                    ctl.openNote(NoteName(path.nameWithoutExtension))
                }
            }
        }
    }

fun createSelectNextNoteAction(ctl: MainWindowController): Action =
    Action("Select next note") { ctl.selectNextNote() }

fun createSelectPrevNoteAction(ctl: MainWindowController): Action =
    Action("Select previous note") { ctl.selectPrevNote() }

fun createMoveNoteUpAction(ctl: MainWindowController): Action =
    Action("Move note up") { ctl.moveNoteUp() }

fun createMoveNoteDownAction(ctl: MainWindowController): Action =
    Action("Move note down") { ctl.moveNoteDown() }