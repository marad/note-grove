package v2.window

import LauncherViewModel
import NoteName
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.AnnotatedString
import com.vladsch.flexmark.util.ast.NodeVisitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import v2.BufferManager
import v2.notestream.NoteCardState
import v2.prepareActionsAndShortcuts
import java.nio.file.Files

class MainWindowController(
    private val bufferManager: BufferManager,
    roots: List<Root>,
    val coScope: CoroutineScope,
    val launcher: LauncherViewModel = LauncherViewModel()
) {
    private val _state = MutableStateFlow(MainWindowState(roots = roots))
    val state = _state.asStateFlow()
    val root get() = state.value.root
    val stream get() = state.value.noteStreamState

    val streamLazyListState = LazyListState()
    val shortcuts = prepareActionsAndShortcuts(this)

    val selectedNoteIndex = mutableStateOf(-1)

    fun updateState(f: (MainWindowState) -> MainWindowState) {
        _state.update(f)
    }

    fun selectRoot(root: Root) {
        updateState { state ->
            val index = state.roots.indexOfFirst { it == root }
            if (index != -1) {
                state.copy(activeRoot = index)
            } else {
                state
            }
        }
    }

    fun cycleRoots() {
        updateState { it.cycleRoots() }
    }

    fun openNote(noteName: NoteName, templateName: String = "templates.note") {
        val index = stream.cards.find { it.buffer.name == noteName }
            ?.let { stream.cards.indexOf(it) }
        if (index != null) {
            coScope.launch {
                streamLazyListState.animateScrollToItem(index)
            }
        } else {
            updateState {
               it.copy(
                    noteStreamState = stream.prependCard(
                        NoteCardState(bufferManager.openBuffer(root, noteName) {
                            Templates.newNote(root, noteName.value, NoteName(templateName))
                        })))
            }
            coScope.launch {
                streamLazyListState.animateScrollToItem(0)
            }
        }
    }

    fun getNote(name: NoteName): NoteCardState? =
        state.value.noteStreamState.cards.find { it.title == name.value }

    fun currentNote(): NoteCardState? {
        val index = selectedNoteIndex.value
        return if (index >= 0 && index < state.value.noteStreamState.cards.size) {
            state.value.noteStreamState.cards[index]
        } else {
            null
        }
    }

    fun replaceNote(old: NoteCardState, new: NoteCardState) {
        updateState { state ->
            state.copy(noteStreamState = state.noteStreamState.replaceCard(old, new))
        }
    }

    fun closeCurrentNote() {
        currentNote()?.let(this::closeNote)
    }

    fun closeNote(card: NoteCardState) {
        updateState { state ->
            state.copy(noteStreamState = state.noteStreamState.closeCard(card))
        }
    }

    /***
     * Updates a note card based on the buffer note name
     */
    fun updateCard(card: NoteCardState) {
        updateState { state ->
            state.copy(noteStreamState = state.noteStreamState.updateCard(card))
        }
    }

    fun saveNote(card: NoteCardState) {
        val content = card.buffer.content.value.text
        val file = card.buffer.path
        val md = Markdown.parse(content)
        val visitor = NodeVisitor(
            Markdown.updateYamlFrontmatterVisitHandler("updated",
                System.currentTimeMillis().toString())
        )
        visitor.visit(md)

        val updatedContent = md.chars.toString()
        coScope.launch {
            card.buffer.updateContent(AnnotatedString(updatedContent))
        }
        Files.write(file, updatedContent.toByteArray())
    }

    fun deleteNote(card: NoteCardState) {
        closeNote(card)
        Files.delete(card.buffer.path)
    }

    fun reloadNoteFromDisk(name: NoteName) {
        bufferManager.reloadBuffer(name)
    }

    fun renameNote(old: NoteName, new: NoteName) {
        val oldCard = getNote(old)
        if (oldCard != null) {
            val root = oldCard.buffer.root
            val updatedNotes = root.renameNote(old, new)
            // FIXME: this should probably update buffer state in place
            //        so that cards in other windows would update as well
            val newCard = NoteCardState(bufferManager.openBuffer(root, new) {
                Templates.newNote(root, new.value, NoteName("templates.note"))
            })
            bufferManager.removeBuffer(oldCard.buffer.path)
            replaceNote(oldCard, newCard)
            updatedNotes.forEach { reloadNoteFromDisk(it) }
        }
    }

    fun refactorHierarchy(srcPattern: String, dstPattern: String, files: List<NoteName>) {
        files.forEach {
            val newName = NoteName(it.value.replace(srcPattern, dstPattern))
            renameNote(it, newName)
        }
    }
}