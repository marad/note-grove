package editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.text.TextFieldScrollState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.coerceIn
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class EditorState(
    val content: TextFieldValue,
    val dirty: Boolean,
)

@OptIn(ExperimentalFoundationApi::class)
class EditorViewModel(content: String = "") : ViewModel() {
    private val _state = MutableStateFlow(
        EditorState(
            TextFieldValue(content),
            dirty = false,
        )
    )
    val state = _state.asStateFlow()
    val scrollState = TextFieldScrollState(Orientation.Vertical)
    val completionsState = CompletionsState()
    val focusRequester = FocusRequester()
    val content get() = state.value.content

    fun markDirty() { _state.update { it.copy(dirty = true) }}
    fun clearDirty() { _state.update { it.copy(dirty = false) }}
    fun updateContent(text: String, markDirty: Boolean = true) {
        val newContent = state.value.content.copy(text)
        updateContent(newContent, markDirty)
    }
    fun updateContent(newContent: TextFieldValue, markDirty: Boolean = true) {
        val shouldBeDirty = markDirty && content.text != newContent.text
        _state.update { it.copy(content = newContent, dirty = shouldBeDirty || it.dirty) }
    }

    fun replace(start: Int, end: Int, text: String) {
        val old = state.value.content.text
        val sb = StringBuilder()
        sb.append(old.substring(0, start))
        sb.append(text)
        sb.append("]]")
        sb.append(old.substring(end))
        val new = state.value.content.copy(sb.toString(), selection = state.value.content.selection.coerceIn(
            start+text.length+2, start+text.length+2
        ))
        updateContent(new)
    }

}

