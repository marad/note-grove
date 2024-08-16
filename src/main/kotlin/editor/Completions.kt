package editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class CompletionsState() {
    private val visible = mutableStateOf(false)
    private val startOffset = mutableStateOf(0)
    private val items = mutableStateListOf<String>()
    private val selected = mutableStateOf(0)

    fun getStartOffset() = startOffset.value
    fun getItems() = items
    fun setItems(items: List<String>) {
        this.items.clear()
        this.items.addAll(items)
    }
    fun isVisible() = visible.value
    fun show() {
        selected.value = 0
        visible.value = true
    }
    fun hide() { visible.value = false }
    fun startCompletion(offset: Int) {
        startOffset.value = offset
        show()
    }
    fun previous() {
        selected.value = (selected.value-1).coerceAtLeast(0)
    }
    fun next() {
        selected.value = (selected.value+1).coerceAtMost(items.size-1)
    }
    fun isSelected(index: Int) = selected.value == index
    fun getSelected(): String? = items.getOrNull(selected.value)
}

@Composable
fun Completions(state: CompletionsState) {
    Column(modifier = Modifier
        .padding(5.dp))
    {
        state.getItems().forEachIndexed { index, it ->
            Surface(
                color = if (state.isSelected(index)) MaterialTheme.colors.secondary else Color.Transparent
            ) {
                Text(it, Modifier.padding(5.dp))
            }
        }
    }
}