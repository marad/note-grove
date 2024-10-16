package notestream

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp

fun <E> Iterable<E>.updated(index: Int, elem: E) = mapIndexed { i, existing ->  if (i == index) elem else existing }

data class NoteStreamState(
    val cards: List<NoteCardState>,
) {
    fun updateCard(index: Int, state: NoteCardState): NoteStreamState =
        copy(
            cards = cards.updated(index, state)
        )

    fun closeCardAt(index: Int): NoteStreamState =
        copy(cards = cards.filterIndexed { idx, _ -> idx != index })

    fun prependCard(card: NoteCardState): NoteStreamState {
        return copy(cards = listOf(card) + cards)
    }
}

@Composable
fun NoteStream(state: NoteStreamState,
               onUpdate: (NoteStreamState) -> Unit,
               onItemFocused: (Int) -> Unit = {},
               modifier: Modifier = Modifier,
               lazyListState: LazyListState = LazyListState()) {
    LazyColumn(modifier,
        state = lazyListState,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(state.cards) { idx, card ->
            NoteCard(card,
                onChange = { onUpdate(state.updateCard(idx, it)) },
                onClose = { onUpdate(state.closeCardAt(idx)) },
                modifier = Modifier.onFocusChanged {
                    if (it.hasFocus) {
                        onItemFocused(idx)
                    }
                })
        }
    }
}


