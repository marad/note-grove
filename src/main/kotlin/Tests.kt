import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.window.singleWindowApplication


class TestState {
    var text = mutableStateOf("")
}

@Composable
fun Test(state: TestState, onChange: (String)->Unit) {
    TextField(state.text.value, onValueChange = onChange)
}

@Composable
fun TestScreen() {
    var state = remember { TestState() }
    Column {
        Test(state, onChange = { state.text.value = it })
        Button(onClick = {
            state.text.value += "World!"
        }) {
            Text("Greet")
        }
    }

}


fun main() {
    singleWindowApplication {
        TestScreen()
    }
}