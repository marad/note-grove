package editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.text.TextFieldScrollState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
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
class EditorViewModel(content: String = "", isDirty: Boolean = false) : ViewModel() {
    private val _state = MutableStateFlow(
        EditorState(
            TextFieldValue(content),
            dirty = isDirty,
        )
    )
    val state = _state.asStateFlow()
    val scrollState = TextFieldScrollState(Orientation.Vertical)
    val completionsState = CompletionsState()
    val focusRequester = FocusRequester()
    val content get() = state.value.content
    var layout: TextLayoutResult? = null


    fun clearDirty() { _state.update { it.copy(dirty = false) }}
    fun updateContent(text: String, markDirty: Boolean = true) {
        val newContent = state.value.content.copy(text)
        updateContent(newContent, markDirty)
    }
    fun updateContent(newContent: TextFieldValue, markDirty: Boolean = true) {
        val shouldBeDirty = markDirty && content.text != newContent.text
        _state.update { it.copy(content = newContent, dirty = shouldBeDirty || it.dirty) }
    }

    fun moveCaretDown() {
        val layout = this.layout
        if (layout != null) {
            val caretOffset = content.selection.end
            val currentLine = layout.getLineForOffset(caretOffset)
            if (currentLine >= layout.lineCount-1) return
            val currentLineStartOffset = layout.getLineStart(currentLine)
            val nextLineStartOffset = layout.getLineStart(currentLine+1)
            val nextLineEndOffset = layout.getLineEnd(currentLine+1)
            val currentLinePos = caretOffset - currentLineStartOffset
            val finalPos = nextLineStartOffset + currentLinePos
            setCaretOffset(finalPos.coerceIn(nextLineStartOffset, nextLineEndOffset))
        }
    }

    fun moveCaretUp() {
        val layout = this.layout
        if (layout != null) {
            val caretOffset = content.selection.end
            val currentLine = layout.getLineForOffset(caretOffset)
            if (currentLine <= 0) return
            val currentLineStartOffset = layout.getLineStart(currentLine)
            val prevLineStartOffset = layout.getLineStart(currentLine-1)
            val prevLineEndOffset = layout.getLineEnd(currentLine-1)
            val currentLinePos = caretOffset - currentLineStartOffset
            val finalPos = prevLineStartOffset + currentLinePos
            setCaretOffset(finalPos.coerceIn(prevLineStartOffset, prevLineEndOffset))
        }
    }

    fun moveCaretLeft() {
        val caretOffset = content.selection.end
        if (caretOffset > 0) {
            setCaretOffset(caretOffset-1)
        }
    }

    fun moveCaretRight() {
        val caretOffset = content.selection.end
        if (caretOffset <= content.text.length) {
            setCaretOffset(caretOffset+1)
        }
    }

    fun setCaretOffset(offset: Int) {
        _state.update { it.copy(content.copy(selection = TextRange(offset.coerceIn(
            0, content.text.length
        )))) }
    }

    fun moveCaretToStartOfCurrentLine() {
        val layout = this.layout
        if (layout != null) {
            val caretOffset = content.selection.end
            val currentLine = layout.getLineForOffset(caretOffset)
            val startOfCurrentLine = layout.getLineStart(currentLine)
            setCaretOffset(startOfCurrentLine)
        }
    }

    fun moveCaretToEndOfCurrentLine() {
        val layout = this.layout
        if (layout != null) {
            val caretOffset = content.selection.end
            val currentLine = layout.getLineForOffset(caretOffset)
            val endOfCurrentLine = layout.getLineEnd(currentLine)
            setCaretOffset(endOfCurrentLine)
        }
    }

    fun moveCaretToEndOfNextWord() {
        val layout = this.layout
        if (layout != null) {
            val caretOffset = content.selection.end + 1
            val currentChar = content.text.getOrNull(caretOffset) ?: return
            val searchStart = if (currentChar.isLetterOrDigit()) {
                caretOffset
            } else {
                caretOffset + content.text.substring(caretOffset).indexOfFirst { it.isLetterOrDigit() }
            }
            val wordBoundary = layout.getWordBoundary(searchStart)
            setCaretOffset(wordBoundary.end-1)
        }
    }

    fun moveCaretToBeginingOfPreviousWord() {
        val layout = this.layout
        if (layout != null) {
            val caretOffset = content.selection.end - 1
            val currentChar = content.text.getOrNull(caretOffset) ?: return
            val searchStart = if (currentChar.isLetterOrDigit()) {
                caretOffset
            } else {
                content.text.substring(0, caretOffset).indexOfLast { it.isLetterOrDigit() }
            }
            val wordBoundary = layout.getWordBoundary(searchStart)
            setCaretOffset(wordBoundary.start)
        }
    }

    fun insertLineBelow() {
        val layout = this.layout ?: return
        val caretOffset = content.selection.end
        val curentLine = layout.getLineForOffset(caretOffset)
        val lineStart = layout.getLineStart(curentLine)
        val lineEnd = layout.getLineEnd(curentLine)

        insert("\n", lineEnd+1)
    }

    fun insertLineAbove() {
        val layout = this.layout ?: return
        val caretOffset = content.selection.end
        val curentLine = layout.getLineForOffset(caretOffset)
        val lineStart = layout.getLineStart(curentLine)

        insert("\n", lineStart)
    }

    fun replace(range: TextRange, text: String) {
        val old = state.value.content.text
        val sb = StringBuilder()
        sb.append(old.substring(0, range.start))
        sb.append(text)
        sb.append(old.substring(range.end))
        val new = state.value.content.copy(sb.toString(), selection = state.value.content.selection.coerceIn(
            range.start+text.length, range.start+text.length
        ))
        updateContent(new)
    }

    fun insert(text: String, offset: Int = content.selection.end) {
        val old = state.value.content.text
        val sb = StringBuilder()
        sb.append(old.substring(0, offset))
        sb.append(text)
        sb.append(old.substring(offset))
        val new = state.value.content.copy(sb.toString(), selection = state.value.content.selection.coerceIn(
            offset, offset
        ))
        updateContent(new)
    }

}
