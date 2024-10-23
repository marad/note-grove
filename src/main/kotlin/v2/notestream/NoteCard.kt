package v2.notestream

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import editor.editorFont
import kotlinx.coroutines.flow.map
import v2.Buffer

data class NoteCardState(
    val buffer: Buffer,
    val selection: TextRange = TextRange.Zero
) {
    val title get() = buffer.name.value
    val textFieldValue get() = TextFieldValue(buffer.content.value, selection)

    fun update(textFieldValue: TextFieldValue): NoteCardState {
        buffer.updateContent(textFieldValue.annotatedString)
        return copy(selection = textFieldValue.selection)
    }
}

@Composable
fun NoteCard(state: NoteCardState,
             modifier: Modifier = Modifier,
             textFieldModifier: Modifier = Modifier,
             active: Boolean = false,
             onChange: (NoteCardState) -> Unit = {},
             onClose: () -> Unit = {},
             onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    val focusRequester = remember {  FocusRequester() }
    val content by state.buffer.content.map {
        TextFieldValue(it, state.selection)
    }.collectAsState(state.textFieldValue)

    Card(modifier) {
        Column(Modifier.padding(8.dp)) {
            Row(Modifier.padding(vertical = 10.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    state.buffer.name.value,
                    style = MaterialTheme.typography.h6)
                Spacer(Modifier.width(10.dp))

                Surface(
                    color = state.buffer.root.color,
                    modifier = Modifier.clip(RoundedCornerShape(5.dp))
                    ) {
                    Text(
                        state.buffer.root.name,
                        modifier = Modifier.padding(5.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.caption,
                    )
                }
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Default.Close, "Close the note",
                    Modifier.align(Alignment.CenterVertically)
                        .defaultMinSize(10.dp, 10.dp)
                        .clickable(onClick = onClose)
                )
            }
            BasicTextField(
                content,
                modifier = textFieldModifier.fillMaxWidth().focusRequester(focusRequester),
                onValueChange = { value -> onChange(state.update(value)) },
                onTextLayout = onTextLayout,
                textStyle = TextStyle(
                    fontFamily = editorFont,
                    fontSize = 16.sp
                ),
            )
        }
    }

    LaunchedEffect(active) {
        if (active) {
            focusRequester.requestFocus()
        }
    }
}
