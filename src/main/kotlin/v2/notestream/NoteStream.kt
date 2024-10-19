package v2.notestream

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun <E> Iterable<E>.updated(index: Int, elem: E) = mapIndexed { i, existing ->  if (i == index) elem else existing }

data class NoteStreamState(
    val cards: List<NoteCardState> = emptyList(),
) {
    fun updateCard(index: Int, state: NoteCardState): NoteStreamState =
        copy(cards = cards.updated(index, state))

    fun updateCard(card: NoteCardState): NoteStreamState =
        copy(cards = cards.map { if (it.buffer.name == card.buffer.name) card else it })

    fun closeCardAt(index: Int): NoteStreamState =
        copy(cards = cards.filterIndexed { idx, _ -> idx != index })

    fun closeCard(card: NoteCardState): NoteStreamState =
        copy(cards = cards.filterNot { it == card })

    fun prependCard(card: NoteCardState): NoteStreamState =
        copy(cards = listOf(card) + cards)

    fun replaceCard(old: NoteCardState, new: NoteCardState) =
        copy(cards = cards.map {
            if (it == old) {
                new
            } else {
                it
            }
        })
}

@Composable
fun NoteStream(state: NoteStreamState,
               onUpdate: (NoteStreamState) -> Unit,
               onItemFocused: (Int) -> Unit = {},
               modifier: Modifier = Modifier,
               lazyListState: LazyListState = LazyListState(),
               outlineNote: Int = 0
               ) {
    if (state.cards.isNotEmpty()) {
        LazyColumn(
            modifier,
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
                    }.let {
                        if (outlineNote == idx) {
                            it.shadow(5.dp)
                                .border(1.dp, Color.Gray, RoundedCornerShape(5.dp))
                        } else {
                            it
                        }
                    }
                )
            }
        }
    } else {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Open or create a note with Ctrl+P",
                color = Color.Gray,
                fontSize = 18.sp
            )
        }
    }
}


