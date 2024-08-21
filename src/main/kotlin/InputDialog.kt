import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Card
import androidx.compose.material.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.singleWindowApplication
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class InputDialogState(
    val text: TextFieldValue,
    val visible: Boolean = false
)

class InputDialogViewModel(
    initialText: String = "",
) : ViewModel() {
    private val _state = MutableStateFlow(InputDialogState(TextFieldValue(initialText, selection = TextRange(initialText.length))))
    val state = _state.asStateFlow()
    val focusRequester = FocusRequester()

    fun updateText(text: String) {
        _state.value = _state.value.copy(text = _state.value.text.copy(text))
    }

    fun updateText(text: TextFieldValue) {
        _state.value = _state.value.copy(text = text)
    }

    fun show() {
        _state.value = _state.value.copy(visible = true)
    }

    fun hide() {
        _state.value = _state.value.copy(visible = false)
    }

    fun selectAll() {
        val start = 0
        val end = _state.value.text.text.length
        _state.value = _state.value.copy(
            text = _state.value.text.copy(selection = TextRange(start, end))
        )
    }
}

@Composable
@Preview
fun InputDialog(
    viewModel: InputDialogViewModel,
    modifier: Modifier = Modifier,
    onAccept: (String) -> Unit = { },
) {
    val state by viewModel.state.collectAsState()

    if (state.visible) {
        Dialog(
            onDismissRequest = { viewModel.hide() }
        ) {
            Card {
                Column(modifier = modifier) {
                    OutlinedTextField(
                        value = state.text,
                        onValueChange = { viewModel.updateText(it) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                            .focusRequester(viewModel.focusRequester)
                            .onPreviewKeyEvent { event ->
                                if (event.key == Key.Enter) {
                                    if (event.type == KeyEventType.KeyDown) {
                                        onAccept(state.text.text)
                                        viewModel.hide()
                                    }
                                    return@onPreviewKeyEvent true
                                }
                                false
                            }
                    )
                }

                LaunchedEffect(Unit) {
                    viewModel.focusRequester.requestFocus()
                }
            }
        }
    }
}

fun main() =
    singleWindowApplication {
        Column {
            val model = InputDialogViewModel("my.important.note")
            InputDialog(model, onAccept = { text ->
                println("Accepted: $text")
            })
            model.show()

        }
    }