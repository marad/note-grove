package editor

import Markdown
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.coerceIn
import androidx.compose.ui.text.input.TextFieldValue

class EditorState(content: String = "") {
    val dirty = mutableStateOf(false)
    private val content = mutableStateOf(TextFieldValue(Markdown.parse(content).toAnnotatedString()))
    val focusRequester = FocusRequester()
    val completionsState = CompletionsState()

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
        // TODO: highlighting should be done as visual transformation
        val md = Markdown.parse(newContent.text)
        content.value = newContent.copy(md.toAnnotatedString())
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