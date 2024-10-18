package v2.window

import LauncherViewModel
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import v2.BufferManager
import v2.notestream.NoteCardState
import v2.prepareActionsAndShortcuts

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

    val currentNote = mutableStateOf(-1)

    fun updateState(f: (MainWindowState) -> MainWindowState) {
        _state.update(f)
    }

    fun openNote(noteName: NoteName) {
        val index = stream.cards.find { it.buffer.title == noteName.name }
            ?.let { stream.cards.indexOf(it) }
        if (index != null) {
            coScope.launch {
                streamLazyListState.animateScrollToItem(index)
            }
        } else {
            updateState {
               it.copy(
                    noteStreamState = stream.prependCard(
                        NoteCardState(bufferManager.openBuffer(root.pathToFile(noteName)))))
            }
            coScope.launch {
                streamLazyListState.animateScrollToItem(0)
            }
        }
    }

    fun currentNoteCard(): NoteCardState? {
        val index = currentNote.value
        return if (index >= 0 && index < state.value.noteStreamState.cards.size) {
            state.value.noteStreamState.cards[index]
        } else {
            null
        }
    }

    fun closeCurrentNote() {
        if (currentNote.value >= 0 && currentNote.value < state.value.noteStreamState.cards.size) {
            updateState {
                it.copy(noteStreamState = it.noteStreamState.closeCardAt(currentNote.value))
            }
        }
    }

}