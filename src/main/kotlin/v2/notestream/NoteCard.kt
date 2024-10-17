package v2.notestream

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import v2.Buffer

data class NoteCardState(
    val buffer: Buffer,
    val selection: TextRange = TextRange.Zero
) {
    val textFieldValue get() = TextFieldValue(buffer.content.value, selection)

    fun update(textFieldValue: TextFieldValue): NoteCardState {
        buffer.updateContent(textFieldValue.annotatedString)
        return copy(selection = textFieldValue.selection)
    }
}

@Composable
fun NoteCard(state: NoteCardState,
             modifier: Modifier = Modifier,
             onChange: (NoteCardState) -> Unit = {},
             onClose: () -> Unit = {}
) {
    Card(modifier) {
        Column(Modifier.padding(8.dp)) {
            Row(Modifier.padding(vertical = 10.dp).fillMaxWidth()) {
                Text(
                    state.buffer.title,
                    style = MaterialTheme.typography.h6)
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Default.Close, "Close the note",
                    Modifier.align(Alignment.CenterVertically)
                        .defaultMinSize(10.dp, 10.dp)
                        .clickable(onClick = onClose)
                )
            }
            BasicTextField(
                state.textFieldValue,
                onValueChange = { value -> onChange(state.update(value)) },
            )
        }
    }
}
