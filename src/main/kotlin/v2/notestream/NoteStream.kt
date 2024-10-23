package v2.notestream

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

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
    val coroutineScope = rememberCoroutineScope()

    if (state.cards.isNotEmpty()) {
        LazyColumn(
            modifier,
            state = lazyListState,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(state.cards) { idx, card ->
                var textLayout: TextLayoutResult? by remember { mutableStateOf(null) }
                var textFieldPosition by remember { mutableStateOf(0f) }

                fun scrollIfCursorOutOfView(cursorOffset: Int) {
                    val item = lazyListState.layoutInfo.visibleItemsInfo.find { it.index == idx }
                    val layout = textLayout
                    if (layout != null && item != null && !lazyListState.isScrollInProgress) {
                        val cursor = runCatching { layout.getCursorRect(cursorOffset) }
                            .getOrElse { layout.getCursorRect(state.cards[idx].selection.start) }
                        // I'm subtracting/adding cursor.height * 2 to make some space around the cursor after scroll
                        val cursorTop = (cursor.top + item.offset + textFieldPosition - cursor.height*2)
                        val cursorBottom = (cursor.bottom + item.offset + textFieldPosition + cursor.height*2)

                        val cursorDistanceFromViewportStart =
                            lazyListState.layoutInfo.viewportStartOffset - cursorTop
                        val cursorDistanceFromViewportEnd =
                            lazyListState.layoutInfo.viewportEndOffset - cursorBottom

                        var scrollOffset = 0f
                        if (cursorDistanceFromViewportStart > 0) {
                            // should scoll up
                            println("scrolling up by $cursorDistanceFromViewportStart")
                            scrollOffset = -cursorDistanceFromViewportStart
                        } else if (cursorDistanceFromViewportEnd < 0) {
                            // should scroll down
                            println("scrolling down by $cursorDistanceFromViewportEnd")
                            scrollOffset = -cursorDistanceFromViewportEnd
                        } else {
                            scrollOffset = 0f
                        }
                        coroutineScope.launch {
                            lazyListState.animateScrollBy(scrollOffset)
                        }
                    }
                }

                NoteCard(card,
                    active = outlineNote == idx,
                    onChange = {
                        val position = it.selection.start.coerceAtMost(state.cards[idx].textFieldValue.text.length)
                        onUpdate(state.updateCard(idx, it))
                        scrollIfCursorOutOfView(position)
                    },
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
                    },
                    textFieldModifier = Modifier.onGloballyPositioned {
                        textFieldPosition = it.positionInParent().y
                    },
                    onTextLayout = { layout ->
                        textLayout = layout
                        scrollIfCursorOutOfView(state.cards[idx].selection.start)
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


