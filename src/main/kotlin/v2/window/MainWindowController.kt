package v2.window

import LauncherViewModel
import androidx.compose.foundation.lazy.LazyListState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    fun updateState(current: MainWindowState, new: MainWindowState) {
        _state.compareAndSet(current, new)
    }

    fun openNote(noteName: NoteName) {
        val index = stream.cards.find { it.buffer.title == noteName.name }
            ?.let { stream.cards.indexOf(it) }
        if (index != null) {
            coScope.launch {
                streamLazyListState.animateScrollToItem(index)
            }
        } else {
            updateState(
                state.value,
                state.value.copy(
                    noteStreamState = stream.prependCard(
                        NoteCardState(bufferManager.openBuffer(root.pathToFile(noteName)))
                    )
                )
            )
            coScope.launch {
                streamLazyListState.animateScrollToItem(0)
            }
        }
    }

}