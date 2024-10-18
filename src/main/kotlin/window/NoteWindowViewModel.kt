package window

import LauncherViewModel
import NoteName
import Root
import RootState
import androidx.lifecycle.ViewModel
import config.RootConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import prepareActionsAndShortcuts
import kotlin.io.path.nameWithoutExtension

class NoteWindowViewModel(
    roots: List<RootConfig>,
    val actionLauncherViewModel: LauncherViewModel = LauncherViewModel(),
) : ViewModel() {
    private val _state = MutableStateFlow(NoteWindowState(
        roots = roots.map { RootState(it.name, Root(it.name, it.path)) }
    ))
    val state = _state.asStateFlow()

    val shortcuts = prepareActionsAndShortcuts(this)


    fun cycleRoots() {
        _state.value = _state.value.copy(activeRootIndex = (_state.value.activeRootIndex + 1) % _state.value.roots.size)
    }

    fun selectRoot(root: Root) {
        val index = _state.value.roots.indexOfFirst { it.root == root }
        if (index != -1) {
            _state.value = _state.value.copy(activeRootIndex = index)
        }
    }

    fun openNote(noteName: NoteName, templateName: String = "templates.note") {
        val root = state.value.root
        val path = root.pathToFile(noteName)
        val title = path.nameWithoutExtension
        val defaultContent = Templates.newNote(root, title, NoteName(templateName))
        state.value.workspace.addTab(path, defaultContent)
    }

    fun renameNote(oldName: NoteName, newName: NoteName) {
        val root = state.value.root
        val updatedNotes = root.renameNote(oldName, newName)
        state.value.workspace.updateTabFile(root.pathToFile(oldName), root.pathToFile(newName))
        updatedNotes.forEach {
            state.value.workspace.reloadTabIfOpened(root.pathToFile(it))
        }
    }

    fun refactorHierarchy(srcPattern: String, dstPattern: String, files: List<NoteName>) {
        val root = state.value.root
        val updatedNotes = mutableSetOf<NoteName>()
        files.forEach {
            val newName = NoteName(it.value.replace(srcPattern, dstPattern))
            state.value.workspace.updateTabFile(root.pathToFile(it), root.pathToFile(newName))
            updatedNotes.addAll(root.renameNote(it, newName))
        }
        updatedNotes.forEach {
            state.value.workspace.reloadTabIfOpened(root.pathToFile(it))
        }
    }
}