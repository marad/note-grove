import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.TextFieldValue

class EditorState(content: String = "") {
    val dirty = mutableStateOf(false)
    val content = mutableStateOf(TextFieldValue(content))
    val focusRequester = FocusRequester()

    fun markDirty() {
        dirty.value = true
    }

    fun isDirty() = dirty.value

    fun updateContent(newContent: TextFieldValue, markDirty: Boolean = true) {
        if (markDirty && content.value.text != newContent.text) {
            markDirty()
        }
        content.value = newContent
    }

    fun requestFocus() {
        focusRequester.requestFocus()
    }
}

@Composable
fun Editor(state: EditorState) {
    OutlinedTextField(state.content.value, state::updateContent,
        modifier = Modifier.fillMaxSize()
            .focusRequester(state.focusRequester)
    )
    LaunchedEffect(state) {
        state.requestFocus()
    }
}

