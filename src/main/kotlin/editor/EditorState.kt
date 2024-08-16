package editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.text.TextFieldScrollState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.coerceIn
import androidx.compose.ui.text.input.TextFieldValue

class EditorState(content: String = "") {
    val dirty = mutableStateOf(false)
    private val content = mutableStateOf(TextFieldValue(content))
    val focusRequester = FocusRequester()
    val completionsState = CompletionsState()
    @OptIn(ExperimentalFoundationApi::class)
    val scrollState = TextFieldScrollState(Orientation.Vertical)

    fun markDirty() {
        dirty.value = true
    }

    fun clearDirty() {
        dirty.value = false
    }

    fun isDirty() = dirty.value

    fun getContent(): TextFieldValue = content.value

    fun updateContent(text: String, markDirty: Boolean = true) {
        updateContent(content.value.copy(text), markDirty)
    }

    fun updateContent(newContent: TextFieldValue, markDirty: Boolean = true) {
        if (markDirty && content.value.text != newContent.text) {
            markDirty()
        }
        content.value = newContent
    }

    fun replace(start: Int, end: Int, text: String) {
        val old = content.value.text
        val sb = StringBuilder()
        sb.append(old.substring(0, start))
        sb.append(text)
        sb.append("]]")
        sb.append(old.substring(end))
        val new = content.value.copy(sb.toString(), selection = content.value.selection.coerceIn(
            start+text.length+2, start+text.length+2
        ))
        updateContent(new)
    }

    fun insert(text: String) {
        val old = content.value.text
        val position = content.value.selection.start
        val sb = StringBuilder()
        sb.append(old.substring(0, position))
        sb.append(text)
        sb.append("]]")
        sb.append(old.substring(position))
        val new = content.value.copy(sb.toString(), selection = content.value.selection.coerceIn(
            content.value.selection.start+text.length+2,
            content.value.selection.start+text.length+2
        ))
        updateContent(new)
    }

    fun requestFocus() {
        focusRequester.requestFocus()
    }
}